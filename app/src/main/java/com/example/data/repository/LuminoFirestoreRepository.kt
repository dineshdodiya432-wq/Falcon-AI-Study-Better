package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.HomeworkSession
import com.example.data.model.QuizAttempt
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LuminoFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun currentIsoDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                throw error
            }
    }

    fun observeHomeworkSessions(userId: String): Flow<List<HomeworkSession>> {
        val collectionPath = "users/$userId/homework"
        return db.collection("users").document(userId).collection("homework")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(HomeworkSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, collectionPath)
                throw error
            }
    }

    fun observeQuizAttempts(userId: String): Flow<List<QuizAttempt>> {
        val collectionPath = "users/$userId/quizzes"
        return db.collection("users").document(userId).collection("quizzes")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(QuizAttempt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, collectionPath)
                throw error
            }
    }

    suspend fun ensureUserProfileInitialized(displayName: String = "Cosmic Explorer"): Result<UserProfile> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existing = existingSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existing != null) {
                    return Result.success(existing)
                }
            }

            val today = currentIsoDate()
            val safeName = displayName.trim().ifEmpty { "Cosmic Explorer" }.take(100)
            val payload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "selectedGrade" to 6,
                "selectedLanguage" to "Simple English",
                "curriculumTags" to listOf("Algebra", "Life Science", "World History"),
                "coins" to 150,
                "xp" to 120,
                "studyStreak" to 3,
                "quizStreak" to 2,
                "brokenStudyStreakToRecover" to 0,
                "brokenQuizStreakToRecover" to 0,
                "streakShields" to 1,
                "unlockedBadges" to listOf("first_launch"),
                "reminderEnabled" to true,
                "reminderHour" to 17,
                "reminderMinute" to 30,
                "reminderDays" to listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
                "lastStudyDate" to today,
                "lastQuizDate" to today,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            // Also seed one initial welcome homework session if the user has none
            val hwRef = docRef.collection("homework")
            val existingHw = hwRef.whereEqualTo("userId", uid).get().await()
            if (existingHw.isEmpty) {
                val sampleHw = mapOf(
                    "userId" to uid,
                    "questionTitle" to "How do plants use sunlight to make their own food?",
                    "subjectTag" to "Life Science",
                    "gradeLevel" to 6,
                    "language" to "Simple English",
                    "steps" to listOf(
                        "Step 1 (Sunlight Capture): Green leaves contain tiny solar panels called chloroplasts filled with chlorophyll that absorb sunlight.",
                        "Step 2 (Water & Air Intake): Roots drink water (H2O) from the soil while tiny leaf pores called stomata breathe in carbon dioxide (CO2) from the air.",
                        "Step 3 (Making Plant Sugar): Inside the leaf, sunlight energy combines water and carbon dioxide to build glucose (plant food) and releases fresh oxygen (O2) into the air!"
                    ),
                    "finalAnswer" to "Plants make food through Photosynthesis: Sunlight + Water + Carbon Dioxide -> Glucose (Sugar) + Oxygen.",
                    "keyConcept" to "Photosynthesis • Chlorophyll (green pigment) • Stomata (leaf pores)",
                    "practiceQuestion" to "Which gas do plants release into the air during photosynthesis that humans need to breathe?",
                    "practiceOptions" to listOf("Oxygen", "Carbon Dioxide", "Helium", "Nitrogen"),
                    "practiceCorrectIndex" to 0,
                    "practiceCompleted" to false,
                    "coinsEarned" to 20,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                hwRef.add(sampleHw).await()
            }

            val created = docRef.get().await().toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(id = uid, userId = uid, displayName = safeName)
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserProfileById(targetUserId: String): Result<UserProfile?> {
        val docRef = db.collection("users").document(targetUserId)
        return try {
            val snap = docRef.get(com.google.firebase.firestore.Source.SERVER).await()
            Result.success(snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateGradeAndLanguage(
        grade: Int,
        language: String,
        curriculumTags: List<String>,
        currentUnlockedBadges: List<String>,
        currentCoins: Int
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val safeGrade = grade.coerceIn(1, 12)
            val safeLang = language.trim().ifEmpty { "Simple English" }.take(50)
            val safeTags = curriculumTags.map { it.take(60) }.take(20)
            val updatedBadges = currentUnlockedBadges.toMutableList()
            var bonusCoins = 0
            if (safeLang != "Simple English" && !updatedBadges.contains("polyglot_scholar")) {
                updatedBadges.add("polyglot_scholar")
                bonusCoins = 45
            }
            val updates = mapOf(
                "selectedGrade" to safeGrade,
                "selectedLanguage" to safeLang,
                "curriculumTags" to safeTags,
                "unlockedBadges" to updatedBadges.take(30),
                "coins" to (currentCoins + bonusCoins).coerceAtMost(1000000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveHomeworkSession(
        questionTitle: String,
        subjectTag: String,
        gradeLevel: Int,
        language: String,
        steps: List<String>,
        finalAnswer: String,
        keyConcept: String,
        practiceQuestion: String,
        practiceOptions: List<String>,
        practiceCorrectIndex: Int,
        coinsEarned: Int = 20
    ): Result<String> {
        val uid = requireUserId()
        val hwCollection = db.collection("users").document(uid).collection("homework")
        return try {
            val safeSteps = if (steps.isEmpty()) listOf("Step 1: Analyze the problem statement.") else steps.map { it.take(2000) }.take(15)
            val safeOptions = practiceOptions.map { it.take(500) }.take(6)
            val payload = mapOf(
                "userId" to uid,
                "questionTitle" to questionTitle.trim().ifEmpty { "Homework Question" }.take(500),
                "subjectTag" to subjectTag.trim().ifEmpty { "General" }.take(100),
                "gradeLevel" to gradeLevel.coerceIn(1, 12),
                "language" to language.trim().ifEmpty { "Simple English" }.take(50),
                "steps" to safeSteps,
                "finalAnswer" to finalAnswer.trim().ifEmpty { "See step-by-step guide above." }.take(2000),
                "keyConcept" to keyConcept.trim().ifEmpty { "Core Principle" }.take(1000),
                "practiceQuestion" to practiceQuestion.trim().take(1000),
                "practiceOptions" to safeOptions,
                "practiceCorrectIndex" to practiceCorrectIndex.coerceIn(0, 5),
                "practiceCompleted" to false,
                "coinsEarned" to coinsEarned.coerceIn(0, 1000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val docRef = hwCollection.add(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, hwCollection.path)
            Result.failure(e)
        }
    }

    suspend fun completeHomeworkPracticeCheck(
        homeworkId: String,
        bonusCoins: Int,
        currentProfile: UserProfile
    ): Result<Unit> {
        val uid = requireUserId()
        val hwDocRef = db.collection("users").document(uid).collection("homework").document(homeworkId)
        val userDocRef = db.collection("users").document(uid)
        return try {
            hwDocRef.update(
                mapOf(
                    "practiceCompleted" to true,
                    "coinsEarned" to (20 + bonusCoins).coerceIn(0, 1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val newBadges = currentProfile.unlockedBadges.toMutableList()
            if (!newBadges.contains("homework_hero")) {
                newBadges.add("homework_hero")
            }
            val newStreak = (currentProfile.studyStreak + 1).coerceAtMost(10000)
            if (newStreak >= 7 && !newBadges.contains("seven_day_orbit")) {
                newBadges.add("seven_day_orbit")
            }

            userDocRef.update(
                mapOf(
                    "coins" to (currentProfile.coins + bonusCoins).coerceIn(0, 1000000),
                    "xp" to (currentProfile.xp + 35).coerceIn(0, 10000000),
                    "studyStreak" to newStreak,
                    "lastStudyDate" to currentIsoDate(),
                    "unlockedBadges" to newBadges.take(30),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, hwDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordStudyReward(
        coinsDelta: Int,
        xpDelta: Int,
        incrementStudyStreak: Boolean,
        currentProfile: UserProfile
    ): Result<Unit> {
        val uid = requireUserId()
        val userDocRef = db.collection("users").document(uid)
        return try {
            val newStreak = if (incrementStudyStreak) {
                (currentProfile.studyStreak + 1).coerceAtMost(10000)
            } else {
                currentProfile.studyStreak
            }
            val newBadges = currentProfile.unlockedBadges.toMutableList()
            if (!newBadges.contains("curious_spark")) {
                newBadges.add("curious_spark")
            }
            if (newStreak >= 7 && !newBadges.contains("seven_day_orbit")) {
                newBadges.add("seven_day_orbit")
            }
            userDocRef.update(
                mapOf(
                    "coins" to (currentProfile.coins + coinsDelta).coerceIn(0, 1000000),
                    "xp" to (currentProfile.xp + xpDelta).coerceIn(0, 10000000),
                    "studyStreak" to newStreak,
                    "lastStudyDate" to currentIsoDate(),
                    "unlockedBadges" to newBadges.take(30),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveQuizAttemptAndReward(
        subjectTag: String,
        gradeLevel: Int,
        language: String,
        score: Int,
        totalQuestions: Int,
        coinsEarned: Int,
        currentProfile: UserProfile
    ): Result<String> {
        val uid = requireUserId()
        val quizCollection = db.collection("users").document(uid).collection("quizzes")
        val userDocRef = db.collection("users").document(uid)
        return try {
            val payload = mapOf(
                "userId" to uid,
                "subjectTag" to subjectTag.trim().ifEmpty { "General" }.take(100),
                "gradeLevel" to gradeLevel.coerceIn(1, 12),
                "language" to language.trim().ifEmpty { "Simple English" }.take(50),
                "score" to score.coerceIn(0, 20),
                "totalQuestions" to totalQuestions.coerceIn(1, 20),
                "coinsEarned" to coinsEarned.coerceIn(0, 1000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val docRef = quizCollection.add(payload).await()

            val newQuizStreak = (currentProfile.quizStreak + 1).coerceAtMost(10000)
            val newBadges = currentProfile.unlockedBadges.toMutableList()
            if (score * 100 / totalQuestions.coerceAtLeast(1) >= 80 && !newBadges.contains("quiz_supernova")) {
                newBadges.add("quiz_supernova")
            }
            if (newQuizStreak >= 7 && !newBadges.contains("seven_day_orbit")) {
                newBadges.add("seven_day_orbit")
            }

            userDocRef.update(
                mapOf(
                    "coins" to (currentProfile.coins + coinsEarned).coerceIn(0, 1000000),
                    "xp" to (currentProfile.xp + score * 20).coerceIn(0, 10000000),
                    "quizStreak" to newQuizStreak,
                    "lastQuizDate" to currentIsoDate(),
                    "unlockedBadges" to newBadges.take(30),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, quizCollection.path)
            Result.failure(e)
        }
    }

    suspend fun simulateMissedStreakForTesting(currentProfile: UserProfile): Result<Unit> {
        val uid = requireUserId()
        val userDocRef = db.collection("users").document(uid)
        return try {
            val savedStudyStreak = currentProfile.studyStreak.coerceAtLeast(5)
            val savedQuizStreak = currentProfile.quizStreak.coerceAtLeast(4)
            userDocRef.update(
                mapOf(
                    "studyStreak" to 0,
                    "quizStreak" to 0,
                    "brokenStudyStreakToRecover" to savedStudyStreak,
                    "brokenQuizStreakToRecover" to savedQuizStreak,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun recoverStreakWithCoins(
        recoverStudy: Boolean,
        coinCost: Int,
        currentProfile: UserProfile
    ): Result<Unit> {
        val uid = requireUserId()
        val userDocRef = db.collection("users").document(uid)
        return try {
            if (currentProfile.coins < coinCost) {
                return Result.failure(IllegalArgumentException("Not enough Star Coins to recover streak."))
            }
            val newBadges = currentProfile.unlockedBadges.toMutableList()
            if (!newBadges.contains("streak_guardian")) {
                newBadges.add("streak_guardian")
            }
            val updates = if (recoverStudy) {
                val restored = currentProfile.brokenStudyStreakToRecover.coerceAtLeast(1)
                mapOf(
                    "coins" to (currentProfile.coins - coinCost).coerceAtLeast(0),
                    "studyStreak" to restored,
                    "brokenStudyStreakToRecover" to 0,
                    "unlockedBadges" to newBadges.take(30),
                    "lastStudyDate" to currentIsoDate(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            } else {
                val restored = currentProfile.brokenQuizStreakToRecover.coerceAtLeast(1)
                mapOf(
                    "coins" to (currentProfile.coins - coinCost).coerceAtLeast(0),
                    "quizStreak" to restored,
                    "brokenQuizStreakToRecover" to 0,
                    "unlockedBadges" to newBadges.take(30),
                    "lastQuizDate" to currentIsoDate(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            userDocRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun buyStreakShieldWithCoins(
        shieldCost: Int = 75,
        currentProfile: UserProfile
    ): Result<Unit> {
        val uid = requireUserId()
        val userDocRef = db.collection("users").document(uid)
        return try {
            if (currentProfile.coins < shieldCost) {
                return Result.failure(IllegalArgumentException("Need $shieldCost Star Coins to buy a Streak Shield."))
            }
            val newBadges = currentProfile.unlockedBadges.toMutableList()
            if (!newBadges.contains("streak_guardian")) {
                newBadges.add("streak_guardian")
            }
            userDocRef.update(
                mapOf(
                    "coins" to (currentProfile.coins - shieldCost).coerceAtLeast(0),
                    "streakShields" to (currentProfile.streakShields + 1).coerceAtMost(100),
                    "unlockedBadges" to newBadges.take(30),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateReminderSchedule(
        enabled: Boolean,
        hour: Int,
        minute: Int,
        days: List<String>
    ): Result<Unit> {
        val uid = requireUserId()
        val userDocRef = db.collection("users").document(uid)
        return try {
            userDocRef.update(
                mapOf(
                    "reminderEnabled" to enabled,
                    "reminderHour" to hour.coerceIn(0, 23),
                    "reminderMinute" to minute.coerceIn(0, 59),
                    "reminderDays" to days.map { it.take(16) }.take(7),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateDarkMode(enabled: Boolean): Result<Unit> {
        val uid = requireUserId()
        val userDocRef = db.collection("users").document(uid)
        return try {
            userDocRef.update(
                mapOf(
                    "darkModeEnabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
            Result.failure(e)
        }
    }
}
