package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiQuizRepository
import com.example.data.FirestoreRepository
import com.example.model.CompetitiveExam
import com.example.model.ExamCatalog
import com.example.model.QuizAttempt
import com.example.model.SavedQuestion
import com.example.model.StudyInsight
import com.example.model.UserProfile
import com.example.model.savedQuestionDocId
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data object NeedsOnboarding : ProfileUiState
    data class Ready(val profile: UserProfile) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

sealed interface InsightGenerationState {
    data object Idle : InsightGenerationState
    data object Loading : InsightGenerationState
    data class Error(val message: String) : InsightGenerationState
}

class AppViewModel(
    private val firestoreRepo: FirestoreRepository,
    private val aiRepo: AiQuizRepository,
    private val currentUserId: String
) : ViewModel() {

    val profileState: StateFlow<ProfileUiState> = firestoreRepo.observeUserProfile(currentUserId)
        .map<UserProfile?, ProfileUiState> { profile ->
            if (profile == null || profile.fullName.isBlank() || profile.selectedExamId.isBlank()) {
                ProfileUiState.NeedsOnboarding
            } else {
                ProfileUiState.Ready(profile)
            }
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(ProfileUiState.Error(rootError.localizedMessage ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ProfileUiState.Loading
        )

    val quizAttempts: StateFlow<List<QuizAttempt>> = firestoreRepo.observeQuizAttempts(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing attempts", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val studyInsight: StateFlow<StudyInsight?> = firestoreRepo.observeStudyInsight(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing study insight", error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    val savedQuestions: StateFlow<List<SavedQuestion>> = firestoreRepo.observeSavedQuestions(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing saved questions", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _onboardingSaving = MutableStateFlow(false)
    val onboardingSaving: StateFlow<Boolean> = _onboardingSaving.asStateFlow()

    private val _onboardingError = MutableStateFlow<String?>(null)
    val onboardingError: StateFlow<String?> = _onboardingError.asStateFlow()

    private val _insightGenState = MutableStateFlow<InsightGenerationState>(InsightGenerationState.Idle)
    val insightGenState: StateFlow<InsightGenerationState> = _insightGenState.asStateFlow()

    fun saveProfile(
        fullName: String,
        dob: String,
        phoneNumber: String,
        selectedExamId: String,
        isNewProfile: Boolean,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _onboardingSaving.value = true
            _onboardingError.value = null
            val res = firestoreRepo.saveUserProfile(
                fullName = fullName,
                dob = dob,
                phoneNumber = phoneNumber,
                selectedExamId = selectedExamId,
                isNewProfile = isNewProfile
            )
            _onboardingSaving.value = false
            res.onSuccess {
                onComplete()
            }.onFailure { err ->
                _onboardingError.value = err.localizedMessage ?: "Failed to save profile."
            }
        }
    }

    fun switchExam(exam: CompetitiveExam) {
        viewModelScope.launch {
            firestoreRepo.updateSelectedExam(exam.id)
        }
    }

    fun generatePersonalizedStudyPlan(profile: UserProfile, attempts: List<QuizAttempt>) {
        if (attempts.isEmpty()) return
        viewModelScope.launch {
            _insightGenState.value = InsightGenerationState.Loading
            try {
                val exam = ExamCatalog.getExamById(profile.selectedExamId)
                val insight = aiRepo.generateStudyInsights(
                    userId = currentUserId,
                    examName = exam.name,
                    examId = exam.id,
                    recentAttempts = attempts
                )
                firestoreRepo.saveStudyInsight(insight).getOrThrow()
                _insightGenState.value = InsightGenerationState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate personalized study plan", e)
                _insightGenState.value = InsightGenerationState.Error(
                    e.localizedMessage ?: "Could not generate AI study plan."
                )
            }
        }
    }

    fun clearOnboardingError() {
        _onboardingError.value = null
    }

    fun toggleQuestionBookmark(
        examId: String,
        examName: String,
        subject: String,
        chapter: String,
        difficulty: String,
        question: String,
        options: List<String>,
        correctIndex: Int,
        explanation: String
    ) {
        val docId = savedQuestionDocId(question, subject, chapter)
        val alreadySaved = savedQuestions.value.any { it.id == docId }
        viewModelScope.launch {
            if (alreadySaved) {
                firestoreRepo.removeQuestionBookmark(docId)
            } else {
                firestoreRepo.saveQuestionBookmark(
                    SavedQuestion(
                        id = docId,
                        userId = currentUserId,
                        examId = examId,
                        examName = examName,
                        subject = subject,
                        chapter = chapter,
                        difficulty = difficulty,
                        question = question,
                        options = options,
                        correctIndex = correctIndex,
                        explanation = explanation
                    )
                )
            }
        }
    }

    fun removeSavedQuestion(docId: String) {
        if (docId.isBlank()) return
        viewModelScope.launch {
            firestoreRepo.removeQuestionBookmark(docId)
        }
    }

    private companion object {
        const val TAG = "AppViewModel"
    }
}
