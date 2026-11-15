package com.example.util

import com.example.model.QuizAttempt
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DayStreakStatus(
    val dayLabel: String,
    val completed: Boolean,
    val isToday: Boolean
)

data class StreakSummary(
    val currentStreak: Int,
    val longestStreak: Int,
    val practicedToday: Boolean,
    val lastSevenDays: List<DayStreakStatus>
)

object StreakCalculator {
    private const val MILLIS_PER_DAY = 86_400_000L

    fun toLocalDayNumber(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val localCal = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                localCal.get(Calendar.YEAR),
                localCal.get(Calendar.MONTH),
                localCal.get(Calendar.DAY_OF_MONTH)
            )
        }
        return utcCal.timeInMillis / MILLIS_PER_DAY
    }

    fun calculate(
        attempts: List<QuizAttempt>,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): StreakSummary {
        val todayDay = toLocalDayNumber(nowMillis, timeZone)
        val practiceDays = attempts
            .mapNotNull { it.createdAt?.toDate()?.time }
            .map { toLocalDayNumber(it, timeZone) }
            .toSet()

        val practicedToday = todayDay in practiceDays

        val currentStreak = when {
            practicedToday -> countConsecutiveBackwards(practiceDays, todayDay)
            (todayDay - 1) in practiceDays -> countConsecutiveBackwards(practiceDays, todayDay - 1)
            else -> 0
        }

        val longestStreak = computeLongestStreak(practiceDays)

        val dayFmt = SimpleDateFormat("EEEEE", Locale.getDefault()).apply {
            this.timeZone = timeZone
        }
        val lastSevenDays = (6 downTo 0).map { daysAgo ->
            val targetDay = todayDay - daysAgo
            val cal = Calendar.getInstance(timeZone).apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            DayStreakStatus(
                dayLabel = dayFmt.format(Date(cal.timeInMillis)),
                completed = targetDay in practiceDays,
                isToday = daysAgo == 0
            )
        }

        return StreakSummary(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            practicedToday = practicedToday,
            lastSevenDays = lastSevenDays
        )
    }

    private fun countConsecutiveBackwards(days: Set<Long>, startDay: Long): Int {
        var count = 0
        var cursor = startDay
        while (cursor in days) {
            count++
            cursor--
        }
        return count
    }

    private fun computeLongestStreak(days: Set<Long>): Int {
        if (days.isEmpty()) return 0
        val sorted = days.sorted()
        var longest = 1
        var run = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1] + 1L) {
                run++
                if (run > longest) longest = run
            } else {
                run = 1
            }
        }
        return longest
    }
}
