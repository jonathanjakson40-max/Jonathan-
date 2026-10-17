package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Question
import com.example.data.model.QuestionType
import com.example.data.model.Quiz
import com.example.ui.DocuQuizViewModel
import com.example.ui.QuizPlayerState

@Composable
fun QuizPlayerView(
    quiz: Quiz,
    viewModel: DocuQuizViewModel,
    modifier: Modifier = Modifier
) {
    val playerState by viewModel.playerState.collectAsState()
    val currentQuestion = quiz.questions.getOrNull(playerState.currentQuestionIndex)

    if (playerState.isQuizFinished) {
        QuizResultView(
            quiz = quiz,
            score = playerState.score,
            userAnswers = playerState.userAnswers,
            onRetake = { viewModel.restartQuiz() },
            onSwitchToFlashcards = { viewModel.setStudySubMode("FLASHCARDS") }
        )
        return
    }

    if (currentQuestion == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No questions available in this quiz.")
        }
        return
    }

    val totalQuestions = quiz.questions.size
    val currentIndex = playerState.currentQuestionIndex
    val progress = (currentIndex + 1).toFloat() / totalQuestions

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header: Quiz Title & Flashcards Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quiz.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "Source: ${quiz.sourceName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = { viewModel.setStudySubMode("FLASHCARDS") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("switch_to_flashcards_button")
            ) {
                Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Flashcards", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Progress Tracker Bar
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Question ${currentIndex + 1} of $totalQuestions",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Score: ${playerState.score}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }

        // Question Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("question_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Type Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = currentQuestion.type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentQuestion.questionText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Answer Options / Input Area
        when (currentQuestion.type) {
            QuestionType.MCQ, QuestionType.TRUE_FALSE -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    currentQuestion.options.forEachIndexed { index, option ->
                        val isSelected = playerState.selectedOptionIndex == index
                        val isAnswerRevealed = playerState.isAnswerRevealed
                        val isCorrectOption = currentQuestion.correctIndex == index ||
                                option.equals(currentQuestion.correctAnswer, ignoreCase = true)

                        val optionBgColor by animateColorAsState(
                            targetValue = when {
                                !isAnswerRevealed -> if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                isCorrectOption -> Color(0xFFE8F5E9) // Light Green
                                isSelected -> Color(0xFFFFEBEE) // Light Red
                                else -> MaterialTheme.colorScheme.surface
                            },
                            label = "optionBg"
                        )

                        val optionBorderColor by animateColorAsState(
                            targetValue = when {
                                !isAnswerRevealed -> if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                isCorrectOption -> Color(0xFF2E7D32) // Green
                                isSelected -> Color(0xFFC62828) // Red
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                            label = "optionBorder"
                        )

                        val optionLetter = ('A' + index).toString()

                        Surface(
                            onClick = { viewModel.selectOption(index) },
                            enabled = !isAnswerRevealed,
                            shape = RoundedCornerShape(14.dp),
                            color = optionBgColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, optionBorderColor, RoundedCornerShape(14.dp))
                                .testTag("option_button_$index")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isAnswerRevealed && isCorrectOption) Color(0xFF2E7D32)
                                            else if (isAnswerRevealed && isSelected) Color(0xFFC62828)
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isAnswerRevealed && isCorrectOption) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    } else if (isAnswerRevealed && isSelected) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    } else {
                                        Text(
                                            text = optionLetter,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            QuestionType.FLASHCARD, QuestionType.FILL_BLANK -> {
                // Flashcard / Open Q&A option in quiz mode
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!playerState.isAnswerRevealed) {
                        Button(
                            onClick = { viewModel.selectOption(0) },
                            modifier = Modifier.fillMaxWidth().testTag("reveal_answer_button")
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reveal Correct Answer")
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Correct Answer:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentQuestion.correctAnswer,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Hint Section
        if (currentQuestion.hint.isNotBlank()) {
            if (!playerState.isHintRevealed) {
                OutlinedButton(
                    onClick = { viewModel.revealHint() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("show_hint_button")
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Show Hint")
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = currentQuestion.hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        // Explanation Card (Shown when answered)
        AnimatedVisibility(visible = playerState.isAnswerRevealed && currentQuestion.explanation.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explanation_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Explanation & Key Concept",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentQuestion.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Next Question Button
        if (playerState.isAnswerRevealed) {
            Button(
                onClick = { viewModel.nextQuestion() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("next_question_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (currentIndex + 1 < totalQuestions) "Next Question" else "See Final Results",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
