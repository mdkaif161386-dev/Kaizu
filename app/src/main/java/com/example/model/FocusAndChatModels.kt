package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class StudyGoalRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val targetMinutes: Long = 0L,
    val loggedSeconds: Long = 0L,
    val activeSessionStartedAtMs: Long = 0L,
    val cycleAnchorEpochMs: Long = 0L,
    val activeSubject: String = "",
    val blockedPackages: List<String> = emptyList(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudyTimeLeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val examId: String = "",
    val examName: String = "",
    val dayKey: String = "",
    val daySeconds: Long = 0L,
    val weekKey: String = "",
    val weekSeconds: Long = 0L,
    val monthKey: String = "",
    val monthSeconds: Long = 0L,
    val totalSeconds: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class StudyTimePeriod(val label: String, val badgeText: String) {
    ONE_DAY("1 Day", "Today"),
    ONE_WEEK("1 Week", "This Week"),
    ONE_MONTH("1 Month", "This Month")
}

enum class ChatAttachmentType(val id: String) {
    NONE("NONE"),
    IMAGE("IMAGE"),
    VIDEO("VIDEO"),
    LINK("LINK");

    companion object {
        fun fromId(id: String): ChatAttachmentType = entries.firstOrNull { it.id == id } ?: NONE
    }
}

@IgnoreExtraProperties
data class StudyChatMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val examId: String = "",
    val threadId: String = "",
    val participantIds: List<String> = emptyList(),
    val text: String = "",
    val attachmentLabel: String = "",
    val attachmentType: String = ChatAttachmentType.NONE.id,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DirectChatThread(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatModerationStatus(
    @DocumentId val id: String = "",
    val userId: String = "",
    val warningCount: Long = 0L,
    val lastWarningReason: String = "",
    val bannedUntilEpochMs: Long = 0L,
    val bannedUntil: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isCurrentlyBanned(nowMs: Long = System.currentTimeMillis()): Boolean {
        val tsMs = bannedUntil?.toDate()?.time ?: 0L
        return maxOf(bannedUntilEpochMs, tsMs) > nowMs
    }

    fun effectiveBanEndMs(): Long {
        val tsMs = bannedUntil?.toDate()?.time ?: 0L
        return maxOf(bannedUntilEpochMs, tsMs)
    }
}

fun buildDirectChatThreadId(uidA: String, uidB: String): String {
    return if (uidA <= uidB) "${uidA}__${uidB}" else "${uidB}__${uidA}"
}
