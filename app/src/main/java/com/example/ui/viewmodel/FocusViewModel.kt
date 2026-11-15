package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreRepository
import com.example.focus.FocusPreferences
import com.example.focus.LocalFocusSnapshot
import com.example.focus.StudyFocusService
import com.example.model.StudyGoalRecord
import com.example.model.StudyTimeLeaderboardEntry
import com.example.model.UserProfile
import com.example.util.FocusCycleCalculator
import com.example.util.FocusPhase
import com.example.util.FocusRuntimeStatus
import com.example.util.SocialMediaAppInfo
import com.example.util.SocialMediaCatalog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FocusViewModel(
    private val appContext: Context,
    private val firestoreRepo: FirestoreRepository,
    private val currentUserId: String
) : ViewModel() {

    private val focusPrefs = FocusPreferences(appContext)

    private val _snapshot = MutableStateFlow(focusPrefs.read())
    val snapshot: StateFlow<LocalFocusSnapshot> = _snapshot.asStateFlow()

    private val _runtimeStatus = MutableStateFlow(evaluateSnapshot(_snapshot.value))
    val runtimeStatus: StateFlow<FocusRuntimeStatus> = _runtimeStatus.asStateFlow()

    private val _socialApps = MutableStateFlow(SocialMediaCatalog.knownApps)
    val socialApps: StateFlow<List<SocialMediaAppInfo>> = _socialApps.asStateFlow()

    private val _hasUsageAccess = MutableStateFlow(false)
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        refreshPermissionsAndApps()
        observeTodayGoal()
        startTicker()
    }

    private fun observeTodayGoal() {
        val todayKey = FocusCycleCalculator.dateKeyFor()
        viewModelScope.launch {
            firestoreRepo.observeStudyGoal(currentUserId, todayKey)
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing study goal", fsErr ?: error)
                }
                .collect { remote ->
                    if (remote != null) {
                        val local = focusPrefs.syncFromRemote(remote, todayKey)
                        _snapshot.value = local
                        val status = evaluateSnapshot(local)
                        _runtimeStatus.value = status
                        syncForegroundService(status)
                    }
                }
        }
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (isActive) {
                val todayKey = FocusCycleCalculator.dateKeyFor()
                val current = _snapshot.value
                val effective = if (current.dateKey != todayKey) {
                    focusPrefs.read(todayKey).also { _snapshot.value = it }
                } else {
                    current
                }
                val evaluated = evaluateSnapshot(effective)
                if (_runtimeStatus.value != evaluated) {
                    _runtimeStatus.value = evaluated
                }
                val isTicking = evaluated.isPhysicalStudyRunning ||
                    (evaluated.hasActiveGoal && !evaluated.isGoalCompleted)
                delay(if (isTicking) 1_000L else 5_000L)
            }
        }
    }

    fun refreshPermissionsAndApps() {
        viewModelScope.launch {
            val (hasAccess, resolvedApps) = withContext(Dispatchers.IO) {
                SocialMediaCatalog.hasUsageStatsPermission(appContext) to
                    SocialMediaCatalog.resolveInstalledStatus(appContext)
            }
            _hasUsageAccess.value = hasAccess
            _socialApps.value = resolvedApps
        }
    }

    fun setDailyGoal(targetMinutes: Long, blockedPackages: Set<String>? = null) {
        val safeMinutes = targetMinutes.coerceIn(15L, 720L)
        val todayKey = FocusCycleCalculator.dateKeyFor()
        val nowMs = System.currentTimeMillis()
        val current = focusPrefs.read(todayKey)
        val updated = current.copy(
            userId = currentUserId,
            dateKey = todayKey,
            targetMinutes = safeMinutes,
            cycleAnchorEpochMs = if (current.cycleAnchorEpochMs > 0L) current.cycleAnchorEpochMs else nowMs,
            blockedPackages = blockedPackages ?: current.blockedPackages
        )
        persistSnapshot(updated)
        _statusMessage.value = "Daily goal set to ${FocusCycleCalculator.formatHoursMinutes(safeMinutes * 60L)}. Social apps guarded!"
    }

    fun clearDailyGoal() {
        val todayKey = FocusCycleCalculator.dateKeyFor()
        val current = focusPrefs.read(todayKey)
        val updated = current.copy(
            userId = currentUserId,
            dateKey = todayKey,
            targetMinutes = 0L,
            cycleAnchorEpochMs = 0L
        )
        persistSnapshot(updated)
        _statusMessage.value = "Daily goal cleared. Social apps unlocked."
    }

    fun toggleBlockedPackage(packageName: String) {
        val todayKey = FocusCycleCalculator.dateKeyFor()
        val current = focusPrefs.read(todayKey)
        val nextSet = current.blockedPackages.toMutableSet()
        if (packageName in nextSet) nextSet.remove(packageName) else nextSet.add(packageName)
        val updated = current.copy(
            userId = currentUserId,
            dateKey = todayKey,
            blockedPackages = nextSet
        )
        persistSnapshot(updated)
    }

    fun startPhysicalStudy(subjectLabel: String) {
        val todayKey = FocusCycleCalculator.dateKeyFor()
        val nowMs = System.currentTimeMillis()
        val current = focusPrefs.read(todayKey)
        if (current.activeSessionStartedAtMs > 0L) return
        val updated = current.copy(
            userId = currentUserId,
            dateKey = todayKey,
            activeSessionStartedAtMs = nowMs,
            activeSubject = subjectLabel.trim().take(80),
            cycleAnchorEpochMs = if (current.targetMinutes > 0L && current.cycleAnchorEpochMs <= 0L) {
                nowMs
            } else {
                current.cycleAnchorEpochMs
            }
        )
        persistSnapshot(updated)
        _statusMessage.value = "Physical study started! Timer keeps counting even if you lock your phone."
    }

    fun stopAndLogPhysicalStudy(profile: UserProfile) {
        val todayKey = FocusCycleCalculator.dateKeyFor()
        val nowMs = System.currentTimeMillis()
        val current = focusPrefs.read(todayKey)
        if (current.activeSessionStartedAtMs <= 0L) return

        val elapsedSec = ((nowMs - current.activeSessionStartedAtMs) / 1000L).coerceAtLeast(1L)
        val updated = current.copy(
            userId = currentUserId,
            dateKey = todayKey,
            loggedSeconds = current.loggedSeconds + elapsedSec,
            activeSessionStartedAtMs = 0L,
            activeSubject = ""
        )
        persistSnapshot(updated)
        viewModelScope.launch {
            firestoreRepo.recordStudySeconds(
                profile = profile,
                addedSeconds = elapsedSec,
                creditDailyGoal = false
            )
        }
        _statusMessage.value = "Logged ${FocusCycleCalculator.formatHoursMinutes(elapsedSec)} of physical study!"
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun persistSnapshot(snap: LocalFocusSnapshot) {
        focusPrefs.write(snap)
        _snapshot.value = snap
        val evaluated = evaluateSnapshot(snap)
        _runtimeStatus.value = evaluated
        syncForegroundService(evaluated)

        viewModelScope.launch {
            firestoreRepo.saveStudyGoal(
                StudyGoalRecord(
                    id = snap.dateKey,
                    userId = currentUserId,
                    dateKey = snap.dateKey,
                    targetMinutes = snap.targetMinutes,
                    loggedSeconds = snap.loggedSeconds,
                    activeSessionStartedAtMs = snap.activeSessionStartedAtMs,
                    cycleAnchorEpochMs = snap.cycleAnchorEpochMs,
                    activeSubject = snap.activeSubject,
                    blockedPackages = snap.blockedPackages.toList()
                )
            )
        }
    }

    private fun syncForegroundService(status: FocusRuntimeStatus) {
        val shouldRun = (status.hasActiveGoal && !status.isGoalCompleted && status.phase != FocusPhase.GOAL_COMPLETED) ||
            status.isPhysicalStudyRunning
        StudyFocusService.sync(appContext, shouldRun)
    }

    private fun evaluateSnapshot(snap: LocalFocusSnapshot): FocusRuntimeStatus {
        return FocusCycleCalculator.evaluate(
            targetMinutes = snap.targetMinutes,
            loggedSecondsToday = snap.loggedSeconds,
            activeSessionStartedAtMs = snap.activeSessionStartedAtMs,
            cycleAnchorEpochMs = snap.cycleAnchorEpochMs
        )
    }

    companion object {
        private const val TAG = "FocusViewModel"

        fun personalTimeBreakdown(
            entry: StudyTimeLeaderboardEntry?,
            liveExtraSec: Long = 0L,
            nowMs: Long = System.currentTimeMillis()
        ): Triple<Long, Long, Long> {
            val dayKey = FocusCycleCalculator.dateKeyFor(nowMs)
            val weekKey = FocusCycleCalculator.weekKeyFor(nowMs)
            val monthKey = FocusCycleCalculator.monthKeyFor(nowMs)

            val dayBase = if (entry?.dayKey == dayKey) entry.daySeconds else 0L
            val weekBase = if (entry?.weekKey == weekKey) entry.weekSeconds else 0L
            val monthBase = if (entry?.monthKey == monthKey) entry.monthSeconds else 0L

            return Triple(
                dayBase + liveExtraSec,
                weekBase + liveExtraSec,
                monthBase + liveExtraSec
            )
        }
    }
}
