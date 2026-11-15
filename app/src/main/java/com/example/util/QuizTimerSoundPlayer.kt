package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

enum class TimerAlertTone {
    NONE,
    THIRTY_SECOND_WARNING,
    TEN_SECOND_URGENT,
    FINAL_COUNTDOWN_TICK
}

object QuizTimerAlertPolicy {
    const val WARNING_THRESHOLD_SEC = 30
    const val URGENT_THRESHOLD_SEC = 10

    fun isWarningActive(remainingSec: Int): Boolean = remainingSec in 1..WARNING_THRESHOLD_SEC

    fun isUrgentActive(remainingSec: Int): Boolean = remainingSec in 1..URGENT_THRESHOLD_SEC

    fun resolveTone(remainingSec: Int, totalSecForQuestion: Int): TimerAlertTone {
        if (remainingSec <= 0) return TimerAlertTone.NONE
        return when {
            remainingSec == WARNING_THRESHOLD_SEC -> TimerAlertTone.THIRTY_SECOND_WARNING
            totalSecForQuestion < WARNING_THRESHOLD_SEC && remainingSec == totalSecForQuestion ->
                TimerAlertTone.THIRTY_SECOND_WARNING
            remainingSec == URGENT_THRESHOLD_SEC -> TimerAlertTone.TEN_SECOND_URGENT
            remainingSec in 1..5 -> TimerAlertTone.FINAL_COUNTDOWN_TICK
            else -> TimerAlertTone.NONE
        }
    }
}

class QuizTimerSoundPlayer {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val audioScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO.limitedParallelism(1)
    )
    private var toneGenerator: ToneGenerator? = null

    init {
        audioScope.launch {
            toneGenerator = runCatching {
                ToneGenerator(AudioManager.STREAM_MUSIC, 85)
            }.onFailure { err ->
                Log.w(TAG, "ToneGenerator unavailable on this device", err)
            }.getOrNull()
        }
    }

    fun play(tone: TimerAlertTone) {
        if (tone == TimerAlertTone.NONE) return
        audioScope.launch {
            val generator = toneGenerator ?: return@launch
            runCatching {
                when (tone) {
                    TimerAlertTone.THIRTY_SECOND_WARNING ->
                        generator.startTone(ToneGenerator.TONE_PROP_BEEP2, 320)
                    TimerAlertTone.TEN_SECOND_URGENT ->
                        generator.startTone(ToneGenerator.TONE_SUP_PIP, 260)
                    TimerAlertTone.FINAL_COUNTDOWN_TICK ->
                        generator.startTone(ToneGenerator.TONE_PROP_BEEP, 110)
                    TimerAlertTone.NONE -> Unit
                }
            }.onFailure { err ->
                Log.w(TAG, "Failed to play timer alert tone $tone", err)
            }
        }
    }

    fun release() {
        audioScope.launch {
            runCatching {
                toneGenerator?.release()
            }
            toneGenerator = null
            audioScope.cancel()
        }
    }

    private companion object {
        const val TAG = "QuizTimerSoundPlayer"
    }
}
