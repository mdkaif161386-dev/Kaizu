package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreRepository
import com.example.model.LeaderboardEntry
import com.example.model.MegaLeaderboardEntry
import com.example.model.StudyTimeLeaderboardEntry
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface LeaderboardUiState<out T> {
    data object Loading : LeaderboardUiState<Nothing>
    data class Success<T>(val entries: List<T>) : LeaderboardUiState<T>
    data class Error(val message: String) : LeaderboardUiState<Nothing>
}

class LeaderboardViewModel(
    private val firestoreRepo: FirestoreRepository,
    initialExamId: String
) : ViewModel() {

    private val _selectedExamId = MutableStateFlow(initialExamId)
    val selectedExamId: StateFlow<String> = _selectedExamId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val examLeaderboard: StateFlow<LeaderboardUiState<LeaderboardEntry>> =
        _selectedExamId.flatMapLatest { examId ->
            firestoreRepo.observeExamLeaderboard(examId)
                .map<List<LeaderboardEntry>, LeaderboardUiState<LeaderboardEntry>> {
                    LeaderboardUiState.Success(it)
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val root = fsErr ?: error
                    Log.w(TAG, "Error observing exam leaderboard", root)
                    emit(LeaderboardUiState.Error(root.localizedMessage ?: "Failed to load leaderboard"))
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = LeaderboardUiState.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val megaLeaderboard: StateFlow<LeaderboardUiState<MegaLeaderboardEntry>> =
        _selectedExamId.flatMapLatest { examId ->
            firestoreRepo.observeMegaLeaderboard(examId)
                .map<List<MegaLeaderboardEntry>, LeaderboardUiState<MegaLeaderboardEntry>> {
                    LeaderboardUiState.Success(it)
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val root = fsErr ?: error
                    Log.w(TAG, "Error observing mega leaderboard", root)
                    emit(LeaderboardUiState.Error(root.localizedMessage ?: "Failed to load Mega leaderboard"))
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = LeaderboardUiState.Loading
        )

    val studyTimeLeaderboard: StateFlow<LeaderboardUiState<StudyTimeLeaderboardEntry>> =
        firestoreRepo.observeStudyTimeLeaderboard()
            .map<List<StudyTimeLeaderboardEntry>, LeaderboardUiState<StudyTimeLeaderboardEntry>> {
                LeaderboardUiState.Success(it)
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                val root = fsErr ?: error
                Log.w(TAG, "Error observing study time leaderboard", root)
                emit(LeaderboardUiState.Error(root.localizedMessage ?: "Failed to load Study Time leaderboard"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = LeaderboardUiState.Loading
            )

    fun selectExam(examId: String) {
        _selectedExamId.value = examId
    }

    private companion object {
        const val TAG = "LeaderboardVM"
    }
}
