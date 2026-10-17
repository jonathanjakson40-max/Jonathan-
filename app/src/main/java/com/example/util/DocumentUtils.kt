package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import com.example.data.model.Question
import com.example.data.model.QuestionType
import com.example.data.model.SampleDoc
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

object DocumentUtils {

    fun getFileName(context: Context, uri: Uri): String {
        var name = "uploaded_file"
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = it.getString(index)
                    }
                }
            }
        } catch (_: Exception) {
            name = uri.lastPathSegment ?: "document"
        }
        return name
    }

    fun getFileSizeString(context: Context, uri: Uri): String {
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0) {
                        val bytes = it.getLong(sizeIndex)
                        return formatBytes(bytes)
                    }
                }
            }
        } catch (_: Exception) {}
        return "Unknown size"
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
        }
    }

    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return null
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun renderPdfFirstPage(context: Context, uri: Uri): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            renderer = PdfRenderer(pfd)
            if (renderer.pageCount == 0) return null
            page = renderer.openPage(0)

            val width = 600
            val height = (width.toFloat() / page.width * page.height).toInt().coerceAtLeast(300)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            // White background for PDF
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { page?.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    fun getPdfPageCount(context: Context, uri: Uri): Int {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return 0
            renderer = PdfRenderer(pfd)
            renderer.pageCount
        } catch (_: Exception) {
            0
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    fun uriToOptimizedImageBase64(context: Context, uri: Uri, maxDimension: Int = 1600): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            bitmapToBase64(originalBitmap, maxDimension)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun bitmapToBase64(bitmap: Bitmap, maxDimension: Int = 1600): String {
        val width = bitmap.width
        val height = bitmap.height
        val maxSide = max(width, height)
        val scaledBitmap = if (maxSide > maxDimension) {
            val scale = maxDimension.toFloat() / maxSide
            val matrix = Matrix()
            matrix.postScale(scale, scale)
            Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    val SAMPLE_DOCUMENTS: List<SampleDoc> = listOf(
        SampleDoc(
            title = "Biology: Cell Biology & Respiration",
            category = "Science",
            snippet = "Cellular respiration is a metabolic pathway that breaks down glucose and produces ATP...",
            fullContent = """
Cellular Respiration and Energy Production

Cellular respiration is a set of metabolic reactions and processes that take place in the cells of organisms to convert biochemical energy from nutrients into adenosine triphosphate (ATP), and then release waste products. The catabolic reactions involved in respiration include glycolysis, the citric acid cycle (Krebs cycle), and oxidative phosphorylation via the electron transport chain.

1. Glycolysis: Occurring in the cytoplasm, glycolysis breaks down one 6-carbon glucose molecule into two 3-carbon pyruvate molecules. This anaerobic phase yields a net gain of 2 ATP and 2 NADH molecules without requiring oxygen.

2. Citric Acid Cycle (Krebs Cycle): In eukaryotes, pyruvate is transported into the mitochondrial matrix and converted to Acetyl-CoA. Acetyl-CoA enters the cycle, producing carbon dioxide (CO2), 2 ATP, 6 NADH, and 2 FADH2 per glucose molecule.

3. Oxidative Phosphorylation: Located on the inner mitochondrial cristae, electrons from NADH and FADH2 pass through transmembrane protein complexes. As electrons travel, protons (H+) are pumped into the intermembrane space, generating an electrochemical gradient. Protons flow back through ATP Synthase (chemiosmosis), producing approximately 28 to 32 ATP molecules. Oxygen acts as the final electron acceptor, combining with electrons and protons to produce metabolic water (H2O).
            """.trimIndent()
        ),
        SampleDoc(
            title = "History: The Roman Republic & Law",
            category = "History",
            snippet = "The Roman Republic lasted from 509 BC to 27 BC, establishing early principles of constitutional governance...",
            fullContent = """
The Roman Republic and Constitutional Evolution

The Roman Republic was the era of classical Roman civilization beginning with the overthrow of the Roman monarchy (traditionally dated to 509 BC) and ending in 27 BC with the establishment of the Roman Empire under Augustus Caesar.

Key Governmental Structures:
- The Consuls: Two magistrates elected annually holding supreme military command (imperium) and civil authority. Their power was balanced by mutual veto power (intercessio).
- The Senate: An advisory council of aristocratic patricians who controlled foreign policy, state finance, and religious affairs.
- Tribunes of the Plebs: Officials elected exclusively to protect plebeians (common citizens) from patrician magistrates, possessing sacrosanct status and veto power over senatorial decrees.

The Twelve Tables (circa 450 BC):
The Twelve Tables represented the earliest codification of Roman civil law, inscribed on bronze tablets in the Roman Forum. This marked a watershed transition from arbitrary oral decrees to public, codified statutory rights. Fundamental principles such as the presumption of innocence, equality before civil statutes, and trial by jury originated from these ancient institutions.
            """.trimIndent()
        ),
        SampleDoc(
            title = "Physics: Newton's Laws & Momentum",
            category = "Physics",
            snippet = "Sir Isaac Newton's three laws of motion describe the mathematical relationship between forces and movement...",
            fullContent = """
Newton's Laws of Motion and Principles of Momentum

In 1687, Sir Isaac Newton published 'Philosophiae Naturalis Principia Mathematica', delineating three fundamental laws governing classical mechanics:

First Law (Law of Inertia): An object at rest remains at rest, and an object in uniform motion continues in straight-line motion at constant velocity unless acted upon by a net external force. Inertia depends directly on an object's mass.

Second Law (Law of Force and Acceleration): The acceleration of a body is directly proportional to the net force acting upon it and inversely proportional to its mass, represented mathematically as F = ma (Force = mass × acceleration). More generally, force equals the time rate of change of linear momentum (p = mv).

Third Law (Action and Reaction): When one body exerts a force on a second body, the second body simultaneously exerts a force equal in magnitude and opposite in direction on the first body. 

Conservation of Momentum: In an isolated closed system where no external forces act, the total linear momentum remains invariant over time, essential for calculating collision dynamics and rocket propulsion.
            """.trimIndent()
        ),
        SampleDoc(
            title = "Literature: Shakespeare's Dramatic Tragedy",
            category = "Literature",
            snippet = "William Shakespeare's tragedies explore hamartia, fate, moral corruption, and catharsis...",
            fullContent = """
The Structure of Shakespearean Tragedy

William Shakespeare's tragic masterpieces—including Hamlet, Macbeth, Othello, and King Lear—adhere to distinct dramatic conventions rooted in Aristotle's Poetics while innovating Renaissance psychological depth.

Central Tragic Conventions:
1. The Tragic Hero: Characters of noble birth or high stature possessing greatness of soul, who suffer a catastrophic downfall.
2. Hamartia (Tragic Flaw or Error in Judgment): The protagonist's fatal predisposition—such as Hamlet's debilitating indecision, Macbeth's vaulting ambition, or Othello's vulnerable jealousy—which precipitates their doom.
3. Hubris & Fate: Excessive pride leading characters to defy societal or cosmic orders, accompanied by external forces or omens.
4. Catharsis & Restoration of Moral Order: The tragic denouement purges pity and fear from the audience. Though the protagonist dies, the chaotic disruption to the state is healed as legitimate authority is restored (e.g., Fortinbras in Hamlet, Malcolm in Macbeth).
            """.trimIndent()
        )
    )

    fun parseQuizJson(
        jsonString: String,
        fallbackTitle: String = "Generated Quiz",
        sourceName: String = "Uploaded Document"
    ): Pair<String, List<Question>> {
        var cleanJson = jsonString.trim()
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.removePrefix("```json").trim()
        }
        if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.removePrefix("```").trim()
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.removeSuffix("```").trim()
        }

        val questionList = mutableListOf<Question>()
        var quizTitle = fallbackTitle
        var quizSummary = "Quiz generated from $sourceName"

        try {
            val root = JSONObject(cleanJson)
            if (root.has("title")) {
                quizTitle = root.getString("title")
            }
            if (root.has("summary")) {
                quizSummary = root.getString("summary")
            }

            val questionsArray = root.optJSONArray("questions") ?: JSONArray()
            for (i in 0 until questionsArray.length()) {
                val qObj = questionsArray.getJSONObject(i)
                val typeStr = qObj.optString("type", "MCQ")
                val type = when (typeStr.uppercase()) {
                    "TRUE_FALSE", "TF" -> QuestionType.TRUE_FALSE
                    "FLASHCARD" -> QuestionType.FLASHCARD
                    "FILL_BLANK", "FILL_IN_THE_BLANK" -> QuestionType.FILL_BLANK
                    else -> QuestionType.MCQ
                }

                val questionText = qObj.optString("question", "Question ${i + 1}")
                val optionsList = mutableListOf<String>()
                val optsArray = qObj.optJSONArray("options")
                if (optsArray != null) {
                    for (j in 0 until optsArray.length()) {
                        optionsList.add(optsArray.getString(j))
                    }
                }

                val correctAnswer = qObj.optString("correctAnswer", "")
                val correctIndex = qObj.optInt("correctIndex", if (optionsList.isNotEmpty()) 0 else -1)
                val explanation = qObj.optString("explanation", "")
                val hint = qObj.optString("hint", "")

                questionList.add(
                    Question(
                        id = (i + 1).toString(),
                        questionText = questionText,
                        type = type,
                        options = optionsList,
                        correctAnswer = correctAnswer,
                        correctIndex = correctIndex,
                        explanation = explanation,
                        hint = hint
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Pair(quizTitle, questionList)
    }
}
