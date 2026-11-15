package com.example

import com.example.util.QuizTimerAlertPolicy
import com.example.util.TimerAlertTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizTimerAlertUnitTest {

    @Test
    fun warningActive_forFinalThirtySecondsOnly() {
        assertFalse(QuizTimerAlertPolicy.isWarningActive(31))
        assertTrue(QuizTimerAlertPolicy.isWarningActive(30))
        assertTrue(QuizTimerAlertPolicy.isWarningActive(15))
        assertTrue(QuizTimerAlertPolicy.isWarningActive(1))
        assertFalse(QuizTimerAlertPolicy.isWarningActive(0))
    }

    @Test
    fun resolveTone_triggersAtThirtyTenAndFinalFiveSeconds() {
        assertEquals(TimerAlertTone.NONE, QuizTimerAlertPolicy.resolveTone(45, 60))
        assertEquals(TimerAlertTone.NONE, QuizTimerAlertPolicy.resolveTone(31, 60))
        assertEquals(TimerAlertTone.THIRTY_SECOND_WARNING, QuizTimerAlertPolicy.resolveTone(30, 60))
        assertEquals(TimerAlertTone.NONE, QuizTimerAlertPolicy.resolveTone(20, 60))
        assertEquals(TimerAlertTone.TEN_SECOND_URGENT, QuizTimerAlertPolicy.resolveTone(10, 60))
        assertEquals(TimerAlertTone.FINAL_COUNTDOWN_TICK, QuizTimerAlertPolicy.resolveTone(5, 60))
        assertEquals(TimerAlertTone.FINAL_COUNTDOWN_TICK, QuizTimerAlertPolicy.resolveTone(1, 60))
        assertEquals(TimerAlertTone.NONE, QuizTimerAlertPolicy.resolveTone(0, 60))
    }

    @Test
    fun resolveTone_triggersImmediateWarningWhenQuestionTimerStartsUnderThirtySeconds() {
        assertEquals(TimerAlertTone.THIRTY_SECOND_WARNING, QuizTimerAlertPolicy.resolveTone(20, 20))
        assertEquals(TimerAlertTone.TEN_SECOND_URGENT, QuizTimerAlertPolicy.resolveTone(10, 20))
    }
}
