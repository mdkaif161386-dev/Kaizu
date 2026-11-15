package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiQuizRepository
import com.example.data.FirestoreRepository
import com.example.model.CompetitiveExam
import com.example.model.FlashcardDeck
import com.example.model.FlashcardFocus
import com.example.model.FlashcardItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FlashcardGenRequest(
    val exam: CompetitiveExam,
    val subjectChapters: Map<String, List<String>>,
    val cardCount: Int,
    val focus: FlashcardFocus
)

sealed interface FlashcardUiState {
    data object Builder : FlashcardUiState

    data class Generating(
        val chapterSummary: String,
        val cardCount: Int,
        val examName: String
    ) : FlashcardUiState

    data class Studying(
        val deck: FlashcardDeck,
        val currentIndex: Int = 0,
        val isFlipped: Boolean = false,
        val filterUnmasteredOnly: Boolean = false,
        val syncError: String? = null
    ) : FlashcardUiState {
        val visibleCards: List<FlashcardItem>
            get() = if (filterUnmasteredOnly) {
                deck.cards.filter { !it.mastered }
            } else {
                deck.cards
            }
    }

    data class Error(
        val message: String,
        val lastRequest: FlashcardGenRequest? = null
    ) : FlashcardUiState
}

class FlashcardViewModel(
    private val aiRepo: AiQuizRepository,
    private val firestoreRepo: FirestoreRepository,
    private val currentUserId: String
) : ViewModel() {

    val savedDecks: StateFlow<List<FlashcardDeck>> = firestoreRepo.observeFlashcardDecks(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing flashcard decks", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow<FlashcardUiState>(FlashcardUiState.Builder)
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    fun generateDeck(request: FlashcardGenRequest) {
        generationJob?.cancel()
        val chapters = request.subjectChapters.values.flatten().distinct()
        val summary = chapters.joinToString(", ").ifBlank { request.exam.name }

        generationJob = viewModelScope.launch {
            _uiState.value = FlashcardUiState.Generating(
                chapterSummary = summary,
                cardCount = request.cardCount,
                examName = request.exam.name
            )
            try {
                val generated = aiRepo.generateFlashcards(
                    userId = currentUserId,
                    exam = request.exam,
                    subjectChapters = request.subjectChapters,
                    cardCount = request.cardCount,
                    focus = request.focus
                )
                val saveRes = firestoreRepo.saveFlashcardDeck(generated)
                val finalDeck = saveRes.getOrNull()?.let { id ->
                    generated.copy(id = id)
                } ?: generated
                val syncErr = saveRes.exceptionOrNull()?.localizedMessage

                _uiState.value = FlashcardUiState.Studying(
                    deck = finalDeck,
                    currentIndex = 0,
                    isFlipped = false,
                    filterUnmasteredOnly = false,
                    syncError = syncErr
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate flashcards", e)
                _uiState.value = FlashcardUiState.Error(
                    message = e.localizedMessage ?: "Failed to generate AI flashcards.",
                    lastRequest = request
                )
            }
        }
    }

    fun openSavedDeck(deck: FlashcardDeck) {
        generationJob?.cancel()
        _uiState.value = FlashcardUiState.Studying(
            deck = deck,
            currentIndex = 0,
            isFlipped = false,
            filterUnmasteredOnly = false
        )
    }

    fun flipCard() {
        val current = _uiState.value as? FlashcardUiState.Studying ?: return
        _uiState.value = current.copy(isFlipped = !current.isFlipped)
    }

    fun nextCard() {
        val current = _uiState.value as? FlashcardUiState.Studying ?: return
        val total = current.visibleCards.size
        if (total == 0) return
        val next = (current.currentIndex + 1).coerceAtMost(total - 1)
        _uiState.value = current.copy(currentIndex = next, isFlipped = false)
    }

    fun previousCard() {
        val current = _uiState.value as? FlashcardUiState.Studying ?: return
        if (current.visibleCards.isEmpty()) return
        val prev = (current.currentIndex - 1).coerceAtLeast(0)
        _uiState.value = current.copy(currentIndex = prev, isFlipped = false)
    }

    fun markCardMastered(cardId: String, mastered: Boolean) {
        val current = _uiState.value as? FlashcardUiState.Studying ?: return
        val updatedCards = current.deck.cards.map { item ->
            if (item.id == cardId) item.copy(mastered = mastered) else item
        }
        val updatedDeck = current.deck.copy(
            cards = updatedCards,
            masteredCount = updatedCards.count { it.mastered }.toLong()
        )
        val newVisibleSize = if (current.filterUnmasteredOnly) {
            updatedCards.count { !it.mastered }
        } else {
            updatedCards.size
        }
        val targetIndex = when {
            newVisibleSize == 0 -> 0
            current.filterUnmasteredOnly -> current.currentIndex.coerceIn(0, newVisibleSize - 1)
            current.currentIndex < newVisibleSize - 1 -> current.currentIndex + 1
            else -> current.currentIndex
        }

        _uiState.value = current.copy(
            deck = updatedDeck,
            currentIndex = targetIndex,
            isFlipped = false,
            syncError = null
        )

        val deckId = updatedDeck.id
        if (deckId.isNotBlank()) {
            viewModelScope.launch {
                val res = firestoreRepo.updateFlashcardMastery(deckId, updatedCards)
                res.onFailure { err ->
                    Log.w(TAG, "Failed to sync flashcard mastery", err)
                    val latest = _uiState.value as? FlashcardUiState.Studying
                    if (latest != null && latest.deck.id == deckId) {
                        _uiState.value = latest.copy(
                            syncError = err.localizedMessage ?: "Progress updated locally; cloud sync failed."
                        )
                    }
                }
            }
        }
    }

    fun shuffleDeck() {
        val current = _uiState.value as? FlashcardUiState.Studying ?: return
        val shuffled = current.deck.copy(cards = current.deck.cards.shuffled())
        _uiState.value = current.copy(
            deck = shuffled,
            currentIndex = 0,
            isFlipped = false
        )
    }

    fun toggleUnmasteredFilter() {
        val current = _uiState.value as? FlashcardUiState.Studying ?: return
        _uiState.value = current.copy(
            filterUnmasteredOnly = !current.filterUnmasteredOnly,
            currentIndex = 0,
            isFlipped = false
        )
    }

    fun resetMasteryInDeck() {
        val current = _uiState.value as? FlashcardUiState.Studying ?: return
        val resetCards = current.deck.cards.map { it.copy(mastered = false) }
        val resetDeck = current.deck.copy(cards = resetCards, masteredCount = 0L)
        _uiState.value = current.copy(
            deck = resetDeck,
            currentIndex = 0,
            isFlipped = false,
            filterUnmasteredOnly = false
        )
        if (resetDeck.id.isNotBlank()) {
            viewModelScope.launch {
                firestoreRepo.updateFlashcardMastery(resetDeck.id, resetCards)
            }
        }
    }

    fun deleteSavedDeck(deckId: String) {
        if (deckId.isBlank()) return
        viewModelScope.launch {
            firestoreRepo.deleteFlashcardDeck(deckId)
        }
    }

    fun backToBuilder() {
        generationJob?.cancel()
        _uiState.value = FlashcardUiState.Builder
    }

    override fun onCleared() {
        super.onCleared()
        generationJob?.cancel()
    }

    private companion object {
        const val TAG = "FlashcardViewModel"
    }
}
