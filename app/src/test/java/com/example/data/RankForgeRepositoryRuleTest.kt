package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.ChatAttachmentType
import com.example.model.QuestionReview
import com.example.model.QuizAttempt
import com.example.model.QuizDifficulty
import com.example.model.QuizMode
import com.example.model.StudyGoalRecord
import com.example.model.StudyInsight
import com.example.model.UserProfile
import com.example.model.buildDirectChatThreadId
import com.example.util.FocusCycleCalculator
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class RankForgeRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndObserveUserProfile_ownerSucceeds() = runBlocking {
        val uid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
        val repo = FirestoreRepository(firestore)

        val saveRes = withTimeout(TIMEOUT_MS) {
            repo.saveUserProfile(
                fullName = "Aarav Sharma",
                dob = "2005-08-14",
                phoneNumber = "+919876543210",
                selectedExamId = "NEET",
                isNewProfile = true
            )
        }
        assertTrue(saveRes.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(uid).first { it != null }
        }
        assertNotNull(profile)
        assertEquals("Aarav Sharma", profile?.fullName)
        assertEquals("NEET", profile?.selectedExamId)
    }

    @Test
    fun submitQuizAttempt_updatesHistoryAndBothLeaderboards() = runBlocking {
        val uid = signInTestUser("mega_${UUID.randomUUID()}@test.com")
        val repo = FirestoreRepository(firestore)

        withTimeout(TIMEOUT_MS) {
            repo.saveUserProfile("Priya Verma", "2004-03-22", "", "NEET", true).getOrThrow()
        }
        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(uid).first { it != null }!!
        }

        val attempt = QuizAttempt(
            userId = uid,
            examId = "NEET",
            examName = "NEET UG",
            mode = QuizMode.MEGA.id,
            subjectNames = listOf("Physics", "Chemistry", "Biology"),
            chapterSummary = "Electrostatics, Bonding, Genetics",
            difficulty = QuizDifficulty.HARD.id,
            totalQuestions = 3L,
            correctCount = 2L,
            wrongCount = 1L,
            skippedCount = 0L,
            scorePoints = 350L,
            accuracyPercent = 66.7,
            timePerQuestionSec = 45L,
            timeTakenSec = 90L,
            questionReviews = listOf(
                QuestionReview(
                    question = "Q1",
                    options = listOf("A", "B", "C", "D"),
                    correctIndex = 0,
                    selectedIndex = 0,
                    explanation = "Exp 1",
                    subject = "Physics",
                    chapter = "Electrostatics",
                    isCorrect = true
                )
            )
        )

        val attemptId = withTimeout(TIMEOUT_MS) {
            repo.submitQuizAttempt(profile, attempt).getOrThrow()
        }
        assertTrue(attemptId.isNotBlank())

        val attempts = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeQuizAttempts(uid).first { it.any { a -> a.id == attemptId } }
        }
        assertTrue(attempts.isNotEmpty())

        val examLb = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeExamLeaderboard("NEET").first { it.any { e -> e.userId == uid } }
        }
        assertTrue(examLb.any { it.userId == uid && it.scorePoints >= 350L })

        val megaLb = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMegaLeaderboard("NEET").first { it.any { e -> e.userId == uid } }
        }
        assertTrue(megaLb.any { it.userId == uid && it.scorePoints >= 350L })
    }

    @Test
    fun studyInsights_ownerCanSaveAndRead() = runBlocking {
        val uid = signInTestUser("insight_${UUID.randomUUID()}@test.com")
        val repo = FirestoreRepository(firestore)

        val insight = StudyInsight(
            userId = uid,
            examId = "JEE",
            headline = "Strong in Calculus, sharpen Electrostatics.",
            recommendations = listOf("Revise Gauss's Law daily", "Attempt 10 Hard MCQs"),
            focusTopics = listOf("Electrostatics", "Rotational Dynamics")
        )
        val res = withTimeout(TIMEOUT_MS) { repo.saveStudyInsight(insight) }
        assertTrue(res.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeStudyInsight(uid).first { it != null }
        }
        assertEquals("JEE", observed?.examId)
    }

    @Test
    fun studyGoalAndTimeLeaderboard_saveAndObserveSucceeds() = runBlocking {
        val uid = signInTestUser("focus_${UUID.randomUUID()}@test.com")
        val repo = FirestoreRepository(firestore)

        withTimeout(TIMEOUT_MS) {
            repo.saveUserProfile("Kabir Patel", "2005-05-10", "", "JEE", true).getOrThrow()
        }
        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(uid).first { it != null }!!
        }

        val todayKey = FocusCycleCalculator.dateKeyFor()
        val goalRes = withTimeout(TIMEOUT_MS) {
            repo.saveStudyGoal(
                StudyGoalRecord(
                    id = todayKey,
                    userId = uid,
                    dateKey = todayKey,
                    targetMinutes = 120L,
                    loggedSeconds = 600L,
                    activeSessionStartedAtMs = 0L,
                    cycleAnchorEpochMs = System.currentTimeMillis(),
                    activeSubject = "Mathematics",
                    blockedPackages = listOf("com.instagram.android", "com.google.android.youtube")
                )
            )
        }
        assertTrue(goalRes.isSuccess)

        val observedGoal = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeStudyGoal(uid, todayKey).first { it != null }
        }
        assertEquals(120L, observedGoal?.targetMinutes)

        val timeRes = withTimeout(TIMEOUT_MS) {
            repo.recordStudySeconds(profile = profile, addedSeconds = 900L, creditDailyGoal = false)
        }
        assertTrue(timeRes.isSuccess)

        val lb = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeStudyTimeLeaderboard().first { list -> list.any { it.userId == uid } }
        }
        assertTrue(lb.any { it.userId == uid && it.daySeconds >= 900L && it.weekSeconds >= 900L && it.monthSeconds >= 900L })
    }

    @Test
    fun studyChat_threeWarningsThenFourthStrikeDisablesChatsForOneWeek() = runBlocking {
        val bobUid = signInTestUser("peer_bob_${UUID.randomUUID()}@test.com")
        val aliceUid = signInTestUser("chat_alice_${UUID.randomUUID()}@test.com")
        val repo = FirestoreRepository(firestore)

        // Alice can send exam chat and 1-on-1 DM initially
        val examMsgId = withTimeout(TIMEOUT_MS) {
            repo.sendExamChatMessage(
                senderName = "Alice",
                examId = "NEET",
                text = "Anyone revising Organic Chemistry?",
                attachmentType = ChatAttachmentType.NONE
            ).getOrThrow()
        }
        assertTrue(examMsgId.isNotBlank())

        val dmMsgId = withTimeout(TIMEOUT_MS) {
            repo.sendDirectChatMessage(
                senderName = "Alice",
                peerId = bobUid,
                peerName = "Bob",
                examId = "NEET",
                text = "Let's solve 20 MCQs together!"
            ).getOrThrow()
        }
        assertTrue(dmMsgId.isNotBlank())

        val threadId = buildDirectChatThreadId(aliceUid, bobUid)
        val dmMessages = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeDirectChatMessages(threadId, aliceUid).first { it.any { m -> m.id == dmMsgId } }
        }
        assertTrue(dmMessages.isNotEmpty())

        // Strikes 1, 2, 3 issue warnings without banning yet
        for (expectedWarning in 1L..3L) {
            val status = withTimeout(TIMEOUT_MS) {
                repo.recordChatPolicyViolation("Warning $expectedWarning").getOrThrow()
            }
            assertEquals(expectedWarning, status.warningCount)
            assertFalse(status.isCurrentlyBanned())
        }

        // 4th violation after 3 warnings imposes a 7-day ban
        val banStatus = withTimeout(TIMEOUT_MS) {
            repo.recordChatPolicyViolation("Adult video sent after 3 warnings").getOrThrow()
        }
        assertEquals(4L, banStatus.warningCount)
        assertTrue(banStatus.isCurrentlyBanned())

        // Subsequent exam chat & DM writes fail with PERMISSION_DENIED enforced by Firestore rules
        val examDenied = withTimeout(TIMEOUT_MS) {
            repo.sendExamChatMessage(
                senderName = "Alice",
                examId = "NEET",
                text = "Should be blocked by Firestore rules"
            )
        }
        assertTrue(examDenied.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            examDenied.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        val dmDenied = withTimeout(TIMEOUT_MS) {
            repo.sendDirectChatMessage(
                senderName = "Alice",
                peerId = bobUid,
                peerName = "Bob",
                examId = "NEET",
                text = "Blocked DM"
            )
        }
        assertTrue(dmDenied.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            dmDenied.exceptionOrNull()?.unwrapFirestoreException()?.code
        )
    }

    @Test
    fun crossUserProfileRead_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_iso_${UUID.randomUUID()}@test.com")
        val aliceRepo = FirestoreRepository(firestore)
        withTimeout(TIMEOUT_MS) {
            aliceRepo.saveUserProfile("Alice", "2005-01-01", "1234567890", "NEET", true).getOrThrow()
        }

        signInTestUser("bob_iso_${UUID.randomUUID()}@test.com")
        val bobRepo = FirestoreRepository(firestore)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeUserProfile(aliceUid).first()
            }
        } catch (e: Exception) {
            caught = e
        }
        val fsEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException for cross-user read, got $caught", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun unauthenticatedLeaderboardRead_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = FirestoreRepository(firestore)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeExamLeaderboard("NEET").first()
            }
        } catch (e: Exception) {
            caught = e
        }
        val fsEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException when signed out, got $caught", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
