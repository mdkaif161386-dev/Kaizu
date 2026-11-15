package com.example.data

import android.content.Context
import com.example.R
import com.example.model.ChatAttachmentType
import com.example.model.ChatModerationStatus
import com.example.model.DirectChatThread
import com.example.model.FlashcardDeck
import com.example.model.FlashcardItem
import com.example.model.LeaderboardEntry
import com.example.model.MegaLeaderboardEntry
import com.example.model.QuizAttempt
import com.example.model.QuizDifficulty
import com.example.model.QuizMode
import com.example.model.SavedQuestion
import com.example.model.StudyChatMessage
import com.example.model.StudyGoalRecord
import com.example.model.StudyInsight
import com.example.model.StudyTimeLeaderboardEntry
import com.example.model.UserProfile
import com.example.model.buildDirectChatThreadId
import com.example.model.savedQuestionDocId
import com.example.util.FocusCycleCalculator
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, docRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun saveUserProfile(
        fullName: String,
        dob: String,
        phoneNumber: String,
        selectedExamId: String,
        isNewProfile: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            if (isNewProfile) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "fullName" to fullName.trim(),
                    "dob" to dob.trim(),
                    "selectedExamId" to selectedExamId,
                    "totalPoints" to 0L,
                    "quizzesCompleted" to 0L,
                    "totalQuestionsSolved" to 0L,
                    "totalCorrectAnswers" to 0L,
                    "megaQuizzesCompleted" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (phoneNumber.isNotBlank()) {
                    payload["phoneNumber"] = phoneNumber.trim()
                }
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "fullName" to fullName.trim(),
                    "dob" to dob.trim(),
                    "phoneNumber" to phoneNumber.trim(),
                    "selectedExamId" to selectedExamId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNewProfile) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateSelectedExam(examId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "selectedExamId" to examId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeQuizAttempts(userId: String): Flow<List<QuizAttempt>> = flow {
        val colRef = db.collection("users").document(userId).collection("quiz_attempts")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        QuizAttempt::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun submitQuizAttempt(
        profile: UserProfile,
        attempt: QuizAttempt
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val userDocRef = db.collection("users").document(uid)
        val attemptDocRef = userDocRef.collection("quiz_attempts").document()

        val attemptPayload = mapOf(
            "userId" to uid,
            "examId" to attempt.examId,
            "examName" to attempt.examName,
            "mode" to attempt.mode,
            "subjectNames" to attempt.subjectNames,
            "chapterSummary" to attempt.chapterSummary.take(300),
            "difficulty" to attempt.difficulty,
            "totalQuestions" to attempt.totalQuestions,
            "correctCount" to attempt.correctCount,
            "wrongCount" to attempt.wrongCount,
            "skippedCount" to attempt.skippedCount,
            "scorePoints" to attempt.scorePoints,
            "accuracyPercent" to attempt.accuracyPercent,
            "timePerQuestionSec" to attempt.timePerQuestionSec,
            "timeTakenSec" to attempt.timeTakenSec,
            "questionReviews" to attempt.questionReviews.map { it.toMap() },
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            attemptDocRef.set(attemptPayload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, attemptDocRef.path)
            throw e
        }

        // Update user aggregate stats
        try {
            val isMega = attempt.mode == QuizMode.MEGA.id
            userDocRef.update(
                mapOf(
                    "totalPoints" to FieldValue.increment(attempt.scorePoints),
                    "quizzesCompleted" to FieldValue.increment(1L),
                    "totalQuestionsSolved" to FieldValue.increment(attempt.totalQuestions),
                    "totalCorrectAnswers" to FieldValue.increment(attempt.correctCount),
                    "megaQuizzesCompleted" to FieldValue.increment(if (isMega) 1L else 0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
            throw e
        }

        // Update Exam-Specific Leaderboard
        val lbDocRef = db.collection("leaderboards")
            .document(attempt.examId)
            .collection("entries")
            .document(uid)
        try {
            val existingSnap = lbDocRef.get().await()
            val displayName = profile.fullName.ifBlank { "Student" }.take(80)
            if (existingSnap.exists()) {
                val prevBest = existingSnap.getDouble("bestAccuracy") ?: 0.0
                lbDocRef.update(
                    mapOf(
                        "displayName" to displayName,
                        "examName" to attempt.examName,
                        "scorePoints" to FieldValue.increment(attempt.scorePoints),
                        "bestAccuracy" to maxOf(prevBest, attempt.accuracyPercent),
                        "quizzesCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                lbDocRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to displayName,
                        "examId" to attempt.examId,
                        "examName" to attempt.examName,
                        "scorePoints" to attempt.scorePoints,
                        "bestAccuracy" to attempt.accuracyPercent,
                        "quizzesCount" to 1L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, lbDocRef.path)
            throw e
        }

        // If Mega Quiz, also update Mega Quiz Arena Leaderboard
        if (attempt.mode == QuizMode.MEGA.id) {
            val megaDocRef = db.collection("mega_leaderboards")
                .document(attempt.examId)
                .collection("entries")
                .document(uid)
            try {
                val existingMega = megaDocRef.get().await()
                val displayName = profile.fullName.ifBlank { "Champion" }.take(80)
                if (existingMega.exists()) {
                    val prevBestAcc = existingMega.getDouble("bestAccuracy") ?: 0.0
                    val prevBestTime = existingMega.getLong("bestTimeSec") ?: Long.MAX_VALUE
                    val prevDiff = existingMega.getString("highestDifficulty") ?: QuizDifficulty.EASY.id
                    val bestDiff = higherDifficulty(prevDiff, attempt.difficulty)
                    val newBestTime = if (prevBestTime <= 0L) attempt.timeTakenSec else minOf(prevBestTime, attempt.timeTakenSec)

                    megaDocRef.update(
                        mapOf(
                            "displayName" to displayName,
                            "examName" to attempt.examName,
                            "scorePoints" to FieldValue.increment(attempt.scorePoints),
                            "bestAccuracy" to maxOf(prevBestAcc, attempt.accuracyPercent),
                            "megaQuizzesCount" to FieldValue.increment(1L),
                            "highestDifficulty" to bestDiff,
                            "bestTimeSec" to newBestTime,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    megaDocRef.set(
                        mapOf(
                            "userId" to uid,
                            "displayName" to displayName,
                            "examId" to attempt.examId,
                            "examName" to attempt.examName,
                            "scorePoints" to attempt.scorePoints,
                            "bestAccuracy" to attempt.accuracyPercent,
                            "megaQuizzesCount" to 1L,
                            "highestDifficulty" to attempt.difficulty,
                            "bestTimeSec" to attempt.timeTakenSec,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, megaDocRef.path)
                throw e
            }
        }

        if (attempt.timeTakenSec > 0L) {
            recordStudySecondsInternal(
                uid = uid,
                displayName = profile.fullName.ifBlank { "Student" }.take(80),
                examId = attempt.examId,
                examName = attempt.examName,
                addedSeconds = attempt.timeTakenSec,
                creditDailyGoal = true
            )
        }

        attemptDocRef.id
    }

    fun observeExamLeaderboard(examId: String): Flow<List<LeaderboardEntry>> = flow {
        val colRef = db.collection("leaderboards").document(examId).collection("entries")
        emitAll(
            colRef.orderBy("scorePoints", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, auth.currentUser?.uid)
                    throw error
                }
        )
    }

    fun observeMegaLeaderboard(examId: String): Flow<List<MegaLeaderboardEntry>> = flow {
        val colRef = db.collection("mega_leaderboards").document(examId).collection("entries")
        emitAll(
            colRef.orderBy("scorePoints", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MegaLeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, auth.currentUser?.uid)
                    throw error
                }
        )
    }

    fun observeStudyInsight(userId: String): Flow<StudyInsight?> = flow {
        val docRef = db.collection("users").document(userId)
            .collection("study_insights").document("latest")
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            StudyInsight::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, docRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun saveStudyInsight(insight: StudyInsight): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("study_insights").document("latest")
        val payload = mapOf(
            "userId" to uid,
            "examId" to insight.examId,
            "headline" to insight.headline.take(200),
            "recommendations" to insight.recommendations.take(10),
            "focusTopics" to insight.focusTopics.take(10),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeFlashcardDecks(userId: String): Flow<List<FlashcardDeck>> = flow {
        val colRef = db.collection("users").document(userId).collection("flashcard_decks")
        emitAll(
            colRef.orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FlashcardDeck::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun saveFlashcardDeck(deck: FlashcardDeck): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("flashcard_decks").document()
        val payload = mapOf(
            "userId" to uid,
            "examId" to deck.examId,
            "examName" to deck.examName,
            "title" to deck.title.take(160),
            "subjectNames" to deck.subjectNames.take(10),
            "chapters" to deck.chapters.take(30),
            "focus" to deck.focus,
            "cards" to deck.cards.take(25).map { it.toMap() },
            "masteredCount" to deck.cards.count { it.mastered }.toLong(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateFlashcardMastery(
        deckId: String,
        cards: List<FlashcardItem>
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("flashcard_decks").document(deckId)
        val payload = mapOf(
            "cards" to cards.take(25).map { it.toMap() },
            "masteredCount" to cards.count { it.mastered }.toLong(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteFlashcardDeck(deckId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("flashcard_decks").document(deckId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeSavedQuestions(userId: String): Flow<List<SavedQuestion>> = flow {
        val colRef = db.collection("users").document(userId).collection("saved_questions")
        emitAll(
            colRef.orderBy("savedAt", Query.Direction.DESCENDING)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SavedQuestion::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun saveQuestionBookmark(saved: SavedQuestion): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = saved.id.ifBlank {
            savedQuestionDocId(saved.question, saved.subject, saved.chapter)
        }
        val docRef = db.collection("users").document(uid)
            .collection("saved_questions").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "examId" to saved.examId,
            "examName" to saved.examName,
            "subject" to saved.subject.take(80).ifBlank { "General" },
            "chapter" to saved.chapter.take(160).ifBlank { "Core" },
            "difficulty" to saved.difficulty,
            "question" to saved.question.take(2000),
            "options" to saved.options.take(6),
            "correctIndex" to saved.correctIndex.coerceIn(0, (saved.options.size - 1).coerceAtLeast(0)),
            "explanation" to saved.explanation.take(4000).ifBlank { "Review the correct option above." },
            "savedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun removeQuestionBookmark(questionDocId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("saved_questions").document(questionDocId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // ---- Daily Study Goal & Social Media Blocker ----

    fun observeStudyGoal(userId: String, dateKey: String): Flow<StudyGoalRecord?> = flow {
        val docRef = db.collection("users").document(userId)
            .collection("study_goals").document(dateKey)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            StudyGoalRecord::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, docRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun saveStudyGoal(goal: StudyGoalRecord): Result<Unit> = runCatching {
        val uid = requireUserId()
        val safeDateKey = goal.dateKey.ifBlank { FocusCycleCalculator.dateKeyFor() }
        val docRef = db.collection("users").document(uid)
            .collection("study_goals").document(safeDateKey)
        val payload = mapOf(
            "userId" to uid,
            "dateKey" to safeDateKey,
            "targetMinutes" to goal.targetMinutes.coerceIn(0L, 720L),
            "loggedSeconds" to goal.loggedSeconds.coerceAtLeast(0L),
            "activeSessionStartedAtMs" to goal.activeSessionStartedAtMs.coerceAtLeast(0L),
            "cycleAnchorEpochMs" to goal.cycleAnchorEpochMs.coerceAtLeast(0L),
            "activeSubject" to goal.activeSubject.take(80),
            "blockedPackages" to goal.blockedPackages.take(30),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun recordStudySeconds(
        profile: UserProfile,
        addedSeconds: Long,
        creditDailyGoal: Boolean = true
    ): Result<Unit> = runCatching {
        if (addedSeconds <= 0L) return@runCatching
        val uid = requireUserId()
        val exam = com.example.model.ExamCatalog.getExamById(profile.selectedExamId)
        recordStudySecondsInternal(
            uid = uid,
            displayName = profile.fullName.ifBlank { "Student" }.take(80),
            examId = exam.id,
            examName = exam.name,
            addedSeconds = addedSeconds,
            creditDailyGoal = creditDailyGoal
        )
    }

    private suspend fun recordStudySecondsInternal(
        uid: String,
        displayName: String,
        examId: String,
        examName: String,
        addedSeconds: Long,
        creditDailyGoal: Boolean
    ) {
        if (addedSeconds <= 0L) return
        val nowMs = System.currentTimeMillis()
        val dayKey = FocusCycleCalculator.dateKeyFor(nowMs)
        val weekKey = FocusCycleCalculator.weekKeyFor(nowMs)
        val monthKey = FocusCycleCalculator.monthKeyFor(nowMs)

        val timeLbRef = db.collection("study_time_leaderboard").document(uid)
        try {
            val existing = timeLbRef.get().await()
            val prevDayKey = existing.getString("dayKey").orEmpty()
            val prevWeekKey = existing.getString("weekKey").orEmpty()
            val prevMonthKey = existing.getString("monthKey").orEmpty()

            val prevDaySec = if (prevDayKey == dayKey) existing.getLong("daySeconds") ?: 0L else 0L
            val prevWeekSec = if (prevWeekKey == weekKey) existing.getLong("weekSeconds") ?: 0L else 0L
            val prevMonthSec = if (prevMonthKey == monthKey) existing.getLong("monthSeconds") ?: 0L else 0L
            val prevTotalSec = existing.getLong("totalSeconds") ?: 0L

            val payload = mapOf(
                "userId" to uid,
                "displayName" to displayName.ifBlank { "Student" }.take(80),
                "examId" to examId.ifBlank { "NEET" },
                "examName" to examName.ifBlank { "NEET UG" },
                "dayKey" to dayKey,
                "daySeconds" to (prevDaySec + addedSeconds),
                "weekKey" to weekKey,
                "weekSeconds" to (prevWeekSec + addedSeconds),
                "monthKey" to monthKey,
                "monthSeconds" to (prevMonthSec + addedSeconds),
                "totalSeconds" to (prevTotalSec + addedSeconds),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            timeLbRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, timeLbRef.path)
            throw e
        }

        if (creditDailyGoal) {
            val goalRef = db.collection("users").document(uid)
                .collection("study_goals").document(dayKey)
            try {
                val goalSnap = goalRef.get().await()
                if (goalSnap.exists()) {
                    goalRef.update(
                        mapOf(
                            "loggedSeconds" to FieldValue.increment(addedSeconds),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, goalRef.path)
                throw e
            }
        }
    }

    fun observeStudyTimeLeaderboard(): Flow<List<StudyTimeLeaderboardEntry>> = flow {
        val colRef = db.collection("study_time_leaderboard")
        emitAll(
            colRef.orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudyTimeLeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, auth.currentUser?.uid)
                    throw error
                }
        )
    }

    // ---- Student Study Chat & 3-Warning 7-Day Ban Moderation ----

    fun observeChatModeration(userId: String): Flow<ChatModerationStatus?> = flow {
        val docRef = db.collection("chat_moderation").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            ChatModerationStatus::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, docRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun recordChatPolicyViolation(reason: String): Result<ChatModerationStatus> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("chat_moderation").document(uid)
        val nowMs = System.currentTimeMillis()
        val safeReason = reason.trim().ifBlank {
            "Sexual content or adult media violates Kaizu Study Chat Policy."
        }.take(300)

        try {
            val existingSnap = docRef.get().await()
            val existing = if (existingSnap.exists()) {
                existingSnap.toObject(ChatModerationStatus::class.java)
            } else null

            val currentlyBanned = existing?.isCurrentlyBanned(nowMs) == true
            val priorBanExpired = existing != null && existing.bannedUntilEpochMs > 0L && !currentlyBanned

            val nextWarnings = when {
                existing == null -> 1L
                currentlyBanned -> existing.warningCount + 1L
                priorBanExpired -> 1L
                else -> existing.warningCount + 1L
            }

            // 3 warnings allowed (1, 2, 3); 4th violation disables chat for 7 days (1 week)
            val newBannedUntilMs = when {
                currentlyBanned -> existing!!.effectiveBanEndMs()
                nextWarnings >= 4L -> nowMs + FocusCycleCalculator.SEVEN_DAYS_MILLIS
                else -> 0L
            }
            val newBannedUntilTs = Timestamp(Date(newBannedUntilMs))

            val payload = mapOf(
                "userId" to uid,
                "warningCount" to nextWarnings,
                "lastWarningReason" to safeReason,
                "bannedUntilEpochMs" to newBannedUntilMs,
                "bannedUntil" to newBannedUntilTs,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            ChatModerationStatus(
                id = uid,
                userId = uid,
                warningCount = nextWarnings,
                lastWarningReason = safeReason,
                bannedUntilEpochMs = newBannedUntilMs,
                bannedUntil = newBannedUntilTs
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeExamChatMessages(examId: String): Flow<List<StudyChatMessage>> = flow {
        val colRef = db.collection("exam_chats").document(examId).collection("messages")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(80)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudyChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).reversed()
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, auth.currentUser?.uid)
                    throw error
                }
        )
    }

    suspend fun sendExamChatMessage(
        senderName: String,
        examId: String,
        text: String,
        attachmentLabel: String = "",
        attachmentType: ChatAttachmentType = ChatAttachmentType.NONE
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("exam_chats").document(examId).collection("messages")
        val docRef = colRef.document()
        val payload = mapOf(
            "senderId" to uid,
            "senderName" to senderName.ifBlank { "Student" }.take(80),
            "examId" to examId,
            "threadId" to "",
            "participantIds" to emptyList<String>(),
            "text" to text.trim().take(1500),
            "attachmentLabel" to attachmentLabel.trim().take(200),
            "attachmentType" to attachmentType.id,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeDirectChatThreads(userId: String): Flow<List<DirectChatThread>> = flow {
        val colRef = db.collection("direct_chat_threads")
        emitAll(
            colRef.whereArrayContains("participantIds", userId)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DirectChatThread::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    fun observeDirectChatMessages(threadId: String, userId: String): Flow<List<StudyChatMessage>> = flow {
        val colRef = db.collection("direct_chats").document(threadId).collection("messages")
        emitAll(
            colRef.whereArrayContains("participantIds", userId)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudyChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun sendDirectChatMessage(
        senderName: String,
        peerId: String,
        peerName: String,
        examId: String,
        text: String,
        attachmentLabel: String = "",
        attachmentType: ChatAttachmentType = ChatAttachmentType.NONE
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(peerId.isNotBlank() && peerId != uid) { "Invalid peer recipient." }
        val threadId = buildDirectChatThreadId(uid, peerId)
        val sortedParticipants = if (uid < peerId) listOf(uid, peerId) else listOf(peerId, uid)
        val safeSenderName = senderName.ifBlank { "Student" }.take(80)
        val safePeerName = peerName.ifBlank { "Peer" }.take(80)
        val cleanText = text.trim().take(1500)
        val cleanAttachment = attachmentLabel.trim().take(200)
        val preview = cleanText.ifBlank { "[${attachmentType.id}] $cleanAttachment" }.take(300)

        val threadRef = db.collection("direct_chat_threads").document(threadId)
        val threadPayload = mapOf(
            "participantIds" to sortedParticipants,
            "participantNames" to mapOf(
                uid to safeSenderName,
                peerId to safePeerName
            ),
            "lastMessage" to preview,
            "lastSenderId" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            threadRef.set(threadPayload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, threadRef.path)
            throw e
        }

        val msgRef = db.collection("direct_chats").document(threadId).collection("messages").document()
        val msgPayload = mapOf(
            "senderId" to uid,
            "senderName" to safeSenderName,
            "examId" to examId,
            "threadId" to threadId,
            "participantIds" to sortedParticipants,
            "text" to cleanText,
            "attachmentLabel" to cleanAttachment,
            "attachmentType" to attachmentType.id,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            msgRef.set(msgPayload).await()
            msgRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            throw e
        }
    }

    private fun higherDifficulty(a: String, b: String): String {
        val rankA = QuizDifficulty.fromId(a).ordinal
        val rankB = QuizDifficulty.fromId(b).ordinal
        return if (rankB >= rankA) b else a
    }

    private fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String?
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
