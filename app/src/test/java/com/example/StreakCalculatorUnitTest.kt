package com.example

import com.example.model.QuizAttempt
import com.example.util.StreakCalculator
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class StreakCalculatorUnitTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private fun dayMillis(year: Int, month: Int, day: Int, hour: Int = 10): Long {
        return Calendar.getInstance(utc).apply {
            set(year, month - 1, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun attemptAt(millis: Long): QuizAttempt =
        QuizAttempt(createdAt = Timestamp(Date(millis)))

    @Test
    fun emptyAttempts_returnsZeroStreak() {
        val now = dayMillis(2026, 11, 15)
        val summary = StreakCalculator.calculate(emptyList(), now, utc)
        assertEquals(0, summary.currentStreak)
        assertEquals(0, summary.longestStreak)
        assertFalse(summary.practicedToday)
        assertEquals(7, summary.lastSevenDays.size)
    }

    @Test
    fun consecutiveDaysEndingToday_computesExactStreak() {
        val now = dayMillis(2026, 11, 15, 18)
        val attempts = listOf(
            attemptAt(dayMillis(2026, 11, 15, 9)),
            attemptAt(dayMillis(2026, 11, 15, 14)), // same day duplicate should count once
            attemptAt(dayMillis(2026, 11, 14, 21)),
            attemptAt(dayMillis(2026, 11, 13, 8)),
            attemptAt(dayMillis(2026, 11, 10, 12)) // gap before 13th
        )
        val summary = StreakCalculator.calculate(attempts, now, utc)
        assertEquals(3, summary.currentStreak)
        assertEquals(3, summary.longestStreak)
        assertTrue(summary.practicedToday)
    }

    @Test
    fun practicedYesterdayButNotToday_keepsStreakAlive() {
        val now = dayMillis(2026, 11, 15, 9)
        val attempts = listOf(
            attemptAt(dayMillis(2026, 11, 14, 20)),
            attemptAt(dayMillis(2026, 11, 13, 18))
        )
        val summary = StreakCalculator.calculate(attempts, now, utc)
        assertEquals(2, summary.currentStreak)
        assertFalse(summary.practicedToday)
    }

    @Test
    fun missedTwoDays_resetsCurrentStreakWhilePreservingLongest() {
        val now = dayMillis(2026, 11, 15, 9)
        val attempts = listOf(
            attemptAt(dayMillis(2026, 11, 12, 10)),
            attemptAt(dayMillis(2026, 11, 11, 10)),
            attemptAt(dayMillis(2026, 11, 10, 10)),
            attemptAt(dayMillis(2026, 11, 9, 10))
        )
        val summary = StreakCalculator.calculate(attempts, now, utc)
        assertEquals(0, summary.currentStreak)
        assertEquals(4, summary.longestStreak)
        assertFalse(summary.practicedToday)
    }
}
