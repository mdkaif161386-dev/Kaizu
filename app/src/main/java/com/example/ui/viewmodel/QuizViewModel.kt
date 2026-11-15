package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiQuizRepository
import com.example.data.FirestoreRepository
import com.example.model.GeneratedQuestion
import com.example.model.QuestionReview
import com.example.model.QuizAttempt
import com.example.model.QuizConfig
import com.example.model.QuizDifficulty
import com.example.model.QuizMode
import com.example.model.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.roundToLong

sealed interface QuizSessionState {
    data object Idle : QuizSessionState
    data class Generating(
        val config: QuizConfig,
        val completedBatches: Int,
        val totalBatches: Int
    ) : QuizSessionState

    data class Playing(
        val config: QuizConfig,
        val questions: List<GeneratedQuestion>,
        val currentIndex: Int,
        val selectedAnswers: Map<Int, Int>,
        val remainingSecForQuestion: Int,
        val totalElapsedSec: Int
    ) : QuizSessionState

    data class Submitting(val attempt: QuizAttempt) : QuizSessionState

    data class Completed(
        val attempt: QuizAttempt,
        val savedAttemptId: String?,
        val saveError: String? = null
    ) : QuizSessionState

    data class Error(val config: QuizConfig, val message: String) : QuizSessionState
}

class QuizViewModel(
    private val aiRepo: AiQuizRepository,
    private val firestoreRepo: FirestoreRepository
) : ViewModel() {

    private val _sessionState = MutableStateFlow<QuizSessionState>(QuizSessionState.Idle)
    val sessionState: StateFlow<QuizSessionState> = _sessionState.asStateFlow()

    private var timerJob: Job? = null
    private var generationJob: Job? = null
    private var activeProfile: UserProfile? = null

    fun startQuizGeneration(config: QuizConfig, profile: UserProfile) {
        activeProfile = profile
        timerJob?.cancel()
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            _sessionState.value = QuizSessionState.Generating(
                config = config,
                completedBatches = 0,
                totalBatches = ((config.questionCount + 9) / 10).coerceAtLeast(1)
            )
            try {
                val questions = aiRepo.generateQuiz(config) { done, total ->
                    val current = _sessionState.value
                    if (current is QuizSessionState.Generating) {
                        _sessionState.value = current.copy(
                            completedBatches = done,
                            totalBatches = total
                        )
                    }
                }
                _sessionState.value = QuizSessionState.Playing(
                    config = config,
                    questions = questions,
                    currentIndex = 0,
                    selectedAnswers = emptyMap(),
                    remainingSecForQuestion = config.timePerQuestionSec,
                    totalElapsedSec = 0
                )
                startQuestionTimer()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Quiz generation failed", e)
                _sessionState.value = QuizSessionState.Error(
                    config = config,
                    message = e.localizedMessage ?: "Failed to generate AI quiz."
                )
            }
        }
    }

    private fun startQuestionTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _sessionState.value
                if (current !is QuizSessionState.Playing) break

                val nextRemaining = current.remainingSecForQuestion - 1
                val nextElapsed = current.totalElapsedSec + 1

                if (nextRemaining <= 0) {
                    // Time expired for current question: auto-advance or finish
                    if (current.currentIndex < current.questions.lastIndex) {
                        _sessionState.value = current.copy(
                            currentIndex = current.currentIndex + 1,
                            remainingSecForQuestion = current.config.timePerQuestionSec,
                            totalElapsedSec = nextElapsed
                        )
                    } else {
                        _sessionState.value = current.copy(totalElapsedSec = nextElapsed)
                        finishQuiz()
                        break
                    }
                } else {
                    _sessionState.value = current.copy(
                        remainingSecForQuestion = nextRemaining,
                        totalElapsedSec = nextElapsed
                    )
                }
            }
        }
    }

    fun selectOption(optionIndex: Int) {
        val current = _sessionState.value as? QuizSessionState.Playing ?: return
        val updated = current.selectedAnswers.toMutableMap()
        updated[current.currentIndex] = optionIndex
        _sessionState.value = current.copy(selectedAnswers = updated)
    }

    /**
     * Immediately advances to the next question before the timer expires,
     * or submits the quiz if on the last question.
     */
    fun advanceToNextQuestion() {
        val current = _sessionState.value as? QuizSessionState.Playing ?: return
        if (current.currentIndex < current.questions.lastIndex) {
            _sessionState.value = current.copy(
                currentIndex = current.currentIndex + 1,
                remainingSecForQuestion = current.config.timePerQuestionSec
            )
        } else {
            finishQuiz()
        }
    }

    fun finishQuizEarly() {
        finishQuiz()
    }

    private fun finishQuiz() {
        timerJob?.cancel()
        val playing = _sessionState.value as? QuizSessionState.Playing ?: return
        val profile = activeProfile ?: return

        val reviews = playing.questions.mapIndexed { idx, q ->
            val selected = playing.selectedAnswers[idx] ?: -1
            val isCorrect = selected == q.correctIndex
            QuestionReview(
                question = q.question,
                options = q.options,
                correctIndex = q.correctIndex,
                selectedIndex = selected,
                explanation = q.explanation,
                subject = q.subject,
                chapter = q.chapter,
                isCorrect = isCorrect
            )
        }

        val total = reviews.size
        val correct = reviews.count { it.isCorrect }
        val skipped = reviews.count { it.selectedIndex == -1 }
        val wrong = total - correct - skipped
        val accuracy = if (total > 0) (correct.toDouble() / total.toDouble()) * 100.0 else 0.0

        val basePoints = correct * 40.0
        val modeBonus = if (playing.config.mode == QuizMode.MEGA) 1.4 else 1.0
        val diffMultiplier = playing.config.difficulty.scoreMultiplier
        val maxAllowedSec = total * playing.config.timePerQuestionSec
        val speedBonus = if (playing.totalElapsedSec < maxAllowedSec && correct > 0) {
            ((maxAllowedSec - playing.totalElapsedSec).toDouble() / maxAllowedSec.toDouble()) * 30.0 * correct
        } else 0.0
        val totalScorePoints = ((basePoints * diffMultiplier * modeBonus) + speedBonus)
            .roundToLong()
            .coerceAtLeast(if (correct > 0) 10L else 0L)

        val chapterSummary = playing.config.subjectChapters.values
            .flatten()
            .distinct()
            .joinToString(", ")
            .take(280)
            .ifBlank { "Mixed Chapters" }

        val attempt = QuizAttempt(
            userId = profile.userId,
            examId = playing.config.exam.id,
            examName = playing.config.exam.name,
            mode = playing.config.mode.id,
            subjectNames = playing.config.subjectChapters.keys.toList(),
            chapterSummary = chapterSummary,
            difficulty = playing.config.difficulty.id,
            totalQuestions = total.toLong(),
            correctCount = correct.toLong(),
            wrongCount = wrong.toLong(),
            skippedCount = skipped.toLong(),
            scorePoints = totalScorePoints,
            accuracyPercent = ((accuracy * 10.0).roundToInt() / 10.0),
            timePerQuestionSec = playing.config.timePerQuestionSec.toLong(),
            timeTakenSec = playing.totalElapsedSec.toLong(),
            questionReviews = reviews
        )

        _sessionState.value = QuizSessionState.Submitting(attempt)
        viewModelScope.launch {
            val res = firestoreRepo.submitQuizAttempt(profile, attempt)
            res.onSuccess { attemptId ->
                _sessionState.value = QuizSessionState.Completed(
                    attempt = attempt,
                    savedAttemptId = attemptId
                )
            }.onFailure { err ->
                Log.e(TAG, "Failed to save quiz attempt", err)
                _sessionState.value = QuizSessionState.Completed(
                    attempt = attempt,
                    savedAttemptId = null,
                    saveError = err.localizedMessage ?: "Score computed, but cloud sync failed."
                )
            }
        }
    }

    fun viewSavedAttempt(attempt: QuizAttempt) {
        timerJob?.cancel()
        generationJob?.cancel()
        _sessionState.value = QuizSessionState.Completed(
            attempt = attempt,
            savedAttemptId = attempt.id
        )
    }

    fun resetSession() {
        timerJob?.cancel()
        generationJob?.cancel()
        _sessionState.value = QuizSessionState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        generationJob?.cancel()
    }

    private companion object {
        const val TAG = "QuizViewModel"
    }
}
