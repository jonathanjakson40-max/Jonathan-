package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Question
import com.example.data.model.QuestionType
import com.example.data.model.Quiz

@Composable
fun QuizResultView(
    quiz: Quiz,
    score: Int,
    userAnswers: Map<Int, Int>,
    onRetake: () -> Unit,
    onSwitchToFlashcards: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val total = quiz.questions.size
    val percentage = if (total > 0) (score * 100) / total else 0

    val (badgeText, badgeColor, feedbackText) = when {
        percentage >= 90 -> Triple("Outstanding Mastery!", Color(0xFF2E7D32), "You demonstrated exceptional understanding of the uploaded material.")
        percentage >= 70 -> Triple("Great Achievement!", Color(0xFF1565C0), "Solid comprehension! Review missed questions below to achieve full mastery.")
        percentage >= 50 -> Triple("Good Effort!", Color(0xFFE65100), "A fair start. Studying the explanations below will boost your recall.")
        else -> Triple("Keep Learning!", Color(0xFFC62828), "Review the flashcards and explanations to strengthen your core knowledge.")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Score Summary Header Card
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("score_summary_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.12f))
                            .border(3.dp, badgeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$percentage%",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                            Text(
                                text = "$score / $total",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = feedbackText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onRetake,
                            modifier = Modifier.weight(1f).testTag("retake_quiz_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retake Quiz")
                        }

                        OutlinedButton(
                            onClick = onSwitchToFlashcards,
                            modifier = Modifier.weight(1f).testTag("study_flashcards_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Flashcards")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Copy Questions & Answers to Clipboard
                    OutlinedButton(
                        onClick = {
                            val notesText = buildString {
                                appendLine("--- ${quiz.title} ---")
                                appendLine("Source: ${quiz.sourceName}")
                                appendLine("Score: $score / $total ($percentage%)\n")
                                quiz.questions.forEachIndexed { i, q ->
                                    appendLine("Q${i + 1}: ${q.questionText}")
                                    appendLine("Correct Answer: ${q.correctAnswer}")
                                    if (q.explanation.isNotBlank()) appendLine("Explanation: ${q.explanation}")
                                    appendLine()
                                }
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("piccque Study Notes", notesText))
                            Toast.makeText(context, "Study sheet copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("copy_study_sheet_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Full Study Sheet & Answers")
                    }
                }
            }
        }

        // Detailed Breakdown Header
        item {
            Text(
                text = "Question by Question Review",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // List of all questions with answers & explanation
        itemsIndexed(quiz.questions) { index, question ->
            val userSelectedOpt = userAnswers[index]
            val isCorrect = when (question.type) {
                QuestionType.MCQ, QuestionType.TRUE_FALSE -> {
                    question.correctIndex == userSelectedOpt ||
                            (userSelectedOpt != null && question.options.getOrNull(userSelectedOpt).equals(question.correctAnswer, ignoreCase = true))
                }
                else -> true
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("review_item_$index"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isCorrect) "Correct" else "Needs Review",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }

                        Text(
                            text = "Question ${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = question.questionText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Correct Answer
                    Text(
                        text = "Correct: ${question.correctAnswer}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )

                    if (question.explanation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
