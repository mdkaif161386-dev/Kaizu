package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiQuizRepository
import com.example.data.FirestoreRepository
import com.example.model.ChatAttachmentType
import com.example.model.ChatModerationStatus
import com.example.model.DirectChatThread
import com.example.model.StudyChatMessage
import com.example.model.buildDirectChatThreadId
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatPeerCandidate(
    val userId: String,
    val displayName: String,
    val examName: String
)

sealed interface ChatSendOutcome {
    data object Idle : ChatSendOutcome
    data object Checking : ChatSendOutcome
    data class WarningIssued(
        val warningNumber: Int,
        val maxWarnings: Int = 3,
        val reason: String
    ) : ChatSendOutcome
    data class BannedForWeek(
        val bannedUntilEpochMs: Long,
        val reason: String
    ) : ChatSendOutcome
    data class Error(val message: String) : ChatSendOutcome
}

class ChatViewModel(
    private val firestoreRepo: FirestoreRepository,
    private val aiRepo: AiQuizRepository,
    private val currentUserId: String,
    initialExamId: String
) : ViewModel() {

    private val _selectedExamId = MutableStateFlow(initialExamId)
    val selectedExamId: StateFlow<String> = _selectedExamId.asStateFlow()

    private val _activePeer = MutableStateFlow<ChatPeerCandidate?>(null)
    val activePeer: StateFlow<ChatPeerCandidate?> = _activePeer.asStateFlow()

    private val _sendOutcome = MutableStateFlow<ChatSendOutcome>(ChatSendOutcome.Idle)
    val sendOutcome: StateFlow<ChatSendOutcome> = _sendOutcome.asStateFlow()

    val moderationStatus: StateFlow<ChatModerationStatus?> =
        firestoreRepo.observeChatModeration(currentUserId)
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing chat moderation", fsErr ?: error)
                emit(null)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = null
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val examMessages: StateFlow<List<StudyChatMessage>> =
        _selectedExamId.flatMapLatest { examId ->
            firestoreRepo.observeExamChatMessages(examId)
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing exam chat", fsErr ?: error)
                    emit(emptyList())
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val directThreads: StateFlow<List<DirectChatThread>> =
        firestoreRepo.observeDirectChatThreads(currentUserId)
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing DM threads", fsErr ?: error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val directMessages: StateFlow<List<StudyChatMessage>> =
        _activePeer.flatMapLatest { peer ->
            if (peer == null) {
                flowOf(emptyList())
            } else {
                val threadId = buildDirectChatThreadId(currentUserId, peer.userId)
                firestoreRepo.observeDirectChatMessages(threadId, currentUserId)
                    .catch { error ->
                        val fsErr = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (fsErr == null && error is CancellationException) throw error
                        Log.w(TAG, "Error observing DM messages", fsErr ?: error)
                        emit(emptyList())
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun selectExamRoom(examId: String) {
        _selectedExamId.value = examId
    }

    fun openPeerChat(peer: ChatPeerCandidate) {
        if (peer.userId == currentUserId) return
        _activePeer.value = peer
    }

    fun closePeerChat() {
        _activePeer.value = null
    }

    fun clearSendOutcome() {
        _sendOutcome.value = ChatSendOutcome.Idle
    }

    fun sendMessage(
        senderName: String,
        examId: String,
        examName: String,
        text: String,
        attachmentLabel: String = "",
        attachmentType: ChatAttachmentType = ChatAttachmentType.NONE,
        imageBitmap: Bitmap? = null,
        onSentSuccess: () -> Unit = {}
    ) {
        val currentMod = moderationStatus.value
        val nowMs = System.currentTimeMillis()
        if (currentMod?.isCurrentlyBanned(nowMs) == true) {
            _sendOutcome.value = ChatSendOutcome.BannedForWeek(
                bannedUntilEpochMs = currentMod.effectiveBanEndMs(),
                reason = currentMod.lastWarningReason.ifBlank {
                    "Your chats are disabled for 1 week due to repeated sexual content policy violations."
                }
            )
            return
        }

        if (text.isBlank() && attachmentLabel.isBlank()) return

        viewModelScope.launch {
            _sendOutcome.value = ChatSendOutcome.Checking
            val check = aiRepo.moderateStudyChatMessage(
                text = text,
                attachmentLabel = attachmentLabel,
                attachmentType = attachmentType.id,
                examName = examName,
                imageBitmap = imageBitmap
            )

            if (check.isViolation) {
                val modRes = firestoreRepo.recordChatPolicyViolation(check.reason)
                modRes.onSuccess { updated ->
                    if (updated.isCurrentlyBanned()) {
                        _sendOutcome.value = ChatSendOutcome.BannedForWeek(
                            bannedUntilEpochMs = updated.effectiveBanEndMs(),
                            reason = updated.lastWarningReason
                        )
                    } else {
                        _sendOutcome.value = ChatSendOutcome.WarningIssued(
                            warningNumber = updated.warningCount.toInt().coerceIn(1, 3),
                            maxWarnings = 3,
                            reason = updated.lastWarningReason
                        )
                    }
                }.onFailure { err ->
                    _sendOutcome.value = ChatSendOutcome.Error(
                        err.localizedMessage ?: "Message blocked by safety policy."
                    )
                }
                return@launch
            }

            val peer = _activePeer.value
            val sendRes = if (peer == null) {
                firestoreRepo.sendExamChatMessage(
                    senderName = senderName,
                    examId = examId,
                    text = text,
                    attachmentLabel = attachmentLabel,
                    attachmentType = attachmentType
                )
            } else {
                firestoreRepo.sendDirectChatMessage(
                    senderName = senderName,
                    peerId = peer.userId,
                    peerName = peer.displayName,
                    examId = examId,
                    text = text,
                    attachmentLabel = attachmentLabel,
                    attachmentType = attachmentType
                )
            }

            sendRes.onSuccess {
                _sendOutcome.value = ChatSendOutcome.Idle
                onSentSuccess()
            }.onFailure { err ->
                _sendOutcome.value = ChatSendOutcome.Error(
                    err.localizedMessage ?: "Could not send message. Please try again."
                )
            }
        }
    }

    private companion object {
        const val TAG = "ChatViewModel"
    }
}
