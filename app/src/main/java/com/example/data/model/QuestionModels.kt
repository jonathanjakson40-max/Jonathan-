package com.example.data.model

enum class QuestionType(val displayName: String) {
    MCQ("Multiple Choice"),
    TRUE_FALSE("True / False"),
    FLASHCARD("Flashcard / Q&A"),
    FILL_BLANK("Fill in the Blank")
}

enum class SourceKind(val label: String) {
    PICTURE("Picture / Photo"),
    PDF("PDF Document"),
    BOOK_TEXT("Book / Excerpt"),
    PRESET("Sample Material")
}

data class Question(
    val id: String = "",
    val questionText: String,
    val type: QuestionType,
    val options: List<String> = emptyList(),
    val correctAnswer: String,
    val correctIndex: Int = -1,
    val explanation: String = "",
    val hint: String = ""
)

data class Quiz(
    val id: String,
    val title: String,
    val summary: String,
    val sourceKind: SourceKind,
    val sourceName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val questions: List<Question>,
    val bestScore: Int? = null,
    val lastAttemptScore: Int? = null
)

data class GenerationSettings(
    val questionCount: Int = 5,
    val questionType: String = "MIXED", // MCQ, TRUE_FALSE, FLASHCARD, MIXED
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val focus: String = "Comprehensive", // Comprehensive, Key Facts, Exam Prep, Vocabulary
    val language: String = "Auto"
)

data class SampleDoc(
    val title: String,
    val category: String,
    val snippet: String,
    val fullContent: String
)
