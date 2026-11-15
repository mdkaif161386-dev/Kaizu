package com.example.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.util.FocusCycleCalculator
import com.example.util.FocusPhase
import com.example.util.SocialMediaCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StudyFocusService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private lateinit var focusPrefs: FocusPreferences
    private lateinit var notificationManager: NotificationManager
    private var usageStatsManager: UsageStatsManager? = null
    private var powerManager: PowerManager? = null

    private var lastPhase: FocusPhase? = null
    private var lastNotificationUpdateMs: Long = 0L
    private var lastRedirectMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        focusPrefs = FocusPreferences(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        ensureNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            monitorJob?.cancel()
            stopSelf()
            return START_NOT_STICKY
        }

        val initialStatus = computeCurrentStatus()
        val initialNotif = buildOngoingNotification(initialStatus)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID_ONGOING,
                initialNotif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID_ONGOING, initialNotif)
        }

        if (monitorJob?.isActive != true) {
            startMonitoringLoop()
        }
        return START_STICKY
    }

    private fun startMonitoringLoop() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                val snap = focusPrefs.read()
                val status = FocusCycleCalculator.evaluate(
                    targetMinutes = snap.targetMinutes,
                    loggedSecondsToday = snap.loggedSeconds,
                    activeSessionStartedAtMs = snap.activeSessionStartedAtMs,
                    cycleAnchorEpochMs = snap.cycleAnchorEpochMs
                )

                val shouldKeepRunning = (status.hasActiveGoal && !status.isGoalCompleted) ||
                    status.isPhysicalStudyRunning

                if (!shouldKeepRunning) {
                    if (status.isGoalCompleted && lastPhase != FocusPhase.GOAL_COMPLETED) {
                        postAlertNotification(
                            title = "Daily Study Goal Complete!",
                            body = "Great discipline! All social media apps are now automatically enabled."
                        )
                    }
                    stopSelf()
                    break
                }

                val nowMs = System.currentTimeMillis()
                val isScreenOn = powerManager?.isInteractive != false
                val phaseChanged = lastPhase != status.phase
                if (phaseChanged) {
                    if (lastPhase == FocusPhase.SOCIAL_LOCKED && status.phase == FocusPhase.FIVE_MIN_BREAK) {
                        postAlertNotification(
                            title = "5-Minute Break Unlocked",
                            body = "Social media apps are temporarily enabled for 5 minutes before the next focus block."
                        )
                    } else if (lastPhase == FocusPhase.FIVE_MIN_BREAK && status.phase == FocusPhase.SOCIAL_LOCKED) {
                        postAlertNotification(
                            title = "Focus Guard Re-Locked",
                            body = "5-minute break ended. Social media apps are disabled until your next hour mark."
                        )
                    }
                    lastPhase = status.phase
                }

                if (phaseChanged || (isScreenOn && nowMs - lastNotificationUpdateMs >= 5_000L)) {
                    lastNotificationUpdateMs = nowMs
                    notificationManager.notify(
                        NOTIFICATION_ID_ONGOING,
                        buildOngoingNotification(status)
                    )
                }

                val needsAppCheck = isScreenOn &&
                    status.isSocialMediaDisabled &&
                    snap.blockedPackages.isNotEmpty() &&
                    SocialMediaCatalog.hasUsageStatsPermission(this@StudyFocusService)

                if (needsAppCheck) {
                    checkForegroundBlockedApp(snap.blockedPackages, nowMs)
                }

                val pollDelayMs = when {
                    !isScreenOn -> 5_000L
                    needsAppCheck -> 1_500L
                    else -> 3_000L
                }
                delay(pollDelayMs)
            }
        }
    }

    private fun checkForegroundBlockedApp(blockedPackages: Set<String>, nowMs: Long) {
        val usm = usageStatsManager ?: return
        val events = runCatching { usm.queryEvents(nowMs - 4_000L, nowMs) }.getOrNull() ?: return
        val event = UsageEvents.Event()
        var latestPackage: String? = null
        var latestTime = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val isResumed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            } else {
                @Suppress("DEPRECATION")
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            }
            if (isResumed && event.timeStamp >= latestTime) {
                latestTime = event.timeStamp
                latestPackage = event.packageName
            }
        }

        if (latestPackage != null &&
            latestPackage != packageName &&
            latestPackage in blockedPackages &&
            nowMs - lastRedirectMs >= 2_500L
        ) {
            lastRedirectMs = nowMs
            val appLabel = SocialMediaCatalog.labelForPackage(latestPackage)
            postAlertNotification(
                title = "$appLabel Blocked by Kaizu Focus",
                body = "Your daily study goal is still active. Tap to return to Kaizu Study Focus."
            )
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_BLOCKED_APP_LABEL, appLabel)
            }
            runCatching { startActivity(launchIntent) }
        }
    }

    private fun computeCurrentStatus() = focusPrefs.read().let { snap ->
        FocusCycleCalculator.evaluate(
            targetMinutes = snap.targetMinutes,
            loggedSecondsToday = snap.loggedSeconds,
            activeSessionStartedAtMs = snap.activeSessionStartedAtMs,
            cycleAnchorEpochMs = snap.cycleAnchorEpochMs
        )
    }

    private fun buildOngoingNotification(status: com.example.util.FocusRuntimeStatus): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            101,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_OPEN_FOCUS_TAB, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            status.isPhysicalStudyRunning && status.phase == FocusPhase.SOCIAL_LOCKED ->
                "Physical Study Active • Social Apps Locked"
            status.isPhysicalStudyRunning && status.phase == FocusPhase.FIVE_MIN_BREAK ->
                "Physical Study Active • 5-Min Break Window"
            status.isPhysicalStudyRunning ->
                "Physical Study Timer Running"
            status.phase == FocusPhase.FIVE_MIN_BREAK ->
                "5-Min Social Break Active"
            else ->
                "Kaizu Focus Guard Active"
        }

        val detail = buildString {
            if (status.hasActiveGoal) {
                append("Goal left: ${FocusCycleCalculator.formatHoursMinutes(status.remainingGoalSec)}")
                when (status.phase) {
                    FocusPhase.SOCIAL_LOCKED ->
                        append(" • 5m break in ${FocusCycleCalculator.formatDurationHms(status.phaseRemainingSec)}")
                    FocusPhase.FIVE_MIN_BREAK ->
                        append(" • Re-locks in ${FocusCycleCalculator.formatDurationHms(status.phaseRemainingSec)}")
                    FocusPhase.GOAL_COMPLETED ->
                        append(" • Goal Complete!")
                    FocusPhase.NO_GOAL -> Unit
                }
            } else if (status.isPhysicalStudyRunning) {
                append("Physical study elapsed: ${FocusCycleCalculator.formatDurationHms(status.liveSessionElapsedSec)}")
            }
        }

        return NotificationCompat.Builder(this, CHANNEL_ONGOING_ID)
            .setSmallIcon(R.drawable.ic_focus_shield)
            .setContentTitle(title)
            .setContentText(detail.ifBlank { "Stay focused until your daily goal completes." })
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun postAlertNotification(title: String, body: String) {
        val openAppIntent = PendingIntent.getActivity(
            this,
            102,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_OPEN_FOCUS_TAB, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_focus_shield)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(NOTIFICATION_ID_ALERT, notif)
    }

    private fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ongoing = NotificationChannel(
                CHANNEL_ONGOING_ID,
                "Study Goal & Focus Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live countdown for daily study goals, physical study timer, and 55m/5m break cycles."
            }
            val alerts = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "Social Blocker & Break Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when blocked social media apps open or when 5-minute break intervals start/end."
            }
            notificationManager.createNotificationChannel(ongoing)
            notificationManager.createNotificationChannel(alerts)
        }
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_BLOCKED_APP_LABEL = "extra_blocked_app_label"
        const val EXTRA_OPEN_FOCUS_TAB = "extra_open_focus_tab"
        private const val ACTION_STOP = "com.example.focus.ACTION_STOP"
        private const val CHANNEL_ONGOING_ID = "kaizu_focus_ongoing"
        private const val CHANNEL_ALERTS_ID = "kaizu_focus_alerts"
        private const val NOTIFICATION_ID_ONGOING = 4401
        private const val NOTIFICATION_ID_ALERT = 4402

        fun sync(context: Context, shouldRun: Boolean) {
            val appCtx = context.applicationContext
            if (shouldRun) {
                val intent = Intent(appCtx, StudyFocusService::class.java)
                runCatching { ContextCompat.startForegroundService(appCtx, intent) }
            } else {
                val intent = Intent(appCtx, StudyFocusService::class.java).apply {
                    action = ACTION_STOP
                }
                runCatching { appCtx.stopService(intent) }
            }
        }
    }
}
