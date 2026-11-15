package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.focus.LocalFocusSnapshot
import com.example.model.UserProfile
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ForgeGold
import com.example.ui.theme.MegaObsidian
import com.example.ui.theme.VioletAccent
import com.example.util.FocusCycleCalculator
import com.example.util.FocusPhase
import com.example.util.FocusRuntimeStatus
import com.example.util.SocialMediaAppInfo
import com.example.util.SocialMediaCatalog
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusGoalScreen(
    profile: UserProfile,
    snapshot: LocalFocusSnapshot,
    status: FocusRuntimeStatus,
    socialApps: List<SocialMediaAppInfo>,
    hasUsageAccess: Boolean,
    personalDaySec: Long,
    personalWeekSec: Long,
    personalMonthSec: Long,
    statusMessage: String?,
    onRefreshPermissions: () -> Unit,
    onSetDailyGoal: (Long) -> Unit,
    onClearDailyGoal: () -> Unit,
    onStartPhysicalStudy: (String) -> Unit,
    onStopPhysicalStudy: () -> Unit,
    onToggleBlockedApp: (String) -> Unit,
    onDismissStatusMessage: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    var goalSliderMinutes by remember(snapshot.targetMinutes) {
        mutableFloatStateOf(
            if (snapshot.targetMinutes > 0L) snapshot.targetMinutes.toFloat() else 120f
        )
    }
    var physicalSubjectInput by remember { mutableStateOf("") }

    val phaseAccent = when (status.phase) {
        FocusPhase.SOCIAL_LOCKED -> CrimsonError
        FocusPhase.FIVE_MIN_BREAK -> EmeraldSuccess
        FocusPhase.GOAL_COMPLETED -> ForgeGold
        FocusPhase.NO_GOAL -> ElectricCyan
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Study Goal & Focus Guard",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Blocks distracting social apps (55m Lock / 5m Break loop) until today's goal is achieved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (statusMessage != null) {
                item {
                    Surface(
                        color = ElectricCyan.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .clickable { onDismissStatusMessage() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Focus update",
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Hero Daily Goal + 55m/5m Social Lock Status Card
            item {
                Surface(
                    color = MegaObsidian,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, phaseAccent.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
                        .testTag("focus_status_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = phaseAccent.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (status.phase) {
                                            FocusPhase.SOCIAL_LOCKED -> Icons.Filled.Lock
                                            FocusPhase.FIVE_MIN_BREAK, FocusPhase.GOAL_COMPLETED -> Icons.Filled.LockOpen
                                            FocusPhase.NO_GOAL -> Icons.Filled.Security
                                        },
                                        contentDescription = "Social guard status",
                                        tint = phaseAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = when (status.phase) {
                                            FocusPhase.SOCIAL_LOCKED -> "SOCIAL APPS DISABLED"
                                            FocusPhase.FIVE_MIN_BREAK -> "5-MIN BREAK ENABLED"
                                            FocusPhase.GOAL_COMPLETED -> "GOAL DONE • APPS UNLOCKED"
                                            FocusPhase.NO_GOAL -> "NO ACTIVE GOAL"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = phaseAccent
                                    )
                                }
                            }

                            if (status.hasActiveGoal && !status.isGoalCompleted) {
                                Text(
                                    text = "Hour #${status.currentHourNumber}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Box(
                            modifier = Modifier.size(152.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val trackColor = Color(0xFF1E293B)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 12.dp.toPx()
                                drawArc(
                                    color = trackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = phaseAccent,
                                    startAngle = -90f,
                                    sweepAngle = 360f * status.progressFraction,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (status.hasActiveGoal) {
                                        FocusCycleCalculator.formatDurationHms(status.remainingGoalSec)
                                    } else {
                                        "--:--"
                                    },
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                                Text(
                                    text = when (status.phase) {
                                        FocusPhase.NO_GOAL -> "Set goal below"
                                        FocusPhase.GOAL_COMPLETED -> "100% Completed"
                                        else -> "Goal remaining"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = when (status.phase) {
                                FocusPhase.SOCIAL_LOCKED ->
                                    "Next 5-minute social break unlocks in ${FocusCycleCalculator.formatDurationHms(status.phaseRemainingSec)}"
                                FocusPhase.FIVE_MIN_BREAK ->
                                    "Break active! Social media re-locks automatically in ${FocusCycleCalculator.formatDurationHms(status.phaseRemainingSec)}"
                                FocusPhase.GOAL_COMPLETED ->
                                    "Target reached! All social media apps are automatically re-enabled for today."
                                FocusPhase.NO_GOAL ->
                                    "Set today's study target to activate the 55-min lock / 5-min hourly break loop."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0),
                            textAlign = TextAlign.Center
                        )

                        if (status.hasActiveGoal) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Studied today: ${FocusCycleCalculator.formatHoursMinutes(status.totalStudiedTodaySec)} / ${FocusCycleCalculator.formatHoursMinutes(status.targetGoalSec)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = ForgeGold
                            )
                        }
                    }
                }
            }

            // Physical / Offline Study Mode Card ("Main Abhi Study Start Kar Raha Hu")
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (status.isPhysicalStudyRunning) EmeraldSuccess.copy(alpha = 0.65f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(18.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MenuBook,
                                    contentDescription = "Physical book study",
                                    tint = EmeraldSuccess
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Start Physical / Offline Study",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Timer keeps reducing your goal & logging study rank even when phone screen is off or studying from books.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (status.isPhysicalStudyRunning) {
                            Surface(
                                color = EmeraldSuccess.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = if (snapshot.activeSubject.isNotBlank()) {
                                                "Studying: ${snapshot.activeSubject}"
                                            } else {
                                                "Physical Study Session Live"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = EmeraldSuccess
                                        )
                                        Text(
                                            text = FocusCycleCalculator.formatDurationHms(status.liveSessionElapsedSec),
                                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.Timer,
                                        contentDescription = "Live timer",
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = onStopPhysicalStudy,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrimsonError,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("focus_stop_physical_button")
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Finish & Log Physical Study Time")
                            }
                        } else {
                            OutlinedTextField(
                                value = physicalSubjectInput,
                                onValueChange = { physicalSubjectInput = it },
                                label = { Text("What are you studying? (Optional)") },
                                placeholder = { Text("e.g., Physics NCERT Ch-3 / PYQ Book") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("focus_physical_subject_input")
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    onStartPhysicalStudy(physicalSubjectInput)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldSuccess,
                                    contentColor = Color(0xFF032217)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("focus_start_physical_button")
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("I'm Starting Study Now (Offline / Books)")
                            }
                        }
                    }
                }
            }

            // 1 Day / 1 Week / 1 Month Personal Study Time Summary
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Your Verified Study Time",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Combines in-app quizzes + physical study sessions (${profile.fullName.substringBefore(" ")})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StudyPeriodTile(
                                label = "1 Day",
                                value = FocusCycleCalculator.formatHoursMinutes(personalDaySec),
                                accent = ElectricCyan,
                                modifier = Modifier.weight(1f)
                            )
                            StudyPeriodTile(
                                label = "1 Week",
                                value = FocusCycleCalculator.formatHoursMinutes(personalWeekSec),
                                accent = ForgeGold,
                                modifier = Modifier.weight(1f)
                            )
                            StudyPeriodTile(
                                label = "1 Month",
                                value = FocusCycleCalculator.formatHoursMinutes(personalMonthSec),
                                accent = VioletAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Set / Update Daily Goal Card
            item {
                val chosenMinutes = (goalSliderMinutes.roundToLong() / 15L) * 15L
                val safeMinutes = chosenMinutes.coerceIn(30L, 720L)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Configure Daily Study Goal",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = FocusCycleCalculator.formatHoursMinutes(safeMinutes * 60L),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = ForgeGold
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(60L to "1 hr", 120L to "2 hr", 180L to "3 hr", 240L to "4 hr", 360L to "6 hr", 480L to "8 hr").forEach { (mins, lbl) ->
                                FilterChip(
                                    selected = safeMinutes == mins,
                                    onClick = { goalSliderMinutes = mins.toFloat() },
                                    label = { Text(lbl) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ForgeGold.copy(alpha = 0.22f),
                                        selectedLabelColor = ForgeGold
                                    )
                                )
                            }
                        }

                        Slider(
                            value = goalSliderMinutes,
                            onValueChange = { goalSliderMinutes = it },
                            valueRange = 30f..720f,
                            colors = SliderDefaults.colors(
                                thumbColor = ForgeGold,
                                activeTrackColor = ForgeGold
                            )
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    onSetDailyGoal(safeMinutes)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForgeGold,
                                    contentColor = Color(0xFF1A1000)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("focus_set_goal_button")
                            ) {
                                Text(
                                    text = if (status.hasActiveGoal) "Update Goal" else "Set Goal & Lock Social Apps",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            AnimatedVisibility(visible = status.hasActiveGoal) {
                                OutlinedButton(
                                    onClick = onClearDailyGoal,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("focus_clear_goal_button")
                                ) {
                                    Text("Reset")
                                }
                            }
                        }
                    }
                }
            }

            // Usage Access System Permission Banner
            item {
                Surface(
                    color = if (hasUsageAccess) EmeraldSuccess.copy(alpha = 0.1f)
                    else ForgeGold.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (hasUsageAccess) EmeraldSuccess.copy(alpha = 0.45f)
                            else ForgeGold.copy(alpha = 0.55f),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hasUsageAccess) Icons.Filled.Security else Icons.Filled.NotificationsActive,
                                contentDescription = "Usage access guard",
                                tint = if (hasUsageAccess) EmeraldSuccess else ForgeGold
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (hasUsageAccess) "App Guard Permission Active"
                                    else "Enable App Shield Access",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (hasUsageAccess) "Kaizu detects & blocks selected social apps during 55-min study windows."
                                    else "Grant Usage Access so Kaizu automatically shields social media apps until goal completion.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { SocialMediaCatalog.openUsageAccessSettings(context) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("focus_usage_access_button")
                        ) {
                            Text(if (hasUsageAccess) "Settings" else "Enable")
                        }
                    }
                }
            }

            // Social Media App Blocker List
            item {
                Text(
                    text = "Social Media Apps Guarded (${snapshot.blockedPackages.size} selected)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(socialApps, key = { it.packageName }) { app ->
                val isBlocked = app.packageName in snapshot.blockedPackages
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onToggleBlockedApp(app.packageName) }
                        .testTag("social_app_toggle_${app.packageName}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = app.displayName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                if (app.isInstalled) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = EmeraldSuccess.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Installed",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldSuccess,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = app.categoryTag,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isBlocked,
                            onCheckedChange = { onToggleBlockedApp(app.packageName) }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StudyPeriodTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = accent
            )
        }
    }
}
