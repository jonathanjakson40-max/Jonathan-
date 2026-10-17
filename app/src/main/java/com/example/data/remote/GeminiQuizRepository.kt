package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.BuildConfig
import com.example.data.model.GenerationSettings
import com.example.data.model.Question
import com.example.data.model.QuestionType
import com.example.data.model.Quiz
import com.example.data.model.SampleDoc
import com.example.data.model.SourceKind
import com.example.util.DocumentUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class GeminiQuizRepository(private val context: Context) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateQuiz(
        sourceKind: SourceKind,
        sourceName: String,
        imageBitmap: Bitmap? = null,
        imageUri: Uri? = null,
        pdfUri: Uri? = null,
        rawText: String? = null,
        settings: GenerationSettings
    ): Result<Quiz> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Return informative error instructing to set key, with fallback option
            return@withContext Result.failure(
                IllegalStateException("GEMINI_API_KEY is not configured. Please set your key in the AI Studio Secrets panel.")
            )
        }

        try {
            val systemPrompt = """
You are an expert educational assessment creator and master teacher.
Your job is to examine the provided content (image of book/text, PDF document, or text excerpt) and generate high-quality, comprehensive questions that test understanding, key concepts, recall, and critical thinking.

Output strictly valid JSON with the following structure:
{
  "title": "A concise, engaging title for this quiz based on the subject",
  "summary": "A 1-2 sentence overview of the content covered",
  "questions": [
    {
      "id": "1",
      "type": "MCQ", // MCQ, TRUE_FALSE, FLASHCARD, or FILL_BLANK
      "question": "Question text...",
      "options": ["Option A", "Option B", "Option C", "Option D"], // Required for MCQ (exactly 4 options). For TRUE_FALSE, ["True", "False"]. For FLASHCARD and FILL_BLANK, can be empty or 2-4 options.
      "correctAnswer": "The exact correct answer string",
      "correctIndex": 0, // 0-based index of correct option for MCQ/TRUE_FALSE
      "explanation": "Clear explanation of why this answer is correct and why other options are wrong.",
      "hint": "A subtle helpful clue without giving away the full answer."
    }
  ]
}

Instructions:
1. Generate exactly ${settings.questionCount} questions.
2. Question style: ${if (settings.questionType == "MIXED") "Mix of Multiple Choice (MCQ), True/False, and Flashcard Q&A" else settings.questionType}.
3. Difficulty: ${settings.difficulty}.
4. Target Focus: ${settings.focus}.
5. Ensure options for Multiple Choice are plausible distractors based on the text.
6. Return ONLY the JSON object.
""".trimIndent()

            val contentsArray = JSONArray()
            val userContent = JSONObject()
            val partsArray = JSONArray()

            // 1. Prompt Part
            val promptPart = JSONObject().apply {
                put("text", "Please analyze the attached material and create ${settings.questionCount} ${settings.difficulty}-level ${settings.questionType} questions as requested.")
            }
            partsArray.put(promptPart)

            // 2. Multimodal Parts (Image or PDF or Text)
            when (sourceKind) {
                SourceKind.PICTURE -> {
                    val base64Data = if (imageBitmap != null) {
                        DocumentUtils.bitmapToBase64(imageBitmap)
                    } else if (imageUri != null) {
                        DocumentUtils.uriToOptimizedImageBase64(context, imageUri)
                    } else null

                    if (base64Data != null) {
                        val inlineData = JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Data)
                        }
                        partsArray.put(JSONObject().put("inlineData", inlineData))
                    } else {
                        return@withContext Result.failure(IllegalArgumentException("Could not read image content."))
                    }
                }

                SourceKind.PDF -> {
                    val base64Pdf = if (pdfUri != null) {
                        DocumentUtils.uriToBase64(context, pdfUri)
                    } else null

                    if (base64Pdf != null) {
                        val inlineData = JSONObject().apply {
                            put("mimeType", "application/pdf")
                            put("data", base64Pdf)
                        }
                        partsArray.put(JSONObject().put("inlineData", inlineData))
                    } else {
                        return@withContext Result.failure(IllegalArgumentException("Could not read PDF document content."))
                    }
                }

                SourceKind.BOOK_TEXT, SourceKind.PRESET -> {
                    if (!rawText.isNullOrBlank()) {
                        val textPart = JSONObject().apply {
                            put("text", "--- DOCUMENT CONTENT ---\n$rawText\n--- END CONTENT ---")
                        }
                        partsArray.put(textPart)
                    } else {
                        return@withContext Result.failure(IllegalArgumentException("No document text was provided."))
                    }
                }
            }

            userContent.put("parts", partsArray)
            contentsArray.put(userContent)

            // Generation config with JSON responseMimeType
            val generationConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.3)
            }

            val systemInstruction = JSONObject().apply {
                val parts = JSONArray().put(JSONObject().put("text", systemPrompt))
                put("parts", parts)
            }

            val requestBodyJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", generationConfig)
                put("systemInstruction", systemInstruction)
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Gemini API error (HTTP ${response.code}): $responseBody")
                )
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("No content generated by Gemini."))
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val responseText = parts?.optJSONObject(0)?.optString("text") ?: ""

            val (parsedTitle, parsedQuestions) = DocumentUtils.parseQuizJson(
                jsonString = responseText,
                fallbackTitle = sourceName.substringBeforeLast("."),
                sourceName = sourceName
            )

            if (parsedQuestions.isEmpty()) {
                return@withContext Result.failure(Exception("Failed to extract valid questions from response."))
            }

            val quiz = Quiz(
                id = UUID.randomUUID().toString(),
                title = parsedTitle,
                summary = "Generated from $sourceName (${parsedQuestions.size} questions)",
                sourceKind = sourceKind,
                sourceName = sourceName,
                questions = parsedQuestions
            )

            Result.success(quiz)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Creates a high-fidelity mock quiz from sample presets or fallback mode
     * so that the user can explore and test the entire UI immediately even before setting an API key.
     */
    fun createSampleFallbackQuiz(
        sourceKind: SourceKind,
        sourceName: String,
        sampleDoc: SampleDoc? = null
    ): Quiz {
        val sample = DocumentUtils.SAMPLE_DOCUMENTS.firstOrNull { it.title.contains(sourceName, ignoreCase = true) }
            ?: DocumentUtils.SAMPLE_DOCUMENTS.first()

        val sampleQuestions = listOf(
            Question(
                id = "1",
                questionText = "Which phase of cellular respiration occurs in the cytoplasm and does not require oxygen?",
                type = QuestionType.MCQ,
                options = listOf("Glycolysis", "Citric Acid Cycle (Krebs)", "Oxidative Phosphorylation", "Electron Transport Chain"),
                correctAnswer = "Glycolysis",
                correctIndex = 0,
                explanation = "Glycolysis breaks down glucose in the cytoplasm anaerobically, netting 2 ATP and 2 NADH.",
                hint = "It breaks down sugar directly in the cytoplasm."
            ),
            Question(
                id = "2",
                questionText = "Oxygen serves as the final electron acceptor in the electron transport chain.",
                type = QuestionType.TRUE_FALSE,
                options = listOf("True", "False"),
                correctAnswer = "True",
                correctIndex = 0,
                explanation = "Oxygen binds with electrons and free protons (H+) at Complex IV to form metabolic water (H2O).",
                hint = "Think about what happens to oxygen when you breathe."
            ),
            Question(
                id = "3",
                questionText = "What enzyme synthesizes ATP using the proton motive force across the inner mitochondrial membrane?",
                type = QuestionType.FLASHCARD,
                options = emptyList(),
                correctAnswer = "ATP Synthase",
                correctIndex = -1,
                explanation = "ATP Synthase acts as a molecular turbine, utilizing the chemiosmotic electrochemical gradient to phosphorylate ADP into ATP.",
                hint = "The name ends with 'Synthase'."
            ),
            Question(
                id = "4",
                questionText = "How many net ATP molecules are produced during the glycolysis phase per glucose molecule?",
                type = QuestionType.MCQ,
                options = listOf("2 ATP", "4 ATP", "32 ATP", "38 ATP"),
                correctAnswer = "2 ATP",
                correctIndex = 0,
                explanation = "Glycolysis consumes 2 ATP during investment and produces 4 ATP, yielding a net total of 2 ATP.",
                hint = "Investment uses 2, output is 4."
            ),
            Question(
                id = "5",
                questionText = "In eukaryotic cells, pyruvate is converted into Acetyl-CoA inside the mitochondrial matrix.",
                type = QuestionType.TRUE_FALSE,
                options = listOf("True", "False"),
                correctAnswer = "True",
                correctIndex = 0,
                explanation = "Pyruvate dehydrogenase complex in the mitochondrial matrix oxidizes pyruvate into Acetyl-CoA prior to the citric acid cycle.",
                hint = "Recall the transition step before Krebs cycle."
            )
        )

        return Quiz(
            id = UUID.randomUUID().toString(),
            title = sample.title,
            summary = sample.snippet,
            sourceKind = sourceKind,
            sourceName = sourceName,
            questions = sampleQuestions
        )
    }
}
