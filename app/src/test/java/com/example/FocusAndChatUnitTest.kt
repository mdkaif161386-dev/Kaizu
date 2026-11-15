package com.example

import com.example.model.StudyTimeLeaderboardEntry
import com.example.ui.viewmodel.FocusViewModel
import com.example.util.ChatPolicyFilter
import com.example.util.FocusCycleCalculator
import com.example.util.FocusPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class FocusAndChatUnitTest {

    @Test
    fun evaluate_noGoal_keepsSocialAppsEnabled() {
        val status = FocusCycleCalculator.evaluate(
            targetMinutes = 0L,
            loggedSecondsToday = 0L,
            activeSessionStartedAtMs = 0L,
            cycleAnchorEpochMs = 0L,
            nowMs = 1_000_000L
        )
        assertFalse(status.hasActiveGoal)
        assertEquals(FocusPhase.NO_GOAL, status.phase)
        assertFalse(status.isSocialMediaDisabled)
        assertEquals(0L, status.remainingGoalSec)
    }

    @Test
    fun evaluate_oneHourLoop_locks55mUnlocks5mAndLoops() {
        val anchorMs = 1_700_000_000_000L
        // At t = 0s: Hour 1, SOCIAL_LOCKED, 3300s left
        val s0 = FocusCycleCalculator.evaluate(
            targetMinutes = 180L,
            loggedSecondsToday = 0L,
            activeSessionStartedAtMs = 0L,
            cycleAnchorEpochMs = anchorMs,
            nowMs = anchorMs
        )
        assertEquals(FocusPhase.SOCIAL_LOCKED, s0.phase)
        assertTrue(s0.isSocialMediaDisabled)
        assertEquals(3300L, s0.phaseRemainingSec)
        assertEquals(1, s0.currentHourNumber)

        // At t = 54m 59s (3299s): still SOCIAL_LOCKED with 1s left
        val sAlmostBreak = FocusCycleCalculator.evaluate(
            targetMinutes = 180L,
            loggedSecondsToday = 0L,
            activeSessionStartedAtMs = 0L,
            cycleAnchorEpochMs = anchorMs,
            nowMs = anchorMs + 3299_000L
        )
        assertEquals(FocusPhase.SOCIAL_LOCKED, sAlmostBreak.phase)
        assertTrue(sAlmostBreak.isSocialMediaDisabled)
        assertEquals(1L, sAlmostBreak.phaseRemainingSec)

        // At t = 55m 00s (3300s): enters FIVE_MIN_BREAK with 300s left
        val sBreakStart = FocusCycleCalculator.evaluate(
            targetMinutes = 180L,
            loggedSecondsToday = 0L,
            activeSessionStartedAtMs = 0L,
            cycleAnchorEpochMs = anchorMs,
            nowMs = anchorMs + 3300_000L
        )
        assertEquals(FocusPhase.FIVE_MIN_BREAK, sBreakStart.phase)
        assertFalse(sBreakStart.isSocialMediaDisabled)
        assertEquals(300L, sBreakStart.phaseRemainingSec)
        assertEquals(1, sBreakStart.currentHourNumber)

        // At t = 59m 59s (3599s): still FIVE_MIN_BREAK with 1s left
        val sBreakEnd = FocusCycleCalculator.evaluate(
            targetMinutes = 180L,
            loggedSecondsToday = 0L,
            activeSessionStartedAtMs = 0L,
            cycleAnchorEpochMs = anchorMs,
            nowMs = anchorMs + 3599_000L
        )
        assertEquals(FocusPhase.FIVE_MIN_BREAK, sBreakEnd.phase)
        assertFalse(sBreakEnd.isSocialMediaDisabled)
        assertEquals(1L, sBreakEnd.phaseRemainingSec)

        // At t = 60m 00s (3600s): loops back to SOCIAL_LOCKED for Hour 2
        val sHour2 = FocusCycleCalculator.evaluate(
            targetMinutes = 180L,
            loggedSecondsToday = 0L,
            activeSessionStartedAtMs = 0L,
            cycleAnchorEpochMs = anchorMs,
            nowMs = anchorMs + 3600_000L
        )
        assertEquals(FocusPhase.SOCIAL_LOCKED, sHour2.phase)
        assertTrue(sHour2.isSocialMediaDisabled)
        assertEquals(3300L, sHour2.phaseRemainingSec)
        assertEquals(2, sHour2.currentHourNumber)
    }

    @Test
    fun evaluate_physicalStudyCompletion_autoUnlocksAllSocialApps() {
        val anchorMs = 1_700_000_000_000L
        // Goal: 60 min (3600s). Logged 3000s, live physical study started 600s ago -> total 3600s.
        val completed = FocusCycleCalculator.evaluate(
            targetMinutes = 60L,
            loggedSecondsToday = 3000L,
            activeSessionStartedAtMs = anchorMs,
            cycleAnchorEpochMs = anchorMs,
            nowMs = anchorMs + 600_000L
        )
        assertEquals(FocusPhase.GOAL_COMPLETED, completed.phase)
        assertFalse(completed.isSocialMediaDisabled)
        assertEquals(0L, completed.remainingGoalSec)
        assertEquals(1f, completed.progressFraction, 0.001f)
        assertTrue(completed.isPhysicalStudyRunning)
    }

    @Test
    fun periodKeys_formatCorrectlyInUtc() {
        // 2026-11-15T12:00:00Z -> epoch ms
        val utc = TimeZone.getTimeZone("UTC")
        val cal = java.util.Calendar.getInstance(utc).apply {
            set(2026, java.util.Calendar.NOVEMBER, 15, 12, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val ms = cal.timeInMillis
        assertEquals("2026-11-15", FocusCycleCalculator.dateKeyFor(ms, utc))
        assertEquals("2026-11", FocusCycleCalculator.monthKeyFor(ms, utc))
        assertTrue(FocusCycleCalculator.weekKeyFor(ms, utc).startsWith("2026-W"))
    }

    @Test
    fun chatPolicyFilter_blocksAdultVideosAndAllowsAcademicBiology() {
        assertTrue(ChatPolicyFilter.evaluateLocal("Watch this adult video link").isViolation)
        assertTrue(ChatPolicyFilter.evaluateLocal("Check pornhub.com").isViolation)
        assertTrue(ChatPolicyFilter.evaluateLocal("Send nudes", "hot_video.mp4").isViolation)
        assertTrue(ChatPolicyFilter.evaluateLocal("Free 18+ video download").isViolation)

        // Academic NCERT Biology doubts should be permitted
        assertFalse(
            ChatPolicyFilter.evaluateLocal(
                "Explain sexual reproduction in flowering plants from NCERT Biology"
            ).isViolation
        )
        assertFalse(
            ChatPolicyFilter.evaluateLocal(
                "Can someone share HC Verma rotational dynamics notes?"
            ).isViolation
        )
    }

    @Test
    fun personalTimeBreakdown_resetsStaleBucketsAndAddsLiveSession() {
        val nowMs = System.currentTimeMillis()
        val entry = StudyTimeLeaderboardEntry(
            userId = "u1",
            dayKey = "1999-01-01",
            daySeconds = 5000L,
            weekKey = "1999-W01",
            weekSeconds = 9000L,
            monthKey = FocusCycleCalculator.monthKeyFor(nowMs),
            monthSeconds = 3600L
        )
        val (day, week, month) = FocusViewModel.personalTimeBreakdown(
            entry = entry,
            liveExtraSec = 120L,
            nowMs = nowMs
        )
        assertEquals(120L, day)
        assertEquals(120L, week)
        assertEquals(3720L, month)
    }
}
