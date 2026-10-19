package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.UUID

class LuminoRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfileInitialized_createsAndReadsOwnProfile() = runBlocking {
        val uid = signInTestUser("alice_${UUID.randomUUID()}@lumino.test")
        val repository = LuminoFirestoreRepository(firestore, auth)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfileInitialized("Alice Explorer")
        }
        assertThat(result.isSuccess).isTrue()
        val profile = result.getOrThrow()
        assertThat(profile.userId).isEqualTo(uid)
        assertThat(profile.displayName).isEqualTo("Alice Explorer")
        assertThat(profile.coins).isEqualTo(150)
    }

    @Test
    fun saveAndObserveHomeworkSession_authenticatedOwner_emitsSession() = runBlocking {
        val uid = signInTestUser("student_${UUID.randomUUID()}@lumino.test")
        val repository = LuminoFirestoreRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) { repository.ensureUserProfileInitialized("Student") }

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveHomeworkSession(
                questionTitle = "Solve 2x + 4 = 10",
                subjectTag = "Algebra",
                gradeLevel = 7,
                language = "Simple English",
                steps = listOf("Step 1: Subtract 4 from both sides to get 2x = 6.", "Step 2: Divide by 2 to get x = 3."),
                finalAnswer = "x = 3",
                keyConcept = "Linear Equations",
                practiceQuestion = "What is x if 2x + 2 = 8?",
                practiceOptions = listOf("3", "4", "2", "6"),
                practiceCorrectIndex = 0,
                coinsEarned = 25
            )
        }
        assertThat(saveResult.isSuccess).isTrue()
        val hwId = saveResult.getOrThrow()

        val sessions = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeHomeworkSessions(uid).first { list -> list.any { it.id == hwId } }
        }
        assertThat(sessions.map { it.id }).contains(hwId)
    }

    @Test
    fun getUserProfileById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_cross_${UUID.randomUUID()}@lumino.test")
        val aliceRepo = LuminoFirestoreRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.ensureUserProfileInitialized("Alice") }

        signInTestUser("bob_cross_${UUID.randomUUID()}@lumino.test")
        val bobRepo = LuminoFirestoreRepository(firestore, auth)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getUserProfileById(aliceUid)
        }
        assertThat(crossReadResult.isFailure).isTrue()
        val exception = crossReadResult.exceptionOrNull() as? FirebaseFirestoreException
        assertThat(exception?.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }

    @Test
    fun observeHomeworkSessions_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_unauth_${UUID.randomUUID()}@lumino.test")
        auth.signOut()
        val repository = LuminoFirestoreRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeHomeworkSessions(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        val firestoreEx = generateSequence(caught) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        assertThat(firestoreEx).isNotNull()
        assertThat(firestoreEx?.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
