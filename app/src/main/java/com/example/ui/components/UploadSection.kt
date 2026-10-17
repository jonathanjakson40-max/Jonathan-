package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.GenerationSettings
import com.example.data.model.SampleDoc
import com.example.data.model.SourceKind
import com.example.ui.DocuQuizViewModel
import com.example.ui.GenerationUiState
import com.example.util.DocumentUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadSection(
    viewModel: DocuQuizViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedSource by viewModel.selectedSourceKind.collectAsState()
    val imageUri by viewModel.imageUri.collectAsState()
    val cameraBitmap by viewModel.cameraBitmap.collectAsState()
    val pdfUri by viewModel.pdfUri.collectAsState()
    val pdfName by viewModel.pdfName.collectAsState()
    val pdfPageCount by viewModel.pdfPageCount.collectAsState()
    val pdfPreviewBitmap by viewModel.pdfPreviewBitmap.collectAsState()
    val bookText by viewModel.bookText.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val generationState by viewModel.generationState.collectAsState()

    // Activity Result Launchers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setImageUri(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.setCameraBitmap(bitmap)
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setPdfUri(uri)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_header_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Sparkle",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Turn Any Document Into a Quiz",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Upload a photo of a book, a PDF chapter, or paste notes to generate tests & flashcards.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Source Type Selector Tabs
        item {
            Text(
                text = "1. Choose Document Source",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SourceTabButton(
                    icon = Icons.Default.Image,
                    label = "Photo / Book",
                    isSelected = selectedSource == SourceKind.PICTURE,
                    modifier = Modifier.weight(1f).testTag("source_picture_tab"),
                    onClick = { viewModel.selectSourceKind(SourceKind.PICTURE) }
                )
                SourceTabButton(
                    icon = Icons.Default.PictureAsPdf,
                    label = "PDF File",
                    isSelected = selectedSource == SourceKind.PDF,
                    modifier = Modifier.weight(1f).testTag("source_pdf_tab"),
                    onClick = { viewModel.selectSourceKind(SourceKind.PDF) }
                )
                SourceTabButton(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = "Text Notes",
                    isSelected = selectedSource == SourceKind.BOOK_TEXT,
                    modifier = Modifier.weight(1f).testTag("source_text_tab"),
                    onClick = { viewModel.selectSourceKind(SourceKind.BOOK_TEXT) }
                )
                SourceTabButton(
                    icon = Icons.Default.School,
                    label = "Samples",
                    isSelected = selectedSource == SourceKind.PRESET,
                    modifier = Modifier.weight(1f).testTag("source_sample_tab"),
                    onClick = { viewModel.selectSourceKind(SourceKind.PRESET) }
                )
            }
        }

        // Selected Source Content Area
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    when (selectedSource) {
                        SourceKind.PICTURE -> {
                            Text(
                                text = "Snap a photo of your book or pick from gallery",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f).testTag("pick_gallery_button")
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pick Photo")
                                }

                                OutlinedButton(
                                    onClick = { cameraLauncher.launch(null) },
                                    modifier = Modifier.weight(1f).testTag("take_camera_button")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Take Photo")
                                }
                            }

                            if (imageUri != null || cameraBitmap != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                ) {
                                    if (cameraBitmap != null) {
                                        Image(
                                            bitmap = cameraBitmap!!.asImageBitmap(),
                                            contentDescription = "Captured camera photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (imageUri != null) {
                                        AsyncImage(
                                            model = imageUri,
                                            contentDescription = "Uploaded book page image",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.setImageUri(null)
                                            viewModel.setCameraBitmap(null)
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                                            .testTag("remove_image_button")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove image", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }

                        SourceKind.PDF -> {
                            Text(
                                text = "Upload a PDF document, article, or textbook chapter",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                                modifier = Modifier.fillMaxWidth().testTag("pick_pdf_button")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (pdfUri != null) "Choose Different PDF" else "Select PDF File")
                            }

                            if (pdfUri != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (pdfPreviewBitmap != null) {
                                            Image(
                                                bitmap = pdfPreviewBitmap!!.asImageBitmap(),
                                                contentDescription = "PDF first page preview",
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.PictureAsPdf,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pdfName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (pdfPageCount > 0) "$pdfPageCount pages • Ready for AI" else "PDF Document • Ready",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.setPdfUri(null) },
                                            modifier = Modifier.testTag("remove_pdf_button")
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear PDF", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }

                        SourceKind.BOOK_TEXT -> {
                            Text(
                                text = "Paste textbook excerpts, notes, or chapter paragraphs:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = bookText,
                                onValueChange = { viewModel.setBookText(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .testTag("book_text_input"),
                                placeholder = {
                                    Text("Paste text here from books, articles, lecture notes, or study guides...")
                                },
                                trailingIcon = {
                                    if (bookText.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setBookText("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear text")
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${bookText.length} characters • ${bookText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size} words",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        SourceKind.PRESET -> {
                            Text(
                                text = "Select a pre-loaded study document to test question generation instantly:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                DocumentUtils.SAMPLE_DOCUMENTS.forEach { sample ->
                                    val isSelected = selectedPreset?.title == sample.title
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectPreset(sample) }
                                            .testTag("sample_item_${sample.category}"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                        ),
                                        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Description,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = sample.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = sample.snippet,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Question Generation Options
        item {
            Text(
                text = "2. Customize Your Quiz",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Question Count Chips
                    Column {
                        Text(
                            text = "Number of Questions: ${settings.questionCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(3, 5, 8, 10).forEach { count ->
                                FilterChip(
                                    selected = settings.questionCount == count,
                                    onClick = { viewModel.setSettings(settings.copy(questionCount = count)) },
                                    label = { Text("$count Qs") },
                                    modifier = Modifier.testTag("count_chip_$count")
                                )
                            }
                        }
                    }

                    // Question Format
                    Column {
                        Text(
                            text = "Question Format",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("MIXED" to "Mixed (Best)", "MCQ" to "Multiple Choice", "TRUE_FALSE" to "True / False", "FLASHCARD" to "Flashcards")) { (typeKey, label) ->
                                FilterChip(
                                    selected = settings.questionType == typeKey,
                                    onClick = { viewModel.setSettings(settings.copy(questionType = typeKey)) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("type_chip_$typeKey")
                                )
                            }
                        }
                    }

                    // Difficulty Level
                    Column {
                        Text(
                            text = "Difficulty Level",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Easy", "Medium", "Hard").forEach { diff ->
                                FilterChip(
                                    selected = settings.difficulty == diff,
                                    onClick = { viewModel.setSettings(settings.copy(difficulty = diff)) },
                                    label = { Text(diff) },
                                    modifier = Modifier.testTag("diff_chip_$diff")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Error Banner or API Key Guidance if error
        if (generationState is GenerationUiState.Error) {
            val err = generationState as GenerationUiState.Error
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("error_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notice",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = err.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.useSampleDemoQuiz() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("try_demo_button")
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch Demo Quiz Instead")
                        }
                    }
                }
            }
        }

        // 4. Loading State Card
        if (generationState is GenerationUiState.Generating) {
            val generating = generationState as GenerationUiState.Generating
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("generating_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing Document with Gemini...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = generating.stepMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // 5. Generate CTA Button
        item {
            val hasContent = when (selectedSource) {
                SourceKind.PICTURE -> imageUri != null || cameraBitmap != null
                SourceKind.PDF -> pdfUri != null
                SourceKind.BOOK_TEXT -> bookText.isNotBlank()
                SourceKind.PRESET -> selectedPreset != null || bookText.isNotBlank()
            }

            val isGenerating = generationState is GenerationUiState.Generating

            Button(
                onClick = { viewModel.generateQuiz() },
                enabled = hasContent && !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("generate_quiz_cta_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isGenerating) "Generating Questions..." else "Generate Quiz from Document",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SourceTabButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(68.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        border = if (!isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
