package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val summary: String,
    val sourceKind: String,
    val sourceName: String,
    val createdAt: Long,
    val questionCount: Int,
    val bestScore: Int?,
    val lastScore: Int?
)

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = QuizEntity::class,
            parentColumns = ["id"],
            childColumns = ["quizId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("quizId")]
)
data class QuestionEntity(
    @PrimaryKey
    val id: String,
    val quizId: String,
    val orderIndex: Int,
    val questionText: String,
    val type: String,
    val optionsJson: String, // Pipe or comma-separated or JSON string
    val correctAnswer: String,
    val correctIndex: Int,
    val explanation: String,
    val hint: String
)
