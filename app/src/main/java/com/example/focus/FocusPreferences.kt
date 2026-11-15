package com.example.focus

import android.content.Context
import com.example.model.StudyGoalRecord
import com.example.util.FocusCycleCalculator
import com.example.util.SocialMediaCatalog

data class LocalFocusSnapshot(
    val userId: String,
    val dateKey: String,
    val targetMinutes: Long,
    val loggedSeconds: Long,
    val activeSessionStartedAtMs: Long,
    val cycleAnchorEpochMs: Long,
    val activeSubject: String,
    val blockedPackages: Set<String>
)

class FocusPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(todayKey: String = FocusCycleCalculator.dateKeyFor()): LocalFocusSnapshot {
        val storedDate = prefs.getString(KEY_DATE_KEY, todayKey).orEmpty().ifBlank { todayKey }
        val userId = prefs.getString(KEY_USER_ID, "").orEmpty()
        val blocked = prefs.getStringSet(KEY_BLOCKED_PACKAGES, SocialMediaCatalog.defaultBlockedPackages)
            ?: SocialMediaCatalog.defaultBlockedPackages

        // Reset daily goal counters automatically when calendar day rolls over
        if (storedDate != todayKey) {
            return LocalFocusSnapshot(
                userId = userId,
                dateKey = todayKey,
                targetMinutes = 0L,
                loggedSeconds = 0L,
                activeSessionStartedAtMs = 0L,
                cycleAnchorEpochMs = 0L,
                activeSubject = "",
                blockedPackages = blocked
            )
        }

        return LocalFocusSnapshot(
            userId = userId,
            dateKey = storedDate,
            targetMinutes = prefs.getLong(KEY_TARGET_MINUTES, 0L),
            loggedSeconds = prefs.getLong(KEY_LOGGED_SECONDS, 0L),
            activeSessionStartedAtMs = prefs.getLong(KEY_ACTIVE_SESSION_START_MS, 0L),
            cycleAnchorEpochMs = prefs.getLong(KEY_CYCLE_ANCHOR_MS, 0L),
            activeSubject = prefs.getString(KEY_ACTIVE_SUBJECT, "").orEmpty(),
            blockedPackages = blocked
        )
    }

    fun write(snapshot: LocalFocusSnapshot) {
        prefs.edit()
            .putString(KEY_USER_ID, snapshot.userId)
            .putString(KEY_DATE_KEY, snapshot.dateKey)
            .putLong(KEY_TARGET_MINUTES, snapshot.targetMinutes)
            .putLong(KEY_LOGGED_SECONDS, snapshot.loggedSeconds)
            .putLong(KEY_ACTIVE_SESSION_START_MS, snapshot.activeSessionStartedAtMs)
            .putLong(KEY_CYCLE_ANCHOR_MS, snapshot.cycleAnchorEpochMs)
            .putString(KEY_ACTIVE_SUBJECT, snapshot.activeSubject)
            .putStringSet(KEY_BLOCKED_PACKAGES, snapshot.blockedPackages)
            .apply()
    }

    fun syncFromRemote(record: StudyGoalRecord, currentDateKey: String = FocusCycleCalculator.dateKeyFor()): LocalFocusSnapshot {
        val blocked = if (record.blockedPackages.isEmpty()) {
            SocialMediaCatalog.defaultBlockedPackages
        } else {
            record.blockedPackages.toSet()
        }
        val snap = LocalFocusSnapshot(
            userId = record.userId,
            dateKey = record.dateKey.ifBlank { currentDateKey },
            targetMinutes = record.targetMinutes,
            loggedSeconds = record.loggedSeconds,
            activeSessionStartedAtMs = record.activeSessionStartedAtMs,
            cycleAnchorEpochMs = record.cycleAnchorEpochMs,
            activeSubject = record.activeSubject,
            blockedPackages = blocked
        )
        write(snap)
        return snap
    }

    fun clearForSignOut() {
        prefs.edit()
            .putString(KEY_USER_ID, "")
            .putLong(KEY_TARGET_MINUTES, 0L)
            .putLong(KEY_LOGGED_SECONDS, 0L)
            .putLong(KEY_ACTIVE_SESSION_START_MS, 0L)
            .putLong(KEY_CYCLE_ANCHOR_MS, 0L)
            .putString(KEY_ACTIVE_SUBJECT, "")
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "kaizu_focus_prefs"
        const val KEY_USER_ID = "user_id"
        const val KEY_DATE_KEY = "date_key"
        const val KEY_TARGET_MINUTES = "target_minutes"
        const val KEY_LOGGED_SECONDS = "logged_seconds"
        const val KEY_ACTIVE_SESSION_START_MS = "active_session_start_ms"
        const val KEY_CYCLE_ANCHOR_MS = "cycle_anchor_ms"
        const val KEY_ACTIVE_SUBJECT = "active_subject"
        const val KEY_BLOCKED_PACKAGES = "blocked_packages"
    }
}
