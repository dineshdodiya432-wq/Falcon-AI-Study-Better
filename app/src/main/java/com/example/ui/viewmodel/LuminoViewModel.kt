package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.HomeworkSession
import com.example.data.model.LuminoCatalog
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestionItem
import com.example.data.model.UserProfile
import com.example.data.remote.GeminiTutorService
import com.example.data.repository.LuminoFirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class HomeworkStudioState(
    val questionInput: String = "",
    val selectedBitmap: Bitmap? = null,
    val isGenerating: Boolean = false,
    val activeSession: HomeworkSession? = null,
    val revealedStepCount: Int = 1,
    val autoSpeakTts: Boolean = true,
    val lastSpokenSessionId: String? = null,
    val statusBanner: String? = null
)

data class DailyQuizArenaState(
    val selectedSubjectTag: String = "Mathematics",
    val selectedDifficulty: String = "Medium",
    val selectedQuestionCount: Int = 5,
    val activeGrade: Int = 6,
    val activeLanguage: String = "Simple English",
    val isGeneratingQuiz: Boolean = false,
    val questions: List<QuizQuestionItem> = LuminoCatalog.getCurriculumQuizForGrade(
        gradeLevel = 6,
        subjectTag = "Mathematics",
        language = "Simple English",
        difficulty = "Medium",
        questionCount = 5
    ),
    val currentQuestionIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isAnswerSubmitted: Boolean = false,
    val isHintVisible: Boolean = false,
    val correctCount: Int = 0,
    val isQuizFinished: Boolean = false,
    val coinsRewarded: Int = 0
)

class LuminoViewModel(
    private val repository: LuminoFirestoreRepository,
    private val geminiService: GeminiTutorService,
    private val currentUserId: String,
    initialDisplayName: String = "Falcon Scholar",
    private val isLocalOrGuestSession: Boolean = false
) : ViewModel() {

    private val isCloudSyncActive: Boolean
        get() = !isLocalOrGuestSession && FirebaseAuth.getInstance().currentUser != null

    private val _darkModeOverride = MutableStateFlow<Boolean?>(null)
    val darkModeOverride: StateFlow<Boolean?> = _darkModeOverride.asStateFlow()

    private val _localProfile = MutableStateFlow(
        UserProfile(
            id = currentUserId,
            userId = currentUserId,
            displayName = initialDisplayName
        )
    )

    private val _localHomeworkList = MutableStateFlow(
        listOf(
            HomeworkSession(
                id = "welcome_hw_1",
                userId = currentUserId,
                questionTitle = "How do plants use sunlight to make their own food?",
                subjectTag = "Biology",
                gradeLevel = 6,
                language = "Simple English",
                steps = listOf(
                    "Step 1 (Sunlight Capture): Green leaves contain tiny solar panels called chloroplasts filled with chlorophyll that absorb sunlight.",
                    "Step 2 (Water & Air Intake): Roots drink water (H2O) from the soil while tiny leaf pores called stomata breathe in carbon dioxide (CO2) from the air.",
                    "Step 3 (Making Plant Sugar): Inside the leaf, sunlight energy combines water and carbon dioxide to build glucose (plant food) and releases fresh oxygen (O2) into the air!"
                ),
                finalAnswer = "Plants make food through Photosynthesis: Sunlight + Water + Carbon Dioxide -> Glucose (Sugar) + Oxygen.",
                keyConcept = "Photosynthesis • Chlorophyll (green pigment) • Stomata (leaf pores)",
                coinsEarned = 20
            )
        )
    )

    private val _localQuizHistory = MutableStateFlow<List<QuizAttempt>>(emptyList())

    val userProfileState: StateFlow<UiState<UserProfile>> = (
        if (isCloudSyncActive) {
            repository.observeUserProfile(currentUserId)
                .map<UserProfile?, UiState<UserProfile>> { profile ->
                    if (profile != null) {
                        _localProfile.value = profile
                        syncQuizGradeAndLanguageIfNeeded(profile.selectedGrade, profile.selectedLanguage)
                        UiState.Success(profile)
                    } else {
                        UiState.Success(_localProfile.value)
                    }
                }
                .catch { error ->
                    Log.w(TAG, "Error observing user profile, falling back to local state", error)
                    emit(UiState.Success(_localProfile.value))
                }
        } else {
            _localProfile.map { profile ->
                syncQuizGradeAndLanguageIfNeeded(profile.selectedGrade, profile.selectedLanguage)
                UiState.Success(profile)
            }
        }
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Success(_localProfile.value)
    )

    val homeworkListState: StateFlow<UiState<List<HomeworkSession>>> = (
        if (isCloudSyncActive) {
            repository.observeHomeworkSessions(currentUserId)
                .map<List<HomeworkSession>, UiState<List<HomeworkSession>>> { list ->
                    _localHomeworkList.value = list
                    UiState.Success(list)
                }
                .catch { error ->
                    Log.w(TAG, "Error observing homework sessions, falling back to local state", error)
                    emit(UiState.Success(_localHomeworkList.value))
                }
        } else {
            _localHomeworkList.map { UiState.Success(it) }
        }
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Success(_localHomeworkList.value)
    )

    val quizHistoryState: StateFlow<UiState<List<QuizAttempt>>> = (
        if (isCloudSyncActive) {
            repository.observeQuizAttempts(currentUserId)
                .map<List<QuizAttempt>, UiState<List<QuizAttempt>>> { list ->
                    _localQuizHistory.value = list
                    UiState.Success(list)
                }
                .catch { error ->
                    Log.w(TAG, "Error observing quiz attempts, falling back to local state", error)
                    emit(UiState.Success(_localQuizHistory.value))
                }
        } else {
            _localQuizHistory.map { UiState.Success(it) }
        }
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Success(_localQuizHistory.value)
    )

    private val _homeworkStudio = MutableStateFlow(HomeworkStudioState())
    val homeworkStudio: StateFlow<HomeworkStudioState> = _homeworkStudio.asStateFlow()

    private val _quizArena = MutableStateFlow(DailyQuizArenaState())
    val quizArena: StateFlow<DailyQuizArenaState> = _quizArena.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        if (isCloudSyncActive) {
            viewModelScope.launch {
                repository.ensureUserProfileInitialized(initialDisplayName)
            }
        }
    }

    private fun currentProfileOrDefault(): UserProfile {
        return (userProfileState.value as? UiState.Success)?.data ?: _localProfile.value
    }

    private fun syncQuizGradeAndLanguageIfNeeded(grade: Int, language: String) {
        val current = _quizArena.value
        if (!current.isGeneratingQuiz &&
            current.currentQuestionIndex == 0 &&
            !current.isAnswerSubmitted &&
            (current.activeGrade != grade || current.activeLanguage != language)
        ) {
            _quizArena.value = current.copy(
                activeGrade = grade,
                activeLanguage = language,
                questions = LuminoCatalog.getCurriculumQuizForGrade(
                    gradeLevel = grade,
                    subjectTag = current.selectedSubjectTag,
                    language = language,
                    difficulty = current.selectedDifficulty,
                    questionCount = current.selectedQuestionCount
                ),
                selectedOptionIndex = null,
                isHintVisible = false
            )
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun setDarkMode(enabled: Boolean) {
        _darkModeOverride.value = enabled
        _localProfile.value = _localProfile.value.copy(darkModeEnabled = enabled)
        _toastMessage.value = if (enabled) "Dark Mode enabled" else "Light Mode enabled"
        if (isCloudSyncActive) {
            viewModelScope.launch {
                repository.updateDarkMode(enabled)
            }
        }
    }

    fun toggleAutoSpeakTts() {
        val current = _homeworkStudio.value
        val next = !current.autoSpeakTts
        _homeworkStudio.value = current.copy(autoSpeakTts = next)
        _toastMessage.value = if (next) "Voice TTS Read-Aloud On" else "Voice TTS Read-Aloud Muted"
    }

    fun markSessionSpoken(sessionId: String) {
        _homeworkStudio.value = _homeworkStudio.value.copy(lastSpokenSessionId = sessionId)
    }

    fun updateGradeAndLanguage(grade: Int, language: String, curriculumTags: List<String>) {
        val profile = currentProfileOrDefault()
        val safeGrade = grade.coerceIn(1, 12)
        val safeLang = language.trim().ifEmpty { "Simple English" }
        val updatedBadges = profile.unlockedBadges.toMutableList()
        var bonusCoins = 0
        if (safeLang != "Simple English" && !updatedBadges.contains("polyglot_scholar")) {
            updatedBadges.add("polyglot_scholar")
            bonusCoins = 45
        }

        _localProfile.value = profile.copy(
            selectedGrade = safeGrade,
            selectedLanguage = safeLang,
            curriculumTags = curriculumTags,
            unlockedBadges = updatedBadges,
            coins = profile.coins + bonusCoins
        )

        val currentQuiz = _quizArena.value
        _quizArena.value = currentQuiz.copy(
            activeGrade = safeGrade,
            activeLanguage = safeLang,
            questions = LuminoCatalog.getCurriculumQuizForGrade(
                gradeLevel = safeGrade,
                subjectTag = currentQuiz.selectedSubjectTag,
                language = safeLang,
                difficulty = currentQuiz.selectedDifficulty,
                questionCount = currentQuiz.selectedQuestionCount
            ),
            currentQuestionIndex = 0,
            selectedOptionIndex = null,
            isAnswerSubmitted = false,
            isHintVisible = false,
            correctCount = 0,
            isQuizFinished = false
        )
        _toastMessage.value = "Updated to Grade $safeGrade • $safeLang"

        if (isCloudSyncActive) {
            viewModelScope.launch {
                repository.updateGradeAndLanguage(
                    grade = safeGrade,
                    language = safeLang,
                    curriculumTags = curriculumTags,
                    currentUnlockedBadges = profile.unlockedBadges,
                    currentCoins = profile.coins
                )
            }
        }
    }

    fun selectQuizGrade(grade: Int) {
        val safeGrade = grade.coerceIn(1, 12)
        val profile = currentProfileOrDefault()
        updateGradeAndLanguage(
            grade = safeGrade,
            language = profile.selectedLanguage,
            curriculumTags = profile.curriculumTags
        )
    }

    fun onHomeworkQuestionChanged(text: String) {
        _homeworkStudio.value = _homeworkStudio.value.copy(questionInput = text)
    }

    fun onHomeworkBitmapSelected(bitmap: Bitmap?) {
        _homeworkStudio.value = _homeworkStudio.value.copy(selectedBitmap = bitmap)
    }

    fun selectExistingHomeworkSession(session: HomeworkSession) {
        _homeworkStudio.value = _homeworkStudio.value.copy(
            activeSession = session,
            revealedStepCount = session.steps.size.coerceAtLeast(1),
            statusBanner = null
        )
    }

    fun revealNextHomeworkStep() {
        val current = _homeworkStudio.value
        val maxSteps = current.activeSession?.steps?.size ?: 1
        if (current.revealedStepCount < maxSteps) {
            _homeworkStudio.value = current.copy(revealedStepCount = current.revealedStepCount + 1)
        }
    }

    fun revealAllHomeworkSteps() {
        val current = _homeworkStudio.value
        val maxSteps = current.activeSession?.steps?.size ?: 1
        _homeworkStudio.value = current.copy(revealedStepCount = maxSteps)
    }

    fun askAiHomeworkTutor(spokenViaMic: Boolean = false) {
        val state = _homeworkStudio.value
        if (state.questionInput.isBlank() && state.selectedBitmap == null) {
            _toastMessage.value = "Type a message, tap the Mic to talk, or snap a photo with the Camera!"
            return
        }
        val profile = currentProfileOrDefault()
        _homeworkStudio.value = state.copy(
            isGenerating = true,
            autoSpeakTts = false,
            statusBanner = null
        )

        viewModelScope.launch {
            val structured = geminiService.explainHomeworkStepByStep(
                questionText = state.questionInput,
                homeworkBitmap = state.selectedBitmap,
                gradeLevel = profile.selectedGrade,
                language = profile.selectedLanguage
            )

            val isProfane = LuminoCatalog.isProfaneOrAngry(state.questionInput)
            val autoDetectedSubject = if (isProfane) "Falcon AI Care" else structured.detectedSubject.ifBlank {
                LuminoCatalog.inferSubjectFromText(state.questionInput)
            }
            val paragraph = if (isProfane && (structured.explanationParagraph.isBlank() || structured.explanationParagraph.length < 25)) {
                LuminoCatalog.getBadWordsApologyExplanation(profile.selectedLanguage)
            } else {
                structured.explanationParagraph.ifBlank { structured.finalAnswer }
            }
            val title = if (isProfane) "Hey, what happened?" else structured.questionTitle

            var savedId = "local_hw_${UUID.randomUUID()}"
            if (isCloudSyncActive) {
                val saveResult = repository.saveHomeworkSession(
                    questionTitle = title,
                    subjectTag = autoDetectedSubject,
                    gradeLevel = profile.selectedGrade,
                    language = profile.selectedLanguage,
                    steps = listOf(paragraph),
                    finalAnswer = paragraph,
                    keyConcept = if (isProfane) "Falcon AI Empathy" else structured.keyConcept,
                    practiceQuestion = "",
                    practiceOptions = emptyList(),
                    practiceCorrectIndex = 0,
                    coinsEarned = 20
                )
                repository.recordStudyReward(
                    coinsDelta = 20,
                    xpDelta = 25,
                    incrementStudyStreak = true,
                    currentProfile = profile
                )
                savedId = saveResult.getOrNull() ?: savedId
            }

            val newBadges = profile.unlockedBadges.toMutableList()
            if (!newBadges.contains("curious_spark")) newBadges.add("curious_spark")
            if (!newBadges.contains("homework_hero")) newBadges.add("homework_hero")
            val nextStudyStreak = profile.studyStreak + 1
            if (nextStudyStreak >= 7 && !newBadges.contains("seven_day_orbit")) newBadges.add("seven_day_orbit")

            _localProfile.value = profile.copy(
                coins = profile.coins + 20,
                xp = profile.xp + 25,
                studyStreak = nextStudyStreak,
                unlockedBadges = newBadges
            )

            val newSession = HomeworkSession(
                id = savedId,
                userId = currentUserId,
                questionTitle = title,
                subjectTag = autoDetectedSubject,
                gradeLevel = profile.selectedGrade,
                language = profile.selectedLanguage,
                steps = listOf(paragraph),
                finalAnswer = paragraph,
                keyConcept = if (isProfane) "Falcon AI Empathy" else structured.keyConcept,
                practiceQuestion = "",
                practiceOptions = emptyList(),
                practiceCorrectIndex = 0,
                practiceCompleted = true,
                coinsEarned = 20,
                explanationParagraph = paragraph
            )
            _localHomeworkList.value = listOf(newSession) + _localHomeworkList.value

            val streakMotivation = LuminoCatalog.getStreakMotivationMessage(nextStudyStreak)
            _homeworkStudio.value = _homeworkStudio.value.copy(
                questionInput = "",
                selectedBitmap = null,
                isGenerating = false,
                activeSession = newSession,
                revealedStepCount = 1,
                statusBanner = if (isProfane) "Falcon AI is here to help and listen — tell us what went wrong!" else "+20 Coins • $streakMotivation"
            )
        }
    }

    fun claimDailyStreakReward() {
        val profile = currentProfileOrDefault()
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        if (profile.lastStreakCheckInDate == today) {
            _toastMessage.value = "You already claimed today's streak reward! Current Streak: ${profile.studyStreak} Day(s)."
            return
        }

        val nextStreak = profile.studyStreak + 1
        val rewardCoins = LuminoCatalog.getDailyStreakRewardCoins(nextStreak)
        val title = LuminoCatalog.getStreakCongratulatoryTitle(nextStreak)
        val motivation = LuminoCatalog.getStreakMotivationMessage(nextStreak)

        val newBadges = profile.unlockedBadges.toMutableList()
        if (nextStreak >= 7 && !newBadges.contains("seven_day_orbit")) {
            newBadges.add("seven_day_orbit")
        }

        _localProfile.value = profile.copy(
            studyStreak = nextStreak,
            coins = profile.coins + rewardCoins,
            xp = profile.xp + 30,
            lastStreakCheckInDate = today,
            unlockedBadges = newBadges
        )

        _toastMessage.value = "$title Claimed +$rewardCoins Star Coins! $motivation"

        if (isCloudSyncActive) {
            viewModelScope.launch {
                repository.recordStudyReward(
                    coinsDelta = rewardCoins,
                    xpDelta = 30,
                    incrementStudyStreak = true,
                    currentProfile = profile
                )
            }
        }
    }

    fun selectQuizSubject(tag: String) {
        val profile = currentProfileOrDefault()
        val current = _quizArena.value
        val updatedQuestions = LuminoCatalog.getCurriculumQuizForGrade(
            gradeLevel = profile.selectedGrade,
            subjectTag = tag,
            language = profile.selectedLanguage,
            difficulty = current.selectedDifficulty,
            questionCount = current.selectedQuestionCount
        )
        _quizArena.value = current.copy(
            selectedSubjectTag = tag,
            activeGrade = profile.selectedGrade,
            activeLanguage = profile.selectedLanguage,
            questions = updatedQuestions,
            currentQuestionIndex = 0,
            selectedOptionIndex = null,
            isAnswerSubmitted = false,
            isHintVisible = false,
            correctCount = 0,
            isQuizFinished = false
        )
    }

    fun selectQuizDifficulty(difficulty: String) {
        val profile = currentProfileOrDefault()
        val current = _quizArena.value
        val updatedQuestions = LuminoCatalog.getCurriculumQuizForGrade(
            gradeLevel = profile.selectedGrade,
            subjectTag = current.selectedSubjectTag,
            language = profile.selectedLanguage,
            difficulty = difficulty,
            questionCount = current.selectedQuestionCount
        )
        _quizArena.value = current.copy(
            selectedDifficulty = difficulty,
            activeGrade = profile.selectedGrade,
            activeLanguage = profile.selectedLanguage,
            questions = updatedQuestions,
            currentQuestionIndex = 0,
            selectedOptionIndex = null,
            isAnswerSubmitted = false,
            isHintVisible = false,
            correctCount = 0,
            isQuizFinished = false
        )
    }

    fun selectQuizQuestionCount(count: Int) {
        val safeCount = count.coerceIn(1, 20)
        val profile = currentProfileOrDefault()
        val current = _quizArena.value
        val updatedQuestions = LuminoCatalog.getCurriculumQuizForGrade(
            gradeLevel = profile.selectedGrade,
            subjectTag = current.selectedSubjectTag,
            language = profile.selectedLanguage,
            difficulty = current.selectedDifficulty,
            questionCount = safeCount
        )
        _quizArena.value = current.copy(
            selectedQuestionCount = safeCount,
            activeGrade = profile.selectedGrade,
            activeLanguage = profile.selectedLanguage,
            questions = updatedQuestions,
            currentQuestionIndex = 0,
            selectedOptionIndex = null,
            isAnswerSubmitted = false,
            isHintVisible = false,
            correctCount = 0,
            isQuizFinished = false
        )
    }

    fun toggleQuizHint() {
        val current = _quizArena.value
        _quizArena.value = current.copy(isHintVisible = !current.isHintVisible)
    }

    fun startDailyQuiz(subjectOverride: String? = null) {
        val profile = currentProfileOrDefault()
        val current = _quizArena.value
        val subject = subjectOverride ?: current.selectedSubjectTag
        val difficulty = current.selectedDifficulty
        val count = current.selectedQuestionCount

        _quizArena.value = current.copy(
            selectedSubjectTag = subject,
            activeGrade = profile.selectedGrade,
            activeLanguage = profile.selectedLanguage,
            isGeneratingQuiz = true,
            questions = LuminoCatalog.getCurriculumQuizForGrade(
                gradeLevel = profile.selectedGrade,
                subjectTag = subject,
                language = profile.selectedLanguage,
                difficulty = difficulty,
                questionCount = count
            ),
            currentQuestionIndex = 0,
            selectedOptionIndex = null,
            isAnswerSubmitted = false,
            isHintVisible = false,
            correctCount = 0,
            isQuizFinished = false
        )

        viewModelScope.launch {
            val questions = geminiService.generateDailyQuiz(
                gradeLevel = profile.selectedGrade,
                subjectTag = subject,
                language = profile.selectedLanguage,
                difficulty = difficulty,
                questionCount = count
            )
            _quizArena.value = _quizArena.value.copy(
                isGeneratingQuiz = false,
                questions = questions,
                currentQuestionIndex = 0,
                selectedOptionIndex = null,
                isAnswerSubmitted = false,
                isHintVisible = false,
                correctCount = 0,
                isQuizFinished = false,
                coinsRewarded = 0
            )
        }
    }

    fun selectQuizOption(index: Int) {
        val current = _quizArena.value
        if (current.isAnswerSubmitted) return
        _quizArena.value = current.copy(selectedOptionIndex = index)
    }

    fun submitCurrentQuizAnswer() {
        val current = _quizArena.value
        val selected = current.selectedOptionIndex ?: return
        if (current.isAnswerSubmitted) return
        val currentQ = current.questions.getOrNull(current.currentQuestionIndex) ?: return
        val isCorrect = selected == currentQ.correctIndex
        _quizArena.value = current.copy(
            isAnswerSubmitted = true,
            correctCount = if (isCorrect) current.correctCount + 1 else current.correctCount
        )
    }

    fun nextQuizQuestionOrFinish() {
        val current = _quizArena.value
        if (current.currentQuestionIndex + 1 < current.questions.size) {
            _quizArena.value = current.copy(
                currentQuestionIndex = current.currentQuestionIndex + 1,
                selectedOptionIndex = null,
                isAnswerSubmitted = false,
                isHintVisible = false
            )
        } else {
            val profile = currentProfileOrDefault()
            val totalQ = current.questions.size.coerceAtLeast(1)
            val perQuestionCoins = LuminoCatalog.coinsPerCorrectForDifficulty(current.selectedDifficulty)
            val earnedCoins = (current.correctCount * perQuestionCoins) + 10
            _quizArena.value = current.copy(
                isQuizFinished = true,
                coinsRewarded = earnedCoins
            )

            val newBadges = profile.unlockedBadges.toMutableList()
            if (current.correctCount * 100 / totalQ >= 80 && !newBadges.contains("quiz_supernova")) {
                newBadges.add("quiz_supernova")
            }
            val nextQuizStreak = profile.quizStreak + 1
            if (nextQuizStreak >= 7 && !newBadges.contains("seven_day_orbit")) {
                newBadges.add("seven_day_orbit")
            }

            _localProfile.value = profile.copy(
                coins = profile.coins + earnedCoins,
                xp = profile.xp + current.correctCount * 25,
                quizStreak = nextQuizStreak,
                unlockedBadges = newBadges
            )

            val attempt = QuizAttempt(
                id = "quiz_${UUID.randomUUID()}",
                userId = currentUserId,
                subjectTag = "${current.selectedSubjectTag} (${current.selectedDifficulty})",
                gradeLevel = profile.selectedGrade,
                language = profile.selectedLanguage,
                score = current.correctCount,
                totalQuestions = totalQ,
                coinsEarned = earnedCoins
            )
            _localQuizHistory.value = listOf(attempt) + _localQuizHistory.value
            val motivation = LuminoCatalog.getStreakMotivationMessage(nextQuizStreak)
            _toastMessage.value = "Quiz Complete! +$earnedCoins Coins • $motivation"

            if (isCloudSyncActive) {
                viewModelScope.launch {
                    repository.saveQuizAttemptAndReward(
                        subjectTag = "${current.selectedSubjectTag} (${current.selectedDifficulty})",
                        gradeLevel = profile.selectedGrade,
                        language = profile.selectedLanguage,
                        score = current.correctCount,
                        totalQuestions = totalQ,
                        coinsEarned = earnedCoins,
                        currentProfile = profile
                    )
                }
            }
        }
    }

    fun simulateMissedDayStreak() {
        val profile = currentProfileOrDefault()
        val savedStudy = profile.studyStreak.coerceAtLeast(5)
        val savedQuiz = profile.quizStreak.coerceAtLeast(4)
        _localProfile.value = profile.copy(
            studyStreak = 0,
            quizStreak = 0,
            brokenStudyStreakToRecover = savedStudy,
            brokenQuizStreakToRecover = savedQuiz
        )
        _toastMessage.value = "Simulated missed day! Use your Star Coins to recover your Study or Quiz Streak."

        if (isCloudSyncActive) {
            viewModelScope.launch {
                repository.simulateMissedStreakForTesting(profile)
            }
        }
    }

    fun recoverStreakWithCoins(recoverStudy: Boolean) {
        val profile = currentProfileOrDefault()
        val coinCost = 75
        if (profile.coins < coinCost) {
            _toastMessage.value = "Not enough Star Coins (75 needed)."
            return
        }
        val newBadges = profile.unlockedBadges.toMutableList()
        if (!newBadges.contains("streak_guardian")) newBadges.add("streak_guardian")

        _localProfile.value = if (recoverStudy) {
            profile.copy(
                coins = (profile.coins - coinCost).coerceAtLeast(0),
                studyStreak = profile.brokenStudyStreakToRecover.coerceAtLeast(1),
                brokenStudyStreakToRecover = 0,
                unlockedBadges = newBadges
            )
        } else {
            profile.copy(
                coins = (profile.coins - coinCost).coerceAtLeast(0),
                quizStreak = profile.brokenQuizStreakToRecover.coerceAtLeast(1),
                brokenQuizStreakToRecover = 0,
                unlockedBadges = newBadges
            )
        }
        val label = if (recoverStudy) "Study Streak" else "Quiz Streak"
        _toastMessage.value = "Time-Warp Complete! Your $label has been restored!"

        if (isCloudSyncActive) {
            viewModelScope.launch {
                repository.recoverStreakWithCoins(
                    recoverStudy = recoverStudy,
                    coinCost = coinCost,
                    currentProfile = profile
                )
            }
        }
    }

    fun buyStreakShield() {
        val profile = currentProfileOrDefault()
        val shieldCost = 75
        if (profile.coins < shieldCost) {
            _toastMessage.value = "Need 75 Star Coins to buy a Shield."
            return
        }
        val newBadges = profile.unlockedBadges.toMutableList()
        if (!newBadges.contains("streak_guardian")) newBadges.add("streak_guardian")

        _localProfile.value = profile.copy(
            coins = (profile.coins - shieldCost).coerceAtLeast(0),
            streakShields = profile.streakShields + 1,
            unlockedBadges = newBadges
        )
        _toastMessage.value = "Streak Freeze Shield equipped! (-75 Star Coins)"

        if (isCloudSyncActive) {
            viewModelScope.launch {
                repository.buyStreakShieldWithCoins(
                    shieldCost = shieldCost,
                    currentProfile = profile
                )
            }
        }
    }

    fun updateStudyReminder(enabled: Boolean, hour: Int, minute: Int, days: List<String>) {
        val profile = currentProfileOrDefault()
        _localProfile.value = profile.copy(
            reminderEnabled = enabled,
            reminderHour = hour.coerceIn(0, 23),
            reminderMinute = minute.coerceIn(0, 59),
            reminderDays = days
        )
        _toastMessage.value = if (enabled) {
            "Study reminder set for %02d:%02d (%s)".format(hour, minute, days.joinToString(", "))
        } else {
            "Study reminders paused"
        }

        if (isCloudSyncActive) {
            viewModelScope.launch {
                repository.updateReminderSchedule(enabled, hour, minute, days)
            }
        }
    }

    fun awardCodingReward(coinsEarned: Int, xpEarned: Int, toastMsg: String) {
        val profile = currentProfileOrDefault()
        _localProfile.value = profile.copy(
            coins = profile.coins + coinsEarned.coerceAtLeast(0),
            xp = profile.xp + xpEarned.coerceAtLeast(0)
        )
        _toastMessage.value = toastMsg
    }

    fun showCustomToast(message: String) {
        _toastMessage.value = message
    }

    companion object {
        private const val TAG = "LuminoViewModel"
    }
}
