package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.HomeworkSession
import com.example.data.model.LuminoCatalog
import com.example.data.model.QuizAttempt
import com.example.data.model.UserProfile
import com.example.data.remote.GeminiTutorService
import com.example.data.repository.LuminoFirestoreRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutFromLumino
import com.example.ui.screens.CodingPlatformScreen
import com.example.ui.screens.DailyCheckInScreen
import com.example.ui.screens.DailyQuizScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GradeAndLanguageDialog
import com.example.ui.screens.HomeworkTutorScreen
import com.example.ui.screens.ProgressAndRemindersScreen
import com.example.ui.screens.StreakRecoveryVaultDialog
import com.example.ui.theme.LuminoAITheme
import com.example.ui.viewmodel.LuminoViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

enum class LuminoTab(val label: String, val icon: ImageVector, val testTag: String) {
    DASHBOARD("Home", Icons.Default.Home, "nav_tab_dashboard"),
    HOMEWORK("Homework", Icons.Default.AutoAwesome, "nav_tab_homework"),
    CODING("Coding", Icons.Default.Code, "nav_tab_coding"),
    QUIZ("Daily Quiz", Icons.Default.Quiz, "nav_tab_quiz"),
    PROGRESS("Settings", Icons.Default.Settings, "nav_tab_progress")
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var isGuestMode by rememberSaveable { mutableStateOf(false) }
    var gmailOnlySession by rememberSaveable { mutableStateOf<String?>(null) }

    val user = currentUser
    when {
        user != null -> {
            LuminoAuthenticatedApp(
                currentUserId = user.uid,
                displayName = user.displayName ?: "Falcon Scholar",
                sessionSubtitle = "${user.email ?: "Connected Gmail"} • Device Gmail",
                isLocalOrGuestSession = false,
                onExitLocalOrGuestSession = {
                    isGuestMode = false
                    gmailOnlySession = null
                }
            )
        }
        isGuestMode -> {
            LuminoAuthenticatedApp(
                currentUserId = "guest_falcon_explorer",
                displayName = "Guest Falcon Scholar",
                sessionSubtitle = "Guest Mode Active • No Verification Needed",
                isLocalOrGuestSession = true,
                onExitLocalOrGuestSession = {
                    isGuestMode = false
                }
            )
        }
        gmailOnlySession != null -> {
            val email = gmailOnlySession!!
            val namePart = email.substringBefore("@").replaceFirstChar { it.uppercase() }
            LuminoAuthenticatedApp(
                currentUserId = "gmail_${email.replace("[^a-zA-Z0-9_]".toRegex(), "_")}",
                displayName = "$namePart (Falcon Scholar)",
                sessionSubtitle = "$email • Gmail Only (No Phone/Password)",
                isLocalOrGuestSession = true,
                onExitLocalOrGuestSession = {
                    gmailOnlySession = null
                }
            )
        }
        else -> {
            LuminoAITheme {
                AuthScreen(
                    onAuthSuccess = {},
                    onEnterGuestMode = { isGuestMode = true },
                    onContinueWithGmailOnly = { enteredGmail ->
                        gmailOnlySession = enteredGmail
                    }
                )
            }
        }
    }
}

@Composable
fun LuminoAuthenticatedApp(
    currentUserId: String,
    displayName: String,
    sessionSubtitle: String = "Guest Mode Active",
    isLocalOrGuestSession: Boolean = false,
    onExitLocalOrGuestSession: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: LuminoViewModel = viewModel(
        key = "$currentUserId-$isLocalOrGuestSession",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                LuminoViewModel(
                    repository = LuminoFirestoreRepository(db),
                    geminiService = GeminiTutorService(),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    isLocalOrGuestSession = isLocalOrGuestSession
                )
            }
        }
    )

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val homeworkListState by viewModel.homeworkListState.collectAsStateWithLifecycle()
    val quizHistoryState by viewModel.quizHistoryState.collectAsStateWithLifecycle()
    val studioState by viewModel.homeworkStudio.collectAsStateWithLifecycle()
    val quizArenaState by viewModel.quizArena.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val darkModeOverride by viewModel.darkModeOverride.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success<UserProfile>)?.data ?: UserProfile(
        userId = currentUserId,
        displayName = displayName
    )
    val systemDark = isSystemInDarkTheme()
    val isDarkMode = darkModeOverride ?: ((profileState as? UiState.Success<UserProfile>)?.data?.darkModeEnabled ?: systemDark)

    val homeworkSessions = (homeworkListState as? UiState.Success<List<HomeworkSession>>)?.data ?: emptyList()
    val quizAttempts = (quizHistoryState as? UiState.Success<List<QuizAttempt>>)?.data ?: emptyList()

    var currentTab by rememberSaveable { mutableStateOf(LuminoTab.DASHBOARD) }
    var showGradeLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showStreakVaultDialog by rememberSaveable { mutableStateOf(false) }
    var showDailyCheckInScreen by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    if (showDailyCheckInScreen) {
        BackHandler {
            showDailyCheckInScreen = false
        }
    } else if (currentTab != LuminoTab.DASHBOARD) {
        BackHandler {
            currentTab = LuminoTab.DASHBOARD
        }
    }

    val activeLanguageMeta = LuminoCatalog.languages.find { it.name == profile.selectedLanguage }
    val layoutDirection = if (activeLanguageMeta?.isRtl == true) LayoutDirection.Rtl else LayoutDirection.Ltr

    val handleSignOut: () -> Unit = {
        onExitLocalOrGuestSession()
        if (!isLocalOrGuestSession) {
            signOutFromLumino(
                credentialManager = credentialManager,
                onSignOutComplete = {},
                scope = scope
            )
        }
    }

    LuminoAITheme(darkTheme = isDarkMode) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isExpandedScreen = maxWidth >= 600.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (!isExpandedScreen) {
                            NavigationBar(
                                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                LuminoTab.entries.forEach { tab ->
                                    NavigationBarItem(
                                        selected = currentTab == tab,
                                        onClick = { currentTab = tab },
                                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                                        label = { Text(tab.label) },
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isExpandedScreen) {
                            NavigationRail {
                                LuminoTab.entries.forEach { tab ->
                                    NavigationRailItem(
                                        selected = currentTab == tab,
                                        onClick = { currentTab = tab },
                                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                                        label = { Text(tab.label) },
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            when (currentTab) {
                                LuminoTab.DASHBOARD -> DashboardScreen(
                                    profile = profile,
                                    homeworkSessions = homeworkSessions,
                                    onOpenGradeLanguageDialog = { showGradeLanguageDialog = true },
                                    onOpenStreakVault = { showStreakVaultDialog = true },
                                    onClaimDailyStreak = viewModel::claimDailyStreakReward,
                                    onOpenDailyCheckIn = { showDailyCheckInScreen = true },
                                    onNavigateToHomework = { prefillPrompt ->
                                        if (!prefillPrompt.isNullOrBlank()) {
                                            viewModel.onHomeworkQuestionChanged(prefillPrompt)
                                        }
                                        currentTab = LuminoTab.HOMEWORK
                                    },
                                    onSendDirectHomeworkPrompt = { prompt ->
                                        viewModel.onHomeworkQuestionChanged(prompt)
                                        currentTab = LuminoTab.HOMEWORK
                                        viewModel.askAiHomeworkTutor()
                                    },
                                    onNavigateToCoding = {
                                        currentTab = LuminoTab.CODING
                                    },
                                    onNavigateToQuiz = { subjectTag ->
                                        if (!subjectTag.isNullOrBlank()) {
                                            viewModel.selectQuizSubject(subjectTag)
                                        }
                                        currentTab = LuminoTab.QUIZ
                                    },
                                    onNavigateToProgress = { currentTab = LuminoTab.PROGRESS },
                                    onSelectHomeworkSession = { session ->
                                        viewModel.selectExistingHomeworkSession(session)
                                        currentTab = LuminoTab.HOMEWORK
                                    },
                                    onSignOut = handleSignOut
                                )

                                LuminoTab.HOMEWORK -> HomeworkTutorScreen(
                                    profile = profile,
                                    studioState = studioState,
                                    homeworkHistory = homeworkSessions,
                                    onQuestionChanged = viewModel::onHomeworkQuestionChanged,
                                    onBitmapSelected = viewModel::onHomeworkBitmapSelected,
                                    onAskTutor = { spokenViaMic ->
                                        viewModel.askAiHomeworkTutor(spokenViaMic = spokenViaMic)
                                    },
                                    onToggleAutoSpeakTts = viewModel::toggleAutoSpeakTts,
                                    onMarkSessionSpoken = viewModel::markSessionSpoken,
                                    onOpenGradeLanguageDialog = { showGradeLanguageDialog = true }
                                )

                                LuminoTab.CODING -> CodingPlatformScreen(
                                    profile = profile,
                                    onAwardCodingReward = { coins, xp, msg ->
                                        viewModel.awardCodingReward(coins, xp, msg)
                                    },
                                    onShowMessage = { msg ->
                                        viewModel.showCustomToast(msg)
                                    }
                                )

                                LuminoTab.QUIZ -> DailyQuizScreen(
                                    profile = profile,
                                    quizState = quizArenaState,
                                    onSelectGrade = viewModel::selectQuizGrade,
                                    onSelectSubject = viewModel::selectQuizSubject,
                                    onSelectDifficulty = viewModel::selectQuizDifficulty,
                                    onSelectQuestionCount = viewModel::selectQuizQuestionCount,
                                    onStartQuiz = { viewModel.startDailyQuiz() },
                                    onSelectOption = viewModel::selectQuizOption,
                                    onToggleHint = viewModel::toggleQuizHint,
                                    onSubmitAnswer = viewModel::submitCurrentQuizAnswer,
                                    onNextQuestion = viewModel::nextQuizQuestionOrFinish,
                                    onOpenGradeLanguageDialog = { showGradeLanguageDialog = true }
                                )

                                LuminoTab.PROGRESS -> ProgressAndRemindersScreen(
                                    profile = profile,
                                    quizAttempts = quizAttempts,
                                    isDarkMode = isDarkMode,
                                    sessionSubtitle = sessionSubtitle,
                                    onToggleDarkMode = viewModel::setDarkMode,
                                    onOpenGradeLanguageDialog = { showGradeLanguageDialog = true },
                                    onUpdateReminder = viewModel::updateStudyReminder,
                                    onOpenStreakVault = { showStreakVaultDialog = true },
                                    onSignOut = handleSignOut,
                                    onShowNotificationStatus = { status ->
                                        scope.launch { snackbarHostState.showSnackbar(status) }
                                    },
                                    onRecoverStudyStreak = { viewModel.recoverStreakWithCoins(recoverStudy = true) },
                                    onRecoverQuizStreak = { viewModel.recoverStreakWithCoins(recoverStudy = false) },
                                    onBuyShield = { viewModel.buyStreakShield() },
                                    onSimulateMissedDay = { viewModel.simulateMissedDayStreak() }
                                )
                            }
                        }
                    }
                }
            }

            if (showGradeLanguageDialog) {
                GradeAndLanguageDialog(
                    profile = profile,
                    onDismiss = { showGradeLanguageDialog = false },
                    onSave = { grade, lang, tags ->
                        viewModel.updateGradeAndLanguage(grade, lang, tags)
                    }
                )
            }

            if (showStreakVaultDialog) {
                StreakRecoveryVaultDialog(
                    profile = profile,
                    onDismiss = { showStreakVaultDialog = false },
                    onRecoverStudyStreak = { viewModel.recoverStreakWithCoins(recoverStudy = true) },
                    onRecoverQuizStreak = { viewModel.recoverStreakWithCoins(recoverStudy = false) },
                    onBuyShield = { viewModel.buyStreakShield() },
                    onSimulateMissedDay = { viewModel.simulateMissedDayStreak() }
                )
            }

            if (showDailyCheckInScreen) {
                DailyCheckInScreen(
                    profile = profile,
                    onBack = { showDailyCheckInScreen = false },
                    onClaimCoins = { viewModel.claimDailyStreakReward() },
                    onOpenStreakVault = { showStreakVaultDialog = true },
                    onNavigateToQuiz = {
                        currentTab = LuminoTab.QUIZ
                        showDailyCheckInScreen = false
                    },
                    onNavigateToHomework = {
                        currentTab = LuminoTab.HOMEWORK
                        showDailyCheckInScreen = false
                    }
                )
            }
        }
    }
}
