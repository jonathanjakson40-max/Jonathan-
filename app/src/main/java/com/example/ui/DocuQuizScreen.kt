package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.FlashcardView
import com.example.ui.components.QuizHistoryView
import com.example.ui.components.QuizPlayerView
import com.example.ui.components.UploadSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocuQuizScreen(
    viewModel: DocuQuizViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val activeQuiz by viewModel.activeQuiz.collectAsState()
    val studySubMode by viewModel.studySubMode.collectAsState()

    // Handle back button when on Study tab or Library tab
    BackHandler(enabled = currentTab != AppTab.CREATE) {
        viewModel.selectTab(AppTab.CREATE)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "piccque",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.CREATE,
                    onClick = { viewModel.selectTab(AppTab.CREATE) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Create") },
                    label = { Text("Upload & Create") },
                    modifier = Modifier.testTag("nav_create_tab")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.STUDY,
                    onClick = { viewModel.selectTab(AppTab.STUDY) },
                    icon = { Icon(Icons.Default.Quiz, contentDescription = "Quiz") },
                    label = { Text("Active Quiz") },
                    modifier = Modifier.testTag("nav_study_tab")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.LIBRARY,
                    onClick = { viewModel.selectTab(AppTab.LIBRARY) },
                    icon = { Icon(Icons.Default.Bookmarks, contentDescription = "Library") },
                    label = { Text("My Library") },
                    modifier = Modifier.testTag("nav_library_tab")
                )
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentTab,
            label = "tab_crossfade",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { tab ->
            when (tab) {
                AppTab.CREATE -> {
                    UploadSection(viewModel = viewModel)
                }

                AppTab.STUDY -> {
                    if (activeQuiz == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Active Quiz",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Upload a photo of a book, a PDF, or pick a quiz from your library.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.selectTab(AppTab.CREATE) },
                                    modifier = Modifier.testTag("empty_study_create_button")
                                ) {
                                    Text("Go to Upload")
                                }
                            }
                        }
                    } else {
                        if (studySubMode == "FLASHCARDS") {
                            FlashcardView(quiz = activeQuiz!!, viewModel = viewModel)
                        } else {
                            QuizPlayerView(quiz = activeQuiz!!, viewModel = viewModel)
                        }
                    }
                }

                AppTab.LIBRARY -> {
                    QuizHistoryView(viewModel = viewModel)
                }
            }
        }
    }
}
