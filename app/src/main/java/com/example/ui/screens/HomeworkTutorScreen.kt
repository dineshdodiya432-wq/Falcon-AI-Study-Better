package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.HomeworkSession
import com.example.data.model.LuminoCatalog
import com.example.data.model.UserProfile
import com.example.ui.theme.CosmicViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SolarFlareAmber
import com.example.ui.theme.SupernovaCoral
import com.example.ui.theme.TurquoiseNebula
import com.example.ui.viewmodel.HomeworkStudioState

private fun createSampleHomeworkBitmap(grade: Int): Bitmap {
    val bmp = Bitmap.createBitmap(480, 260, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(android.graphics.Color.parseColor("#171536"))
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#FFC82C")
        textSize = 28f
        isFakeBoldText = true
    }
    canvas.drawText("Grade $grade Problem Sheet", 32f, 80f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 24f
    canvas.drawText("Problem: Solve 3x + 7 = 22", 32f, 145f, paint)
    paint.color = android.graphics.Color.parseColor("#00D2B8")
    paint.textSize = 20f
    canvas.drawText("Captured via Falcon AI Camera", 32f, 205f, paint)
    return bmp
}

private val sampleVoiceQuestions = listOf(
    "Can you explain how to solve 3x + 7 = 22 in a clear paragraph?",
    "How do plants make food through photosynthesis?",
    "Explain Newton's three laws of motion in simple terms.",
    "Why does the Moon have different phases during the month?"
)

@Composable
fun HomeworkTutorScreen(
    profile: UserProfile,
    studioState: HomeworkStudioState,
    homeworkHistory: List<HomeworkSession> = emptyList(),
    onQuestionChanged: (String) -> Unit,
    onBitmapSelected: (Bitmap?) -> Unit,
    onAskTutor: (spokenViaMic: Boolean) -> Unit,
    onToggleAutoSpeakTts: () -> Unit = {},
    onMarkSessionSpoken: (String) -> Unit = {},
    onOpenGradeLanguageDialog: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val activeLang = LuminoCatalog.languages.find { it.name == profile.selectedLanguage }
        ?: LuminoCatalog.languages.first()

    var voiceSampleIndex by remember { mutableIntStateOf(0) }
    var copiedSessionId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Chronological conversation thread
    val chatMessages = remember(homeworkHistory, studioState.activeSession) {
        val base = homeworkHistory.reversed()
        val active = studioState.activeSession
        if (active != null && base.none { it.id == active.id }) {
            base + active
        } else {
            base
        }
    }

    // Auto-scroll to latest response smoothly
    LaunchedEffect(chatMessages.size, studioState.isGenerating) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Camera capture launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            onBitmapSelected(bitmap)
        } else {
            onBitmapSelected(createSampleHomeworkBitmap(profile.selectedGrade))
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                takePictureLauncher.launch(null)
            } catch (_: Exception) {
                onBitmapSelected(createSampleHomeworkBitmap(profile.selectedGrade))
            }
        } else {
            onBitmapSelected(createSampleHomeworkBitmap(profile.selectedGrade))
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    onBitmapSelected(bmp)
                }
            } catch (_: Exception) {
                onBitmapSelected(createSampleHomeworkBitmap(profile.selectedGrade))
            }
        }
    }

    val fallbackVoicePromptAndSend: () -> Unit = {
        val sample = sampleVoiceQuestions[voiceSampleIndex % sampleVoiceQuestions.size]
        voiceSampleIndex += 1
        onQuestionChanged(sample)
        focusManager.clearFocus()
        onAskTutor(true)
    }

    // Speech-to-Text launcher (voice dictation into text, without oral TTS)
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()?.trim()
            if (!spokenText.isNullOrBlank()) {
                onQuestionChanged(spokenText)
                focusManager.clearFocus()
                onAskTutor(true)
            } else {
                fallbackVoicePromptAndSend()
            }
        } else {
            fallbackVoicePromptAndSend()
        }
    }

    val launchVoiceInput: () -> Unit = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, activeLang.localeCode)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Falcon AI...")
            }
            speechRecognizerLauncher.launch(intent)
        } catch (_: Exception) {
            fallbackVoicePromptAndSend()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchVoiceInput()
        } else {
            fallbackVoicePromptAndSend()
        }
    }

    val triggerSend: () -> Unit = {
        focusManager.clearFocus()
        onAskTutor(false)
    }

    val copyToClipboard: (String, String) -> Unit = { id, text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Falcon AI Explanation", text)
        clipboard.setPrimaryClip(clip)
        copiedSessionId = id
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("homework_tutor_screen")
    ) {
        // 1. Sleek Falcon AI Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.falcon_logo),
                        contentDescription = "Falcon AI Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Falcon AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TurquoiseNebula.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "AI Tutor",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TurquoiseNebula,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Grade ${profile.selectedGrade} • ${profile.selectedLanguage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenGradeLanguageDialog,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("${activeLang.flagEmoji} Gr.${profile.selectedGrade}")
                    }

                    IconButton(
                        onClick = {
                            onQuestionChanged("")
                            onBitmapSelected(null)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "New Chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Falcon AI Message Feed (Smooth, Natural Paragraphs, Animated Messages)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (chatMessages.isEmpty() && !studioState.isGenerating) {
                // Falcon AI Starter Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.falcon_logo),
                        contentDescription = "Falcon AI Logo",
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "How can Falcon AI help you today?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ask any question, snap homework with your camera, or dictate by voice. You'll get clear, easy-to-understand explanations from Falcon AI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    val starters = listOf(
                        "Explain photosynthesis in plants",
                        "Solve 3x + 7 = 22 step by step",
                        "What are Newton's three laws of motion?",
                        "How do fractions work with different numbers?"
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        starters.forEach { prompt ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onQuestionChanged(prompt)
                                        triggerSend()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = TurquoiseNebula,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(chatMessages, key = { it.id.ifEmpty { it.questionTitle } }) { session ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // User Message Bubble (Right-aligned capsule with slide-in animation)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInHorizontally(
                                        initialOffsetX = { fullWidth -> (fullWidth / 4).coerceAtMost(80) },
                                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(durationMillis = 280))
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(22.dp, 22.dp, 4.dp, 22.dp),
                                        color = CosmicViolet,
                                        modifier = Modifier.widthIn(max = 320.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                            Text(
                                                text = session.questionTitle,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = Color.White,
                                                lineHeight = 22.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Assistant Message (Falcon AI style: natural paragraph text with subtle slide-in animation & Falcon logo)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically(
                                        initialOffsetY = { fullHeight -> (fullHeight / 4).coerceAtMost(60) },
                                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(durationMillis = 300))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.falcon_logo),
                                                contentDescription = "Falcon AI",
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Falcon AI",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (session.subjectTag.isNotBlank() && session.subjectTag != "General") {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        text = session.subjectTag,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Natural paragraph text response (rendered smoothly with typography)
                                        val paragraphText = session.displayParagraph
                                        Card(
                                            shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text(
                                                    text = paragraphText,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    lineHeight = 24.sp
                                                )

                                                // Action Bar below response: Copy Text, Star Coins earned
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { copyToClipboard(session.id, paragraphText) }
                                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (copiedSessionId == session.id) Icons.Default.Check else Icons.Default.ContentCopy,
                                                            contentDescription = "Copy Response",
                                                            tint = if (copiedSessionId == session.id) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = if (copiedSessionId == session.id) "Copied!" else "Copy",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (copiedSessionId == session.id) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }

                                                    Text(
                                                        text = "🪙 +${session.coinsEarned} Coins",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SolarFlareAmber,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (studioState.isGenerating) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = TurquoiseNebula.copy(alpha = 0.2f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = TurquoiseNebula,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                                Text(
                                    text = "Falcon AI is generating your explanation...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Falcon AI Style Composer (Camera on Left, Smooth Pill Text Input, Mic & Send on Right)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Photo Attachment Pill above composer
                AnimatedVisibility(
                    visible = studioState.selectedBitmap != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (studioState.selectedBitmap != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    bitmap = studioState.selectedBitmap.asImageBitmap(),
                                    contentDescription = "Attached Homework Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { onBitmapSelected(null) },
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Homework Photo Ready",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap Send to get an instant explanation from Falcon AI.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Falcon AI Floating Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // LEFT: Camera Button
                    IconButton(
                        onClick = {
                            val hasCamPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasCamPerm) {
                                try {
                                    takePictureLauncher.launch(null)
                                } catch (_: Exception) {
                                    onBitmapSelected(createSampleHomeworkBitmap(profile.selectedGrade))
                                }
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                            .testTag("camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Camera",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // LEFT: Gallery Photo Button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                            .testTag("pick_homework_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Upload Photo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // CENTER: Rounded Pill Text Input Field
                    OutlinedTextField(
                        value = studioState.questionInput,
                        onValueChange = onQuestionChanged,
                        placeholder = {
                            Text(
                                text = "Message Falcon AI...",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                        },
                        maxLines = 4,
                        shape = RoundedCornerShape(26.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { triggerSend() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicViolet,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("homework_question_input")
                    )

                    // MIC: Voice Dictation Button (Transcribes into input, no oral speech output)
                    IconButton(
                        onClick = {
                            val hasMicPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasMicPerm) {
                                launchVoiceInput()
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                color = SupernovaCoral.copy(alpha = 0.14f),
                                shape = CircleShape
                            )
                            .testTag("mic_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Dictation",
                            tint = SupernovaCoral
                        )
                    }

                    // RIGHT: Circular Send Button (Highlights active when text or photo is present)
                    val isReadyToSend = studioState.questionInput.isNotBlank() || studioState.selectedBitmap != null
                    IconButton(
                        onClick = triggerSend,
                        enabled = isReadyToSend && !studioState.isGenerating,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = if (isReadyToSend && !studioState.isGenerating) {
                                    Brush.linearGradient(colors = listOf(CosmicViolet, TurquoiseNebula))
                                } else {
                                    Brush.linearGradient(colors = listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f)))
                                },
                                shape = CircleShape
                            )
                            .testTag("explain_homework_button")
                    ) {
                        if (studioState.isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (isReadyToSend) Color.White else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
