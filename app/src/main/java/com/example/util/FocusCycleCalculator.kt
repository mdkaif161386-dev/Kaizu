package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class FocusPhase {
    NO_GOAL,
    SOCIAL_LOCKED,
    FIVE_MIN_BREAK,
    GOAL_COMPLETED
}

data class FocusRuntimeStatus(
    val hasActiveGoal: Boolean,
    val targetGoalSec: Long,
    val loggedSecondsToday: Long,
    val liveSessionElapsedSec: Long,
    val totalStudiedTodaySec: Long,
    val remainingGoalSec: Long,
    val phase: FocusPhase,
    val phaseRemainingSec: Long,
    val currentHourNumber: Int,
    val progressFraction: Float,
    val isPhysicalStudyRunning: Boolean,
    val isSocialMediaDisabled: Boolean
) {
    val isGoalCompleted: Boolean
        get() = phase == FocusPhase.GOAL_COMPLETED
}

object FocusCycleCalculator {
    const val CYCLE_DURATION_SECONDS: Long = 3600L // 60 minutes loop
    const val LOCK_DURATION_SECONDS: Long = 3300L  // 55 minutes social apps disabled
    const val BREAK_DURATION_SECONDS: Long = 300L  // 5 minutes social apps enabled per hour
    const val SEVEN_DAYS_MILLIS: Long = 7L * 24L * 60L * 60L * 1000L

    private val defaultDateFmt = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
    private val defaultMonthFmt = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM", Locale.US)
    }
    private val defaultWeekCal = ThreadLocal.withInitial {
        Calendar.getInstance(Locale.US).apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
        }
    }

    fun dateKeyFor(epochMs: Long = System.currentTimeMillis(), timeZone: TimeZone = TimeZone.getDefault()): String {
        val fmt = if (timeZone == TimeZone.getDefault()) {
            defaultDateFmt.get()!!.apply { this.timeZone = timeZone }
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { this.timeZone = timeZone }
        }
        return fmt.format(Date(epochMs))
    }

    fun weekKeyFor(epochMs: Long = System.currentTimeMillis(), timeZone: TimeZone = TimeZone.getDefault()): String {
        val cal = if (timeZone == TimeZone.getDefault()) {
            defaultWeekCal.get()!!.apply {
                this.timeZone = timeZone
                timeInMillis = epochMs
            }
        } else {
            Calendar.getInstance(timeZone, Locale.US).apply {
                firstDayOfWeek = Calendar.MONDAY
                minimalDaysInFirstWeek = 4
                timeInMillis = epochMs
            }
        }
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return String.format(Locale.US, "%04d-W%02d", year, week)
    }

    fun monthKeyFor(epochMs: Long = System.currentTimeMillis(), timeZone: TimeZone = TimeZone.getDefault()): String {
        val fmt = if (timeZone == TimeZone.getDefault()) {
            defaultMonthFmt.get()!!.apply { this.timeZone = timeZone }
        } else {
            SimpleDateFormat("yyyy-MM", Locale.US).apply { this.timeZone = timeZone }
        }
        return fmt.format(Date(epochMs))
    }

    fun evaluate(
        targetMinutes: Long,
        loggedSecondsToday: Long,
        activeSessionStartedAtMs: Long,
        cycleAnchorEpochMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): FocusRuntimeStatus {
        val targetGoalSec = (targetMinutes.coerceAtLeast(0L)) * 60L
        val safeLogged = loggedSecondsToday.coerceAtLeast(0L)
        val liveElapsedSec = if (activeSessionStartedAtMs > 0L) {
            ((nowMs - activeSessionStartedAtMs) / 1000L).coerceAtLeast(0L)
        } else {
            0L
        }
        val totalStudiedSec = safeLogged + liveElapsedSec

        if (targetGoalSec <= 0L) {
            return FocusRuntimeStatus(
                hasActiveGoal = false,
                targetGoalSec = 0L,
                loggedSecondsToday = safeLogged,
                liveSessionElapsedSec = liveElapsedSec,
                totalStudiedTodaySec = totalStudiedSec,
                remainingGoalSec = 0L,
                phase = FocusPhase.NO_GOAL,
                phaseRemainingSec = 0L,
                currentHourNumber = 0,
                progressFraction = 0f,
                isPhysicalStudyRunning = activeSessionStartedAtMs > 0L,
                isSocialMediaDisabled = false
            )
        }

        val remainingGoalSec = (targetGoalSec - totalStudiedSec).coerceAtLeast(0L)
        val progress = (totalStudiedSec.toFloat() / targetGoalSec.toFloat()).coerceIn(0f, 1f)
        val isGoalDone = totalStudiedSec >= targetGoalSec

        if (isGoalDone) {
            return FocusRuntimeStatus(
                hasActiveGoal = true,
                targetGoalSec = targetGoalSec,
                loggedSecondsToday = safeLogged,
                liveSessionElapsedSec = liveElapsedSec,
                totalStudiedTodaySec = totalStudiedSec,
                remainingGoalSec = 0L,
                phase = FocusPhase.GOAL_COMPLETED,
                phaseRemainingSec = 0L,
                currentHourNumber = 0,
                progressFraction = 1f,
                isPhysicalStudyRunning = activeSessionStartedAtMs > 0L,
                isSocialMediaDisabled = false
            )
        }

        val effectiveAnchor = if (cycleAnchorEpochMs > 0L) cycleAnchorEpochMs else nowMs
        val elapsedCycleSec = ((nowMs - effectiveAnchor) / 1000L).coerceAtLeast(0L)
        val posInHour = elapsedCycleSec % CYCLE_DURATION_SECONDS
        val hourNumber = (elapsedCycleSec / CYCLE_DURATION_SECONDS).toInt() + 1

        val isLocked = posInHour < LOCK_DURATION_SECONDS
        val phase = if (isLocked) FocusPhase.SOCIAL_LOCKED else FocusPhase.FIVE_MIN_BREAK
        val phaseRemaining = if (isLocked) {
            LOCK_DURATION_SECONDS - posInHour
        } else {
            CYCLE_DURATION_SECONDS - posInHour
        }

        return FocusRuntimeStatus(
            hasActiveGoal = true,
            targetGoalSec = targetGoalSec,
            loggedSecondsToday = safeLogged,
            liveSessionElapsedSec = liveElapsedSec,
            totalStudiedTodaySec = totalStudiedSec,
            remainingGoalSec = remainingGoalSec,
            phase = phase,
            phaseRemainingSec = phaseRemaining,
            currentHourNumber = hourNumber,
            progressFraction = progress,
            isPhysicalStudyRunning = activeSessionStartedAtMs > 0L,
            isSocialMediaDisabled = isLocked
        )
    }

    fun formatDurationHms(seconds: Long): String {
        val safe = seconds.coerceAtLeast(0L)
        val h = safe / 3600L
        val m = (safe % 3600L) / 60L
        val s = safe % 60L
        return if (h > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    fun formatHoursMinutes(seconds: Long): String {
        val safe = seconds.coerceAtLeast(0L)
        val totalMinutes = safe / 60L
        val hours = totalMinutes / 60L
        val mins = totalMinutes % 60L
        return when {
            safe == 0L -> "0m"
            hours == 0L && mins == 0L -> "${safe}s"
            hours == 0L -> "${mins}m"
            mins == 0L -> "${hours}h"
            else -> "${hours}h ${mins}m"
        }
    }

    fun formatRemainingBan(remainingMs: Long): String {
        val safeSec = (remainingMs / 1000L).coerceAtLeast(0L)
        val days = safeSec / 86400L
        val hours = (safeSec % 86400L) / 3600L
        val mins = (safeSec % 3600L) / 60L
        return "${days}d ${hours}h ${mins}m"
    }
}
