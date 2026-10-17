package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class QuizWithQuestions(
    val quiz: QuizEntity,
    val questions: List<QuestionEntity>
)

@Dao
interface QuizDao {
    @Query("SELECT * FROM quizzes ORDER BY createdAt DESC")
    fun getAllQuizzesFlow(): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes WHERE id = :quizId LIMIT 1")
    suspend fun getQuizById(quizId: String): QuizEntity?

    @Query("SELECT * FROM questions WHERE quizId = :quizId ORDER BY orderIndex ASC")
    suspend fun getQuestionsForQuiz(quizId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE quizId = :quizId ORDER BY orderIndex ASC")
    fun getQuestionsForQuizFlow(quizId: String): Flow<List<QuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Transaction
    suspend fun insertFullQuiz(quiz: QuizEntity, questions: List<QuestionEntity>) {
        insertQuiz(quiz)
        insertQuestions(questions)
    }

    @Query("UPDATE quizzes SET lastScore = :score, bestScore = CASE WHEN bestScore IS NULL OR :score > bestScore THEN :score ELSE bestScore END WHERE id = :quizId")
    suspend fun updateScore(quizId: String, score: Int)

    @Query("DELETE FROM quizzes WHERE id = :quizId")
    suspend fun deleteQuiz(quizId: String)

    @Query("DELETE FROM quizzes")
    suspend fun clearAll()
}
