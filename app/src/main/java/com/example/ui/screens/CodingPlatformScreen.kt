package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.coding.AiCodeAction
import com.example.data.coding.AiCodeAssistantResponse
import com.example.data.coding.ChallengeType
import com.example.data.coding.CodeExecutionEngine
import com.example.data.coding.CodingCurriculumCatalog
import com.example.data.coding.CodingExecutionResult
import com.example.data.coding.CodingLesson
import com.example.data.coding.CodingProgressStore
import com.example.data.coding.CodingProject
import com.example.data.coding.CodingSubTab
import com.example.data.coding.FalconCodeAiService
import com.example.data.coding.PracticeChallenge
import com.example.data.coding.ProgrammingLanguageId
import com.example.data.coding.TestCaseRunResult
import com.example.data.model.UserProfile
import com.example.ui.theme.CosmicIndigo
import com.example.ui.theme.CosmicViolet
import com.example.ui.theme.DeepSpaceBackground
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SolarFlareAmber
import com.example.ui.theme.StarlightGold
import com.example.ui.theme.SupernovaCoral
import com.example.ui.theme.TurquoiseNebula
import kotlinx.coroutines.launch

@Composable
fun CodingPlatformScreen(
    profile: UserProfile,
    onAwardCodingReward: (coins: Int, xp: Int, message: String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val progressStore = remember { CodingProgressStore(context) }
    val executionEngine = remember { CodeExecutionEngine() }
    val codeAiService = remember { FalconCodeAiService() }

    val userKey = profile.userId.ifBlank { "default_scholar" }
    var codingProgress by remember(userKey) {
        mutableStateOf(progressStore.loadProgress(userKey))
    }

    val selectedLanguage = codingProgress.selectedLanguage
    val languageLessons = remember(selectedLanguage) {
        CodingCurriculumCatalog.getLessonsForLanguage(selectedLanguage)
    }
    val languageChallenges = remember(selectedLanguage) {
        CodingCurriculumCatalog.getPracticeChallenges(selectedLanguage)
    }
    val languageProjects = remember(selectedLanguage) {
        CodingCurriculumCatalog.getProjectsForLanguage(selectedLanguage)
    }
    val roadmapStages = remember(selectedLanguage) {
        CodingCurriculumCatalog.getThreeMonthRoadmap(selectedLanguage)
    }

    var activeSubTab by rememberSaveable { mutableStateOf(CodingSubTab.LEARN) }
    var activeLessonId by rememberSaveable { mutableStateOf<String?>(null) }
    val activeLesson = remember(activeLessonId, languageLessons) {
        languageLessons.find { it.id == activeLessonId }
    }

    // Playground State — starts EMPTY unless the user previously wrote and saved code
    var playgroundCode by rememberSaveable(selectedLanguage) {
        mutableStateOf(
            codingProgress.savedCodeByLanguage[selectedLanguage.key] ?: ""
        )
    }
    var playgroundStdin by rememberSaveable { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }
    var executionResult by remember { mutableStateOf<CodingExecutionResult?>(null) }
    var isAiLoading by remember { mutableStateOf(false) }
    var aiResponse by remember { mutableStateOf<AiCodeAssistantResponse?>(null) }

    fun updateAndPersistProgress(updated: com.example.data.coding.UserCodingProgress) {
        codingProgress = updated
        progressStore.saveProgress(userKey, updated)
    }

    fun switchLanguage(newLang: ProgrammingLanguageId) {
        val updatedCodeMap = codingProgress.savedCodeByLanguage.toMutableMap()
        updatedCodeMap[selectedLanguage.key] = playgroundCode
        val nextProgress = codingProgress.copy(
            selectedLanguage = newLang,
            savedCodeByLanguage = updatedCodeMap
        )
        updateAndPersistProgress(nextProgress)
        playgroundCode = updatedCodeMap[newLang.key] ?: ""
        executionResult = null
        aiResponse = null
        activeLessonId = null
    }

    fun executePlaygroundCode(codeToRun: String = playgroundCode, stdinToRun: String = playgroundStdin) {
        if (isExecuting) return
        playgroundCode = codeToRun
        playgroundStdin = stdinToRun
        val updatedMap = codingProgress.savedCodeByLanguage.toMutableMap()
        updatedMap[selectedLanguage.key] = codeToRun
        isExecuting = true
        scope.launch {
            val res = try {
                executionEngine.executeCode(
                    language = selectedLanguage,
                    code = codeToRun,
                    stdin = stdinToRun
                )
            } catch (e: Exception) {
                CodingExecutionResult(
                    stdout = "",
                    stderr = "Execution service error: ${e.message ?: "Runtime temporarily unavailable. Your code has been preserved."}",
                    exitCode = 1,
                    executionTimeMs = 1L,
                    runtimeUsed = selectedLanguage.runtimeLabel,
                    compilationStatus = "Runtime Error",
                    errorType = "RuntimeError",
                    errorSummary = "An unexpected error occurred while running the ${selectedLanguage.displayName} runtime. Your code is safe in the editor."
                )
            }
            executionResult = res
            isExecuting = false
            updateAndPersistProgress(
                codingProgress.copy(
                    savedCodeByLanguage = updatedMap,
                    totalExecutions = codingProgress.totalExecutions + 1
                )
            )
        }
    }

    if (activeLesson != null) {
        BackHandler {
            activeLessonId = null
        }
        InteractiveLessonDetailScreen(
            lesson = activeLesson,
            isCompleted = codingProgress.completedLessonIds.contains(activeLesson.id),
            executionEngine = executionEngine,
            codeAiService = codeAiService,
            onBack = { activeLessonId = null },
            onOpenInPlayground = { exampleCode, sampleStdin ->
                playgroundCode = exampleCode
                if (sampleStdin.isNotEmpty()) playgroundStdin = sampleStdin
                activeLessonId = null
                activeSubTab = CodingSubTab.FALCON_CODE
            },
            onRecordWeakTopic = { topic ->
                updateAndPersistProgress(
                    codingProgress.copy(weakTopics = codingProgress.weakTopics + topic)
                )
            },
            onCompleteLesson = { completedLesson ->
                val alreadyDone = codingProgress.completedLessonIds.contains(completedLesson.id)
                val updatedProgress = codingProgress.copy(
                    completedLessonIds = codingProgress.completedLessonIds + completedLesson.id,
                    masteredTopics = codingProgress.masteredTopics + completedLesson.topicTag,
                    weakTopics = codingProgress.weakTopics - completedLesson.topicTag,
                    codingXp = codingProgress.codingXp + if (alreadyDone) 5 else completedLesson.xpReward
                )
                updateAndPersistProgress(updatedProgress)
                if (!alreadyDone) {
                    onAwardCodingReward(
                        completedLesson.coinsReward,
                        completedLesson.xpReward,
                        "Completed ${completedLesson.title}! +${completedLesson.coinsReward} Coins"
                    )
                }
                // Advance to next lesson if available
                val currIdx = languageLessons.indexOfFirst { it.id == completedLesson.id }
                val nextLesson = languageLessons.getOrNull(currIdx + 1)
                activeLessonId = nextLesson?.id
            }
        )
        return
    }

    val completedInCurrentLang = languageLessons.count { codingProgress.completedLessonIds.contains(it.id) }
    val recommendedNextLesson = languageLessons.firstOrNull { !codingProgress.completedLessonIds.contains(it.id) }
        ?: languageLessons.first()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("coding_platform_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TOP EXECUTE & MODE BAR ("Falcon Code" Execute Tab right at the top!)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(
                    1.5.dp,
                    if (activeSubTab == CodingSubTab.FALCON_CODE) EmeraldSuccess else CosmicViolet.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldSuccess.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, EmeraldSuccess)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "EXECUTE TAB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldSuccess,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Run ${selectedLanguage.displayName} code directly without tutorials",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (activeSubTab == CodingSubTab.FALCON_CODE) {
                                OutlinedButton(
                                    onClick = { activeSubTab = CodingSubTab.LEARN },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, CosmicViolet),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("top_back_to_dashboard_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Dashboard",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (activeSubTab == CodingSubTab.FALCON_CODE) {
                                        executePlaygroundCode(playgroundCode, playgroundStdin)
                                    } else {
                                        activeSubTab = CodingSubTab.FALCON_CODE
                                    }
                                },
                                enabled = !isExecuting,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldSuccess,
                                    contentColor = Color(0xFF052E16)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("top_falcon_code_switch_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (activeSubTab == CodingSubTab.FALCON_CODE) {
                                        if (isExecuting) "Running..." else "Execute Code"
                                    } else {
                                        "Falcon Code (Execute)"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // Top Navigation Tabs Row (Languages & Learn | Falcon Code (Execute) | Practice | Projects | 90-Day Roadmap)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CodingSubTab.entries.forEach { tab ->
                            val selected = activeSubTab == tab
                            val isFalconCodeTab = tab == CodingSubTab.FALCON_CODE
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    selected && isFalconCodeTab -> EmeraldSuccess
                                    selected -> CosmicViolet
                                    isFalconCodeTab -> EmeraldSuccess.copy(alpha = 0.16f)
                                    else -> Color(0xFF1E293B)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        selected && isFalconCodeTab -> EmeraldSuccess
                                        selected -> TurquoiseNebula
                                        isFalconCodeTab -> EmeraldSuccess.copy(alpha = 0.65f)
                                        else -> Color(0xFF334155)
                                    }
                                ),
                                modifier = Modifier
                                    .clickable {
                                        if (activeSubTab == tab && isFalconCodeTab) {
                                            executePlaygroundCode(playgroundCode, playgroundStdin)
                                        } else {
                                            activeSubTab = tab
                                        }
                                    }
                                    .testTag("coding_subtab_${tab.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isFalconCodeTab) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = if (selected) Color(0xFF052E16) else EmeraldSuccess,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = when {
                                            selected && isFalconCodeTab -> Color(0xFF052E16)
                                            selected -> Color.White
                                            isFalconCodeTab -> EmeraldSuccess
                                            else -> Color(0xFFCBD5E1)
                                        },
                                        fontWeight = if (selected || isFalconCodeTab) FontWeight.ExtraBold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Active Sub-Tab Content
        when (activeSubTab) {
            CodingSubTab.LEARN -> {
                // Hero Dashboard Card
                item {
                    Card(
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = CosmicIndigo),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(DeepSpaceBackground, CosmicIndigo, Color(0xFF1E1B4B))
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            painter = painterResource(id = R.drawable.falcon_logo),
                                            contentDescription = "Falcon AI Coding",
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Falcon AI Coding Hub",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Language Dashboard • Falcon Code Runner • 90d Roadmap",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFCBD5E1)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = StarlightGold.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, StarlightGold)
                                    ) {
                                        Text(
                                            text = "⚡ ${codingProgress.codingXp} XP",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = StarlightGold,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                // Selected Language Progress & Continue / Falcon Code Buttons
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, TurquoiseNebula.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${selectedLanguage.displayName} Track • $completedInCurrentLang / ${languageLessons.size} Lessons",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TurquoiseNebula,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${((completedInCurrentLang.toFloat() / languageLessons.size.coerceAtLeast(1)) * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        LinearProgressIndicator(
                                            progress = { completedInCurrentLang.toFloat() / languageLessons.size.coerceAtLeast(1) },
                                            color = TurquoiseNebula,
                                            trackColor = Color.White.copy(alpha = 0.15f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(7.dp)
                                                .clip(RoundedCornerShape(50))
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Up Next in ${selectedLanguage.displayName}:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF94A3B8)
                                                )
                                                Text(
                                                    text = recommendedNextLesson.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = { activeSubTab = CodingSubTab.FALCON_CODE },
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, EmeraldSuccess),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                                    modifier = Modifier.testTag("hero_open_falcon_code_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Falcon Code", fontWeight = FontWeight.Bold)
                                                }

                                                Button(
                                                    onClick = { activeLessonId = recommendedNextLesson.id },
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = TurquoiseNebula,
                                                        contentColor = CosmicIndigo
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                                    modifier = Modifier.testTag("continue_learning_button")
                                                ) {
                                                    Text("Continue", fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
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

                // Primary Language-Selection Dashboard (Featured: Python, JS, C++ + Java, C, HTML/CSS)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Primary Language-Selection Dashboard",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Choose a language to learn or tap 'Falcon Code' to execute code immediately",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Horizontal Quick Selector Strip (preserves language_selector_row testTag)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("language_selector_row")
                        ) {
                            items(ProgrammingLanguageId.entries) { lang ->
                                val isSelected = lang == selectedLanguage
                                val langLessons = CodingCurriculumCatalog.getLessonsForLanguage(lang)
                                val doneCount = langLessons.count { codingProgress.completedLessonIds.contains(it.id) }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) CosmicViolet else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) TurquoiseNebula else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .clickable { switchLanguage(lang) }
                                        .testTag("select_lang_${lang.key}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(lang.accentHex).copy(alpha = 0.25f),
                                            border = BorderStroke(1.dp, Color(lang.accentHex))
                                        ) {
                                            Text(
                                                text = lang.shortBadge,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = lang.displayName,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (lang.isPrimaryFeatured) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "★",
                                                        color = StarlightGold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "$doneCount/${langLessons.size} lessons",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Detailed Language Dashboard Cards (with direct "Falcon Code (Execute)" + "Learn" buttons)
                        ProgrammingLanguageId.entries.chunked(2).forEach { rowLangs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowLangs.forEach { lang ->
                                    val isSelected = lang == selectedLanguage
                                    val langLessons = CodingCurriculumCatalog.getLessonsForLanguage(lang)
                                    val doneCount = langLessons.count { codingProgress.completedLessonIds.contains(it.id) }
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                CosmicIndigo.copy(alpha = 0.92f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) TurquoiseNebula else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { switchLanguage(lang) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(lang.accentHex).copy(alpha = 0.25f),
                                                    border = BorderStroke(1.dp, Color(lang.accentHex))
                                                ) {
                                                    Text(
                                                        text = lang.shortBadge,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                if (lang.isPrimaryFeatured) {
                                                    Text(
                                                        text = "★ Featured",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = StarlightGold,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Text(
                                                text = lang.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "$doneCount / ${langLessons.size} Lessons",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) TurquoiseNebula else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Button(
                                                onClick = {
                                                    switchLanguage(lang)
                                                    activeSubTab = CodingSubTab.FALCON_CODE
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = EmeraldSuccess,
                                                    contentColor = Color(0xFF052E16)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(34.dp)
                                                    .testTag("dashboard_falcon_code_${lang.key}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Falcon Code",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                val groupedByModule = languageLessons.groupBy { it.moduleNumber to it.moduleTitle }
                groupedByModule.forEach { (moduleHeader, lessonsInModule) ->
                    item(key = "module_${moduleHeader.first}") {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val doneInMod = lessonsInModule.count { codingProgress.completedLessonIds.contains(it.id) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = moduleHeader.second,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = CosmicViolet.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "$doneInMod/${lessonsInModule.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CosmicViolet,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                lessonsInModule.forEach { lesson ->
                                    val isDone = codingProgress.completedLessonIds.contains(lesson.id)
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isDone) {
                                            EmeraldSuccess.copy(alpha = 0.1f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isDone) EmeraldSuccess.copy(alpha = 0.5f) else Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { activeLessonId = lesson.id }
                                            .testTag("lesson_card_${lesson.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.School,
                                                    contentDescription = null,
                                                    tint = if (isDone) EmeraldSuccess else CosmicViolet,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = lesson.title,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = "${lesson.difficulty} • ${lesson.topicTag} • +${lesson.coinsReward} 🪙",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Open Lesson",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CodingSubTab.FALCON_CODE -> {
                item(key = "falcon_code_playground") {
                    ProfessionalCodePlayground(
                        selectedLanguage = selectedLanguage,
                        code = playgroundCode,
                        stdin = playgroundStdin,
                        isExecuting = isExecuting,
                        executionResult = executionResult,
                        isAiLoading = isAiLoading,
                        aiResponse = aiResponse,
                        onCodeChanged = { updatedCode ->
                            playgroundCode = updatedCode
                            val updatedMap = codingProgress.savedCodeByLanguage.toMutableMap()
                            updatedMap[selectedLanguage.key] = updatedCode
                            updateAndPersistProgress(codingProgress.copy(savedCodeByLanguage = updatedMap))
                        },
                        onStdinChanged = { playgroundStdin = it },
                        onRunCode = {
                            executePlaygroundCode(playgroundCode, playgroundStdin)
                        },
                        onResetStarterCode = {
                            val exampleCode = CodingCurriculumCatalog.getStarterCode(selectedLanguage)
                            playgroundCode = exampleCode
                            val updatedMap = codingProgress.savedCodeByLanguage.toMutableMap()
                            updatedMap[selectedLanguage.key] = exampleCode
                            updateAndPersistProgress(codingProgress.copy(savedCodeByLanguage = updatedMap))
                            executionResult = null
                            onShowMessage("Loaded ${selectedLanguage.displayName} example into editor")
                        },
                        onRequestAiAction = { action, convertTarget, customQuestion ->
                            if (isAiLoading) return@ProfessionalCodePlayground
                            isAiLoading = true
                            scope.launch {
                                aiResponse = codeAiService.assistWithCode(
                                    action = action,
                                    language = selectedLanguage,
                                    code = playgroundCode,
                                    lastResult = executionResult,
                                    customQuestion = customQuestion,
                                    targetConvertLanguage = convertTarget
                                )
                                isAiLoading = false
                            }
                        },
                        onApplySuggestedCode = { suggested, targetLang ->
                            if (targetLang != null && targetLang != selectedLanguage) {
                                switchLanguage(targetLang)
                            }
                            playgroundCode = suggested
                            val updatedMap = codingProgress.savedCodeByLanguage.toMutableMap()
                            val activeKey = (targetLang ?: selectedLanguage).key
                            updatedMap[activeKey] = suggested
                            updateAndPersistProgress(codingProgress.copy(savedCodeByLanguage = updatedMap))
                            onShowMessage("Applied corrected code to editor — tap Execute Code to verify!")
                        },
                        onShowMessage = onShowMessage,
                        onSelectLanguage = { newLang ->
                            switchLanguage(newLang)
                        },
                        onClearConsole = {
                            executionResult = null
                        },
                        onDismissAiResponse = {
                            aiResponse = null
                        }
                    )
                }
            }

            CodingSubTab.PRACTICE -> {
                items(languageChallenges, key = { it.id }) { challenge ->
                    PracticeChallengeCard(
                        challenge = challenge,
                        isCompleted = codingProgress.completedChallengeIds.contains(challenge.id),
                        executionEngine = executionEngine,
                        onCompleted = {
                            val firstTime = !codingProgress.completedChallengeIds.contains(challenge.id)
                            updateAndPersistProgress(
                                codingProgress.copy(
                                    completedChallengeIds = codingProgress.completedChallengeIds + challenge.id,
                                    masteredTopics = codingProgress.masteredTopics + challenge.topicTag,
                                    weakTopics = codingProgress.weakTopics - challenge.topicTag,
                                    codingXp = codingProgress.codingXp + if (firstTime) challenge.xpReward else 5
                                )
                            )
                            if (firstTime) {
                                onAwardCodingReward(
                                    challenge.coinsReward,
                                    challenge.xpReward,
                                    "Challenge Solved! +${challenge.coinsReward} Coins"
                                )
                            }
                        },
                        onFailedAttempt = {
                            updateAndPersistProgress(
                                codingProgress.copy(weakTopics = codingProgress.weakTopics + challenge.topicTag)
                            )
                        }
                    )
                }
            }

            CodingSubTab.PROJECTS -> {
                items(languageProjects, key = { it.id }) { project ->
                    CodingProjectCard(
                        project = project,
                        isCompleted = codingProgress.completedProjectIds.contains(project.id),
                        onOpenInPlayground = {
                            playgroundCode = project.starterCode
                            if (project.sampleStdin.isNotEmpty()) playgroundStdin = project.sampleStdin
                            activeSubTab = CodingSubTab.FALCON_CODE
                        },
                        onVerifyAndComplete = { userCode ->
                            scope.launch {
                                val res = executionEngine.executeCode(project.languageId, userCode, project.sampleStdin)
                                val allMatched = res.isSuccess && project.expectedOutputKeywords.all { kw ->
                                    res.stdout.contains(kw, ignoreCase = true)
                                }
                                if (allMatched) {
                                    val firstTime = !codingProgress.completedProjectIds.contains(project.id)
                                    updateAndPersistProgress(
                                        codingProgress.copy(
                                            completedProjectIds = codingProgress.completedProjectIds + project.id,
                                            codingXp = codingProgress.codingXp + if (firstTime) project.xpReward else 10
                                        )
                                    )
                                    if (firstTime) {
                                        onAwardCodingReward(
                                            project.coinsReward,
                                            project.xpReward,
                                            "Project Completed: ${project.title}! +${project.coinsReward} Coins"
                                        )
                                    } else {
                                        onShowMessage("Project verified successfully!")
                                    }
                                } else {
                                    onShowMessage("Output didn't match project requirements yet. Expected: ${project.expectedOutputKeywords.joinToString()}")
                                }
                            }
                        }
                    )
                }
            }

            CodingSubTab.ROADMAP -> {
                item {
                    ProgressAndRoadmapSection(
                        selectedLanguage = selectedLanguage,
                        progress = codingProgress,
                        totalLessons = languageLessons.size,
                        completedLessonsInLang = completedInCurrentLang,
                        roadmapStages = roadmapStages,
                        onUpdateStudyGoal = { mins ->
                            updateAndPersistProgress(codingProgress.copy(dailyStudyMinutesGoal = mins))
                            onShowMessage("Daily coding goal set to $mins mins/day")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveLessonDetailScreen(
    lesson: CodingLesson,
    isCompleted: Boolean,
    executionEngine: CodeExecutionEngine,
    codeAiService: FalconCodeAiService,
    onBack: () -> Unit,
    onOpenInPlayground: (String, String) -> Unit,
    onRecordWeakTopic: (String) -> Unit,
    onCompleteLesson: (CodingLesson) -> Unit
) {
    val scope = rememberCoroutineScope()
    var showSimplerAnalogy by rememberSaveable(lesson.id) { mutableStateOf(false) }

    // Exercise state
    var exerciseCode by remember(lesson.id) { mutableStateOf(lesson.exerciseStarterCode) }
    var exerciseResult by remember(lesson.id) { mutableStateOf<CodingExecutionResult?>(null) }
    var exercisePassed by remember(lesson.id) { mutableStateOf(false) }
    var showExerciseHint by remember(lesson.id) { mutableStateOf(false) }
    var isRunningExercise by remember { mutableStateOf(false) }

    // Quiz state
    var selectedQuizOption by remember(lesson.id) { mutableIntStateOf(-1) }
    var quizSubmitted by remember(lesson.id) { mutableStateOf(false) }
    val quizPassed = quizSubmitted && selectedQuizOption == lesson.quizCorrectIndex

    // Contextual Falcon AI Lesson Tutor state
    var tutorQuestion by remember { mutableStateOf("") }
    var tutorResponse by remember(lesson.id) { mutableStateOf<AiCodeAssistantResponse?>(null) }
    var isAskingTutor by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("coding_lesson_detail_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header with Back Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("lesson_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Curriculum"
                        )
                    }
                    Column {
                        Text(
                            text = lesson.moduleTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicViolet,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = lesson.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = EmeraldSuccess.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, EmeraldSuccess)
                    ) {
                        Text(
                            text = "Completed ✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 1. Concept Explanation & "I Don't Understand — Explain Simpler" Toggle
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Concept Explanation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicViolet
                    )
                    Text(
                        text = lesson.conceptExplanation,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 23.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Why This Exists & When It Is Useful:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lesson.whyItExists,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showSimplerAnalogy = !showSimplerAnalogy },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("explain_simpler_button")
                    ) {
                        Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = SolarFlareAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (showSimplerAnalogy) "Hide Simpler Analogy"
                            else "I Don't Understand — Explain Differently with a Simpler Example"
                        )
                    }

                    AnimatedVisibility(visible = showSimplerAnalogy) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SolarFlareAmber.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, SolarFlareAmber),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Simpler Everyday Analogy:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SolarFlareAmber,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lesson.simplerExplanation,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Syntax Guide, Working Example & Line-by-Line Walkthrough
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Syntax & Working Example",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = { onOpenInPlayground(lesson.workingExampleCode, lesson.exerciseRequiredInput) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open in Falcon Code", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lesson.workingExampleCode,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Text(
                        text = "Expected Output:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF111827),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lesson.expectedOutput,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = EmeraldSuccess,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Text(
                        text = "Line-by-Line Walkthrough:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    lesson.lineByLineWalkthrough.forEach { step ->
                        Text(
                            text = "• $step",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 3. Common Beginner Mistakes & Real-World Application
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = SupernovaCoral)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Common Beginner Mistakes & How to Fix Them",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    lesson.commonMistakes.forEach { mistake ->
                        Text(
                            text = "• $mistake",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Real-World Example: ${lesson.realWorldExample}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. Interactive Coding Exercise
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, if (exercisePassed) EmeraldSuccess else CosmicViolet.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Interactive Practice Exercise",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = lesson.exercisePrompt,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = exerciseCode,
                        onValueChange = { exerciseCode = it },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lesson_exercise_editor")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isRunningExercise = true
                                scope.launch {
                                    val res = executionEngine.executeCode(
                                        language = lesson.languageId,
                                        code = exerciseCode,
                                        stdin = lesson.exerciseRequiredInput
                                    )
                                    exerciseResult = res
                                    isRunningExercise = false
                                    val passed = res.isSuccess && lesson.exerciseExpectedOutputContains.all { expected ->
                                        res.stdout.contains(expected, ignoreCase = true)
                                    }
                                    exercisePassed = passed
                                    if (!passed) {
                                        onRecordWeakTopic(lesson.topicTag)
                                    }
                                }
                            },
                            enabled = !isRunningExercise,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color(0xFF052E16)),
                            modifier = Modifier.testTag("run_lesson_exercise_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run & Check Exercise", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(onClick = { showExerciseHint = !showExerciseHint }) {
                            Text(if (showExerciseHint) "Hide Hint" else "Need a Hint?")
                        }
                    }

                    if (showExerciseHint) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SolarFlareAmber.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Hint: ${lesson.exerciseHint}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    val exRes = exerciseResult
                    if (exRes != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (exercisePassed) EmeraldSuccess.copy(alpha = 0.14f) else SupernovaCoral.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, if (exercisePassed) EmeraldSuccess else SupernovaCoral),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (exercisePassed) "✓ Exercise Passed! Output verified." else "Not quite right yet — check output/errors below and try again:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (exercisePassed) EmeraldSuccess else SupernovaCoral
                                )
                                if (exRes.stdout.isNotBlank()) {
                                    Text(
                                        text = "Output:\n${exRes.stdout}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                }
                                if (exRes.stderr.isNotBlank()) {
                                    Text(
                                        text = "Error:\n${exRes.stderr}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = SupernovaCoral
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Short Knowledge-Check Quiz (with Wrong-Answer Explanation, Hint & Retry)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Knowledge-Check Quiz",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = lesson.quizQuestion,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    lesson.quizOptions.forEachIndexed { idx, opt ->
                        val isChosen = selectedQuizOption == idx
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isChosen) CosmicViolet.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isChosen) CosmicViolet else Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedQuizOption = idx
                                    quizSubmitted = true
                                    if (idx != lesson.quizCorrectIndex) {
                                        onRecordWeakTopic(lesson.topicTag)
                                    }
                                }
                                .testTag("lesson_quiz_option_$idx")
                        ) {
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (quizSubmitted) {
                        if (quizPassed) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldSuccess.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, EmeraldSuccess),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "✓ Correct! ${lesson.quizWrongExplanation}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SolarFlareAmber.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, SolarFlareAmber),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Why that wasn't right: ${lesson.quizWrongExplanation}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Hint to try again: ${lesson.quizHint}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SolarFlareAmber
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Ask Falcon AI About This Lesson
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Ask Falcon AI About ${lesson.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tutorQuestion,
                            onValueChange = { tutorQuestion = it },
                            placeholder = { Text("Ask a question about this lesson...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (tutorQuestion.isNotBlank() && !isAskingTutor) {
                                    val q = tutorQuestion
                                    tutorQuestion = ""
                                    isAskingTutor = true
                                    scope.launch {
                                        tutorResponse = codeAiService.assistWithCode(
                                            action = AiCodeAction.EXPLAIN_CONCEPT,
                                            language = lesson.languageId,
                                            code = exerciseCode,
                                            lastResult = exerciseResult,
                                            lessonTitle = lesson.title,
                                            customQuestion = q
                                        )
                                        isAskingTutor = false
                                    }
                                }
                            }
                        ) {
                            Text("Ask")
                        }
                    }
                    if (tutorResponse != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CosmicViolet.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = tutorResponse!!.explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // 7. Complete Lesson & Continue Button
        item {
            Button(
                onClick = { onCompleteLesson(lesson) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CosmicViolet),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("complete_lesson_button")
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mark Lesson Complete & Continue (+${lesson.coinsReward} 🪙)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PracticeChallengeCard(
    challenge: PracticeChallenge,
    isCompleted: Boolean,
    executionEngine: CodeExecutionEngine,
    onCompleted: () -> Unit,
    onFailedAttempt: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var codeAttempt by remember(challenge.id) { mutableStateOf(challenge.starterCode) }
    var selectedMcqIndex by remember(challenge.id) { mutableIntStateOf(-1) }
    var testResults by remember(challenge.id) { mutableStateOf<List<TestCaseRunResult>>(emptyList()) }
    var showHint by remember(challenge.id) { mutableStateOf(false) }
    var feedbackMessage by remember(challenge.id) { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("practice_challenge_${challenge.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CosmicViolet.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "${challenge.type.displayTitle} • ${challenge.difficulty}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicViolet,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (isCompleted) {
                    Text(
                        text = "Solved ✓",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = challenge.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = challenge.prompt,
                style = MaterialTheme.typography.bodyMedium
            )

            if (challenge.options.isNotEmpty()) {
                // Predict the Output or Multiple Choice
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = challenge.starterCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(10.dp)
                    )
                }
                challenge.options.forEachIndexed { idx, option ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedMcqIndex == idx) CosmicViolet.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMcqIndex = idx
                                if (idx == challenge.correctOptionIndex) {
                                    feedbackMessage = "✓ Correct! ${challenge.explanation}"
                                    onCompleted()
                                } else {
                                    feedbackMessage = "Not quite — ${challenge.hint}"
                                    onFailedAttempt()
                                }
                            }
                    ) {
                        Text(
                            text = option,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            } else {
                // Code Challenge with Multi-Test-Case Verification
                OutlinedTextField(
                    value = codeAttempt,
                    onValueChange = { codeAttempt = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                val results = challenge.testCases.map { tc ->
                                    val exec = executionEngine.executeCode(challenge.languageId, codeAttempt, tc.input)
                                    val pass = exec.isSuccess && exec.stdout.trim() == tc.expectedOutput.trim()
                                    TestCaseRunResult(
                                        testCase = tc,
                                        passed = pass,
                                        actualOutput = exec.stdout.ifBlank { exec.stderr },
                                        errorDetail = exec.stderr
                                    )
                                }
                                testResults = results
                                if (results.isNotEmpty() && results.all { it.passed }) {
                                    feedbackMessage = "✓ All ${results.size} test cases passed (including edge cases)!"
                                    onCompleted()
                                } else {
                                    feedbackMessage = "Some test cases failed. Check the feedback below or view a hint."
                                    onFailedAttempt()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color(0xFF052E16))
                    ) {
                        Text("Run Test Suite (${challenge.testCases.size} Tests)", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(onClick = { showHint = !showHint }) {
                        Text(if (showHint) "Hide Hint" else "Hint")
                    }
                }

                if (testResults.isNotEmpty()) {
                    testResults.forEachIndexed { idx, res ->
                        val label = if (res.testCase.isHidden) {
                            "Test Case #${idx + 1}: Hidden Edge Case"
                        } else {
                            "Test Case #${idx + 1}: ${res.testCase.description}"
                        }
                        Text(
                            text = if (res.passed) "✓ $label — PASSED" else "✗ $label — FAILED",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (res.passed) EmeraldSuccess else SupernovaCoral,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (showHint) {
                Text(
                    text = "Hint: ${challenge.hint}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SolarFlareAmber
                )
            }
            if (!feedbackMessage.isNullOrBlank()) {
                Text(
                    text = feedbackMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CodingProjectCard(
    project: CodingProject,
    isCompleted: Boolean,
    onOpenInPlayground: () -> Unit,
    onVerifyAndComplete: (String) -> Unit
) {
    var projectCode by remember(project.id) { mutableStateOf(project.starterCode) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isCompleted) EmeraldSuccess else CosmicViolet.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${project.stage} • ${project.difficulty}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TurquoiseNebula,
                    fontWeight = FontWeight.Bold
                )
                if (isCompleted) {
                    Text("Completed ✓", color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = project.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = project.description,
                style = MaterialTheme.typography.bodyMedium
            )
            project.objectives.forEach { obj ->
                Text(text = "• $obj", style = MaterialTheme.typography.bodySmall)
            }

            OutlinedTextField(
                value = projectCode,
                onValueChange = { projectCode = it },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onVerifyAndComplete(projectCode) },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicViolet)
                ) {
                    Text("Run & Verify Project (+${project.coinsReward} 🪙)")
                }
                OutlinedButton(onClick = onOpenInPlayground) {
                    Text("Open in Falcon Code")
                }
            }
        }
    }
}

@Composable
private fun ProgressAndRoadmapSection(
    selectedLanguage: ProgrammingLanguageId,
    progress: com.example.data.coding.UserCodingProgress,
    totalLessons: Int,
    completedLessonsInLang: Int,
    roadmapStages: List<com.example.data.coding.RoadmapStage>,
    onUpdateStudyGoal: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Progress Summary Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${selectedLanguage.displayName} Progress & Adaptive Revision",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Lessons Completed: $completedLessonsInLang / $totalLessons • Challenges Solved: ${progress.completedChallengeIds.size} • Projects Built: ${progress.completedProjectIds.size} • Code Runs: ${progress.totalExecutions}",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (progress.masteredTopics.isNotEmpty()) {
                    Text(
                        text = "Mastered Skills: ${progress.masteredTopics.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (progress.weakTopics.isNotEmpty()) {
                    Text(
                        text = "Recommended Topics for Revision: ${progress.weakTopics.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SolarFlareAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Adaptive Revision Status: No weak topics flagged right now!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                Text(
                    text = "Adjustable Daily Study Schedule (Current Goal: ${progress.dailyStudyMinutesGoal} mins/day)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Progress is measured by demonstrated skills, completed lessons, and verified coding exercises rather than time alone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        FilterChip(
                            selected = progress.dailyStudyMinutesGoal == mins,
                            onClick = { onUpdateStudyGoal(mins) },
                            label = { Text("${mins}m/day") }
                        )
                    }
                }
            }
        }

        // 90-Day Three-Month Roadmap Stages
        roadmapStages.forEach { stage ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stage.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicViolet
                    )
                    Text(
                        text = stage.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = TurquoiseNebula
                    )
                    stage.coreTopics.forEach { topic ->
                        Text(text = "• $topic", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Milestones: ${stage.milestones.joinToString(" • ")}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldSuccess
                    )
                }
            }
        }
    }
}
