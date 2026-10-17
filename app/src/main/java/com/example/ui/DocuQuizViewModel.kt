package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.QuestionEntity
import com.example.data.local.QuizEntity
import com.example.data.model.GenerationSettings
import com.example.data.model.Question
import com.example.data.model.QuestionType
import com.example.data.model.Quiz
import com.example.data.model.SampleDoc
import com.example.data.model.SourceKind
import com.example.data.remote.GeminiQuizRepository
import com.example.util.DocumentUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface GenerationUiState {
    object Idle : GenerationUiState
    data class Generating(val stepMessage: String) : GenerationUiState
    data class Ready(val quiz: Quiz) : GenerationUiState
    data class Error(val message: String, val canUseDemo: Boolean = true) : GenerationUiState
}

enum class AppTab(val title: String) {
    CREATE("Create Quiz"),
    LIBRARY("Saved Quizzes"),
    STUDY("Active Quiz")
}

data class QuizPlayerState(
    val currentQuestionIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isAnswerRevealed: Boolean = false,
    val isHintRevealed: Boolean = false,
    val userAnswers: Map<Int, Int> = emptyMap(), // questionIndex -> selectedOptionIndex
    val isQuizFinished: Boolean = false,
    val score: Int = 0
)

data class FlashcardState(
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val masteredIds: Set<String> = emptySet(),
    val reviewLaterIds: Set<String> = emptySet()
)

class DocuQuizViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val quizDao = db.quizDao()
    private val repository = GeminiQuizRepository(application)

    // Current Navigation Tab
    private val _currentTab = MutableStateFlow(AppTab.CREATE)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Source Selection
    private val _selectedSourceKind = MutableStateFlow(SourceKind.PICTURE)
    val selectedSourceKind: StateFlow<SourceKind> = _selectedSourceKind.asStateFlow()

    // Uploaded assets
    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri: StateFlow<Uri?> = _imageUri.asStateFlow()

    private val _cameraBitmap = MutableStateFlow<Bitmap?>(null)
    val cameraBitmap: StateFlow<Bitmap?> = _cameraBitmap.asStateFlow()

    private val _pdfUri = MutableStateFlow<Uri?>(null)
    val pdfUri: StateFlow<Uri?> = _pdfUri.asStateFlow()

    private val _pdfName = MutableStateFlow<String>("")
    val pdfName: StateFlow<String> = _pdfName.asStateFlow()

    private val _pdfPageCount = MutableStateFlow(0)
    val pdfPageCount: StateFlow<Int> = _pdfPageCount.asStateFlow()

    private val _pdfPreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val pdfPreviewBitmap: StateFlow<Bitmap?> = _pdfPreviewBitmap.asStateFlow()

    private val _bookText = MutableStateFlow("")
    val bookText: StateFlow<String> = _bookText.asStateFlow()

    private val _selectedPreset = MutableStateFlow<SampleDoc?>(null)
    val selectedPreset: StateFlow<SampleDoc?> = _selectedPreset.asStateFlow()

    // Generation Settings
    private val _settings = MutableStateFlow(GenerationSettings())
    val settings: StateFlow<GenerationSettings> = _settings.asStateFlow()

    // Generation Status
    private val _generationState = MutableStateFlow<GenerationUiState>(GenerationUiState.Idle)
    val generationState: StateFlow<GenerationUiState> = _generationState.asStateFlow()

    // Active Quiz Session
    private val _activeQuiz = MutableStateFlow<Quiz?>(null)
    val activeQuiz: StateFlow<Quiz?> = _activeQuiz.asStateFlow()

    private val _playerState = MutableStateFlow(QuizPlayerState())
    val playerState: StateFlow<QuizPlayerState> = _playerState.asStateFlow()

    private val _flashcardState = MutableStateFlow(FlashcardState())
    val flashcardState: StateFlow<FlashcardState> = _flashcardState.asStateFlow()

    private val _studySubMode = MutableStateFlow("QUIZ") // "QUIZ" or "FLASHCARDS"
    val studySubMode: StateFlow<String> = _studySubMode.asStateFlow()

    // Library of saved quizzes from Room
    val savedQuizzes: StateFlow<List<Quiz>> = quizDao.getAllQuizzesFlow()
        .map { entities ->
            entities.map { entity ->
                Quiz(
                    id = entity.id,
                    title = entity.title,
                    summary = entity.summary,
                    sourceKind = try { SourceKind.valueOf(entity.sourceKind) } catch (_: Exception) { SourceKind.BOOK_TEXT },
                    sourceName = entity.sourceName,
                    createdAt = entity.createdAt,
                    questions = emptyList(), // Loaded on demand
                    bestScore = entity.bestScore,
                    lastAttemptScore = entity.lastScore
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun selectSourceKind(kind: SourceKind) {
        _selectedSourceKind.value = kind
    }

    fun setSettings(newSettings: GenerationSettings) {
        _settings.value = newSettings
    }

    fun setImageUri(uri: Uri?) {
        _imageUri.value = uri
        _cameraBitmap.value = null
    }

    fun setCameraBitmap(bitmap: Bitmap?) {
        _cameraBitmap.value = bitmap
        _imageUri.value = null
    }

    fun setPdfUri(uri: Uri?) {
        _pdfUri.value = uri
        if (uri != null) {
            val context = getApplication<Application>()
            _pdfName.value = DocumentUtils.getFileName(context, uri)
            _pdfPageCount.value = DocumentUtils.getPdfPageCount(context, uri)
            viewModelScope.launch(Dispatchers.IO) {
                val preview = DocumentUtils.renderPdfFirstPage(context, uri)
                withContext(Dispatchers.Main) {
                    _pdfPreviewBitmap.value = preview
                }
            }
        } else {
            _pdfName.value = ""
            _pdfPageCount.value = 0
            _pdfPreviewBitmap.value = null
        }
    }

    fun setBookText(text: String) {
        _bookText.value = text
    }

    fun selectPreset(preset: SampleDoc) {
        _selectedPreset.value = preset
        _bookText.value = preset.fullContent
    }

    fun generateQuiz() {
        val kind = _selectedSourceKind.value
        val currentSettings = _settings.value

        viewModelScope.launch {
            _generationState.value = GenerationUiState.Generating("Analyzing content and extracting concepts...")

            val sourceName = when (kind) {
                SourceKind.PICTURE -> if (_imageUri.value != null) DocumentUtils.getFileName(getApplication(), _imageUri.value!!) else "Camera Photo"
                SourceKind.PDF -> _pdfName.value.ifBlank { "PDF Document" }
                SourceKind.BOOK_TEXT -> "Text Excerpt (${_bookText.value.take(25)}...)"
                SourceKind.PRESET -> _selectedPreset.value?.title ?: "Sample Material"
            }

            val result = repository.generateQuiz(
                sourceKind = kind,
                sourceName = sourceName,
                imageBitmap = _cameraBitmap.value,
                imageUri = _imageUri.value,
                pdfUri = _pdfUri.value,
                rawText = if (kind == SourceKind.PRESET) _selectedPreset.value?.fullContent else _bookText.value,
                settings = currentSettings
            )

            result.onSuccess { quiz ->
                _generationState.value = GenerationUiState.Ready(quiz)
                startQuizSession(quiz)
                saveQuizToDatabase(quiz)
            }.onFailure { err ->
                val errorMsg = err.message ?: "Failed to generate questions."
                _generationState.value = GenerationUiState.Error(errorMsg, canUseDemo = true)
            }
        }
    }

    fun useSampleDemoQuiz() {
        val kind = _selectedSourceKind.value
        val sourceName = when (kind) {
            SourceKind.PICTURE -> "Sample Photo of Biology Textbook"
            SourceKind.PDF -> "Sample PDF of History Document"
            SourceKind.BOOK_TEXT -> "Textbook Excerpt"
            SourceKind.PRESET -> _selectedPreset.value?.title ?: "Cell Biology & Respiration"
        }

        val demoQuiz = repository.createSampleFallbackQuiz(kind, sourceName)
        _generationState.value = GenerationUiState.Ready(demoQuiz)
        startQuizSession(demoQuiz)
        saveQuizToDatabase(demoQuiz)
    }

    fun startQuizSession(quiz: Quiz) {
        _activeQuiz.value = quiz
        _playerState.value = QuizPlayerState()
        _flashcardState.value = FlashcardState()
        _currentTab.value = AppTab.STUDY
    }

    fun setStudySubMode(mode: String) {
        _studySubMode.value = mode
    }

    // --- Quiz Player Actions ---
    fun selectOption(optionIndex: Int) {
        val state = _playerState.value
        if (state.isAnswerRevealed || state.isQuizFinished) return

        val quiz = _activeQuiz.value ?: return
        val currentQ = quiz.questions.getOrNull(state.currentQuestionIndex) ?: return

        val isCorrect = currentQ.correctIndex == optionIndex ||
                (currentQ.options.getOrNull(optionIndex).equals(currentQ.correctAnswer, ignoreCase = true))

        val newAnswers = state.userAnswers.toMutableMap()
        newAnswers[state.currentQuestionIndex] = optionIndex

        val newScore = if (isCorrect) state.score + 1 else state.score

        _playerState.value = state.copy(
            selectedOptionIndex = optionIndex,
            isAnswerRevealed = true,
            userAnswers = newAnswers,
            score = newScore
        )
    }

    fun revealHint() {
        _playerState.value = _playerState.value.copy(isHintRevealed = true)
    }

    fun nextQuestion() {
        val state = _playerState.value
        val quiz = _activeQuiz.value ?: return

        if (state.currentQuestionIndex + 1 < quiz.questions.size) {
            val nextIndex = state.currentQuestionIndex + 1
            val nextSelected = state.userAnswers[nextIndex]
            _playerState.value = state.copy(
                currentQuestionIndex = nextIndex,
                selectedOptionIndex = nextSelected,
                isAnswerRevealed = nextSelected != null,
                isHintRevealed = false
            )
        } else {
            // Finished Quiz!
            _playerState.value = state.copy(isQuizFinished = true)
            // Save final score to database
            viewModelScope.launch(Dispatchers.IO) {
                quizDao.updateScore(quiz.id, state.score)
            }
        }
    }

    fun restartQuiz() {
        _playerState.value = QuizPlayerState()
    }

    // --- Flashcards Actions ---
    fun flipCard() {
        _flashcardState.value = _flashcardState.value.copy(isFlipped = !_flashcardState.value.isFlipped)
    }

    fun markCardMastered(questionId: String) {
        val current = _flashcardState.value
        val newMastered = current.masteredIds + questionId
        val newReview = current.reviewLaterIds - questionId
        advanceCard(current.copy(masteredIds = newMastered, reviewLaterIds = newReview))
    }

    fun markCardReviewLater(questionId: String) {
        val current = _flashcardState.value
        val newReview = current.reviewLaterIds + questionId
        val newMastered = current.masteredIds - questionId
        advanceCard(current.copy(masteredIds = newMastered, reviewLaterIds = newReview))
    }

    private fun advanceCard(newState: FlashcardState) {
        val total = _activeQuiz.value?.questions?.size ?: 0
        val nextIdx = if (newState.currentIndex + 1 < total) newState.currentIndex + 1 else 0
        _flashcardState.value = newState.copy(currentIndex = nextIdx, isFlipped = false)
    }

    fun resetFlashcards() {
        _flashcardState.value = FlashcardState()
    }

    // --- Database Operations ---
    private fun saveQuizToDatabase(quiz: Quiz) {
        viewModelScope.launch(Dispatchers.IO) {
            val quizEntity = QuizEntity(
                id = quiz.id,
                title = quiz.title,
                summary = quiz.summary,
                sourceKind = quiz.sourceKind.name,
                sourceName = quiz.sourceName,
                createdAt = quiz.createdAt,
                questionCount = quiz.questions.size,
                bestScore = quiz.bestScore,
                lastScore = quiz.lastAttemptScore
            )

            val questionEntities = quiz.questions.mapIndexed { index, q ->
                QuestionEntity(
                    id = "${quiz.id}_$index",
                    quizId = quiz.id,
                    orderIndex = index,
                    questionText = q.questionText,
                    type = q.type.name,
                    optionsJson = q.options.joinToString("|||"),
                    correctAnswer = q.correctAnswer,
                    correctIndex = q.correctIndex,
                    explanation = q.explanation,
                    hint = q.hint
                )
            }

            quizDao.insertFullQuiz(quizEntity, questionEntities)
        }
    }

    fun loadQuizFromLibrary(quizId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = quizDao.getQuizById(quizId) ?: return@launch
            val qEntities = quizDao.getQuestionsForQuiz(quizId)

            val questions = qEntities.map { qe ->
                val opts = if (qe.optionsJson.isBlank()) emptyList() else qe.optionsJson.split("|||")
                val type = try { QuestionType.valueOf(qe.type) } catch (_: Exception) { QuestionType.MCQ }
                Question(
                    id = qe.id,
                    questionText = qe.questionText,
                    type = type,
                    options = opts,
                    correctAnswer = qe.correctAnswer,
                    correctIndex = qe.correctIndex,
                    explanation = qe.explanation,
                    hint = qe.hint
                )
            }

            val sourceKind = try { SourceKind.valueOf(entity.sourceKind) } catch (_: Exception) { SourceKind.BOOK_TEXT }
            val quiz = Quiz(
                id = entity.id,
                title = entity.title,
                summary = entity.summary,
                sourceKind = sourceKind,
                sourceName = entity.sourceName,
                createdAt = entity.createdAt,
                questions = questions,
                bestScore = entity.bestScore,
                lastAttemptScore = entity.lastScore
            )

            withContext(Dispatchers.Main) {
                startQuizSession(quiz)
            }
        }
    }

    fun deleteQuiz(quizId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            quizDao.deleteQuiz(quizId)
            if (_activeQuiz.value?.id == quizId) {
                withContext(Dispatchers.Main) {
                    _activeQuiz.value = null
                    _currentTab.value = AppTab.CREATE
                }
            }
        }
    }
}
