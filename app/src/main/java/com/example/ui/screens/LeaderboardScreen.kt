package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.ExamCatalog
import com.example.model.LeaderboardEntry
import com.example.model.MegaLeaderboardEntry
import com.example.model.QuizDifficulty
import com.example.model.StudyTimeLeaderboardEntry
import com.example.model.StudyTimePeriod
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ForgeGold
import com.example.ui.theme.ForgeGoldBright
import com.example.ui.theme.MegaBronze
import com.example.ui.theme.MegaCardSurface
import com.example.ui.theme.MegaObsidian
import com.example.ui.theme.MegaSilver
import com.example.ui.theme.VioletAccent
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.LeaderboardUiState
import com.example.util.FocusCycleCalculator
import com.example.util.FocusRuntimeStatus
import kotlinx.coroutines.flow.StateFlow

@Composable
fun LeaderboardScreen(
    currentUserId: String,
    selectedExamId: String,
    onSelectExam: (String) -> Unit,
    examLeaderboardState: LeaderboardUiState<LeaderboardEntry>,
    megaLeaderboardState: LeaderboardUiState<MegaLeaderboardEntry>,
    studyTimeLeaderboardState: LeaderboardUiState<StudyTimeLeaderboardEntry> = LeaderboardUiState.Loading,
    liveSessionExtraSec: Long = 0L,
    focusRuntimeStatusFlow: StateFlow<FocusRuntimeStatus>? = null
) {
    // 0 = Mega Quiz Championship (Premium), 1 = Exam Leaderboard, 2 = Study Time (1D/1W/1M)
    var modeTab by remember { mutableIntStateOf(0) }
    val selectedExam = remember(selectedExamId) { ExamCatalog.getExamById(selectedExamId) }

    val containerBg = if (modeTab == 0) MegaObsidian else MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(containerBg),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            // Mode Switcher Header
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (modeTab == 0) MegaCardSurface else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            1.dp,
                            if (modeTab == 0) ForgeGold.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(4.dp)
                ) {
                    ModeTogglePill(
                        label = "Mega Arena",
                        icon = Icons.Filled.WorkspacePremium,
                        selected = modeTab == 0,
                        isGold = true,
                        onClick = { modeTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("leaderboard_mode_mega")
                    )
                    ModeTogglePill(
                        label = "Exam",
                        icon = Icons.Filled.EmojiEvents,
                        selected = modeTab == 1,
                        isGold = false,
                        onClick = { modeTab = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("leaderboard_mode_exam")
                    )
                    ModeTogglePill(
                        label = "Study Time",
                        icon = Icons.Filled.Schedule,
                        selected = modeTab == 2,
                        isGold = false,
                        onClick = { modeTab = 2 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("leaderboard_mode_time")
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Competitive Exam Filter Strip (separate leaderboard per competitive exam!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExamCatalog.exams.forEach { exam ->
                        val isSelected = exam.id == selectedExamId
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectExam(exam.id) },
                            label = {
                                Text(
                                    text = exam.name,
                                    color = when {
                                        isSelected -> if (modeTab == 0) Color(0xFF1A1000) else ForgeGold
                                        modeTab == 0 -> Color(0xFFCBD5E1)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (modeTab == 0) ForgeGold else ForgeGold.copy(alpha = 0.2f),
                                containerColor = if (modeTab == 0) MegaCardSurface else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.testTag("lb_exam_chip_${exam.id}")
                        )
                    }
                }
            }

            when (modeTab) {
                0 -> MegaLeaderboardContent(
                    examName = selectedExam.name,
                    currentUserId = currentUserId,
                    state = megaLeaderboardState
                )
                1 -> ExamLeaderboardContent(
                    examName = selectedExam.name,
                    currentUserId = currentUserId,
                    state = examLeaderboardState
                )
                else -> StudyTimeLeaderboardContent(
                    selectedExamId = selectedExamId,
                    examName = selectedExam.name,
                    currentUserId = currentUserId,
                    liveSessionExtraSec = liveSessionExtraSec,
                    focusRuntimeStatusFlow = focusRuntimeStatusFlow,
                    state = studyTimeLeaderboardState
                )
            }
        }
    }
}

@Composable
private fun ModeTogglePill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    isGold: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        selected && isGold -> Brush.horizontalGradient(listOf(ForgeGold, ForgeGoldBright))
        selected -> Brush.horizontalGradient(listOf(ElectricCyan, ElectricCyan))
        else -> Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    val textColor = when {
        selected -> Color(0xFF0B1120)
        isGold -> Color(0xFFE2E8F0)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
    }
}

@Composable
private fun MegaLeaderboardContent(
    examName: String,
    currentUserId: String,
    state: LeaderboardUiState<MegaLeaderboardEntry>
) {
    when (state) {
        is LeaderboardUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ForgeGold)
            }
        }

        is LeaderboardUiState.Error -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
        }

        is LeaderboardUiState.Success -> {
            val entries = state.entries
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Ultra-Premium Mega Championship Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .border(
                                1.5.dp,
                                Brush.linearGradient(listOf(ForgeGold, ElectricCyan, ForgeGold)),
                                RoundedCornerShape(22.dp)
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_mega_arena),
                            contentDescription = "Mega Leaderboard Crown",
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(MegaObsidian.copy(alpha = 0.55f), MegaObsidian.copy(alpha = 0.92f))
                                    )
                                )
                        )
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = ForgeGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "$examName CHAMPIONSHIP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ForgeGold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Mega Quiz Hall of Fame",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Ranked by cumulative Mega Quiz points, accuracy & speed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }

                if (entries.isEmpty()) {
                    item {
                        Surface(
                            color = MegaCardSurface,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ForgeGold.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WorkspacePremium,
                                    contentDescription = null,
                                    tint = ForgeGold,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "Claim the #1 Crown for $examName",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Complete a multi-subject Mega Quiz to appear on this elite board.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Top 3 Championship Podium
                    item {
                        MegaPodiumRow(entries.take(3), currentUserId)
                    }

                    itemsIndexed(entries, key = { _, e -> e.userId }) { idx, entry ->
                        val isMe = entry.userId == currentUserId
                        val rankColor = when (idx) {
                            0 -> ForgeGold
                            1 -> MegaSilver
                            2 -> MegaBronze
                            else -> ElectricCyan
                        }
                        Surface(
                            color = if (isMe) ForgeGold.copy(alpha = 0.16f) else MegaCardSurface,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    if (isMe || idx == 0) 1.5.dp else 1.dp,
                                    if (isMe) ForgeGold else rankColor.copy(alpha = 0.3f),
                                    RoundedCornerShape(18.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(rankColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${idx + 1}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = rankColor
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = entry.displayName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isMe) {
                                            Spacer(Modifier.width(6.dp))
                                            Surface(
                                                color = ForgeGold,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "YOU",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF1A1000),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${QuizDifficulty.fromId(entry.highestDifficulty).label} • Best ${entry.bestAccuracy.toInt()}% • ${entry.megaQuizzesCount} Mega",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${entry.scorePoints}",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = ForgeGold
                                    )
                                    Text(
                                        text = "pts",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MegaPodiumRow(
    topEntries: List<MegaLeaderboardEntry>,
    currentUserId: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val second = topEntries.getOrNull(1)
        val first = topEntries.getOrNull(0)
        val third = topEntries.getOrNull(2)

        if (second != null) {
            PodiumSlot(
                rank = 2,
                entry = second,
                isMe = second.userId == currentUserId,
                accent = MegaSilver,
                heightDp = 128,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        if (first != null) {
            PodiumSlot(
                rank = 1,
                entry = first,
                isMe = first.userId == currentUserId,
                accent = ForgeGold,
                heightDp = 152,
                modifier = Modifier.weight(1.1f)
            )
        }

        if (third != null) {
            PodiumSlot(
                rank = 3,
                entry = third,
                isMe = third.userId == currentUserId,
                accent = MegaBronze,
                heightDp = 118,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun PodiumSlot(
    rank: Int,
    entry: MegaLeaderboardEntry,
    isMe: Boolean,
    accent: Color,
    heightDp: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MegaCardSurface,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .height(heightDp.dp)
            .border(
                if (rank == 1 || isMe) 1.5.dp else 1.dp,
                accent.copy(alpha = 0.75f),
                RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MilitaryTech,
                contentDescription = "Rank $rank",
                tint = accent,
                modifier = Modifier.size(if (rank == 1) 30.dp else 24.dp)
            )
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = entry.displayName.substringBefore(" "),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${entry.scorePoints} pts",
                style = MaterialTheme.typography.labelMedium,
                color = accent
            )
        }
    }
}

@Composable
private fun ExamLeaderboardContent(
    examName: String,
    currentUserId: String,
    state: LeaderboardUiState<LeaderboardEntry>
) {
    when (state) {
        is LeaderboardUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ElectricCyan)
            }
        }

        is LeaderboardUiState.Error -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
        }

        is LeaderboardUiState.Success -> {
            val entries = state.entries
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "$examName Aspirants Leaderboard",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Dedicated rankings for $examName across all chapter & Mega quizzes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (entries.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No participants ranked yet for $examName. Complete a quiz to claim #1!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    itemsIndexed(entries, key = { _, e -> e.userId }) { idx, entry ->
                        val isMe = entry.userId == currentUserId
                        val accent = when (idx) {
                            0 -> ForgeGold
                            1 -> ElectricCyan
                            2 -> EmeraldSuccess
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            color = if (isMe) ForgeGold.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    if (isMe) 1.5.dp else 1.dp,
                                    if (isMe) ForgeGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${idx + 1}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = accent
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isMe) "${entry.displayName} (You)" else entry.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${entry.quizzesCount} quizzes • Best ${entry.bestAccuracy.toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${entry.scorePoints} pts",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (idx == 0) ForgeGold else ElectricCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyTimeLeaderboardContent(
    selectedExamId: String,
    examName: String,
    currentUserId: String,
    liveSessionExtraSec: Long,
    focusRuntimeStatusFlow: StateFlow<FocusRuntimeStatus>?,
    state: LeaderboardUiState<StudyTimeLeaderboardEntry>
) {
    var selectedPeriod by remember { mutableStateOf(StudyTimePeriod.ONE_DAY) }
    var filterBySelectedExam by remember { mutableStateOf(false) }
    val observedLiveExtraSec = focusRuntimeStatusFlow?.collectAsStateWithLifecycle()?.value?.liveSessionElapsedSec
    val effectiveLiveExtraSec = observedLiveExtraSec ?: liveSessionExtraSec

    when (state) {
        is LeaderboardUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ElectricCyan)
            }
        }

        is LeaderboardUiState.Error -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
        }

        is LeaderboardUiState.Success -> {
            val nowMs = System.currentTimeMillis()
            val dayKey = FocusCycleCalculator.dateKeyFor(nowMs)
            val weekKey = FocusCycleCalculator.weekKeyFor(nowMs)
            val monthKey = FocusCycleCalculator.monthKeyFor(nowMs)

            val myEntry = remember(state.entries, currentUserId) {
                state.entries.firstOrNull { it.userId == currentUserId }
            }
            val (myDaySec, myWeekSec, myMonthSec) = FocusViewModel.personalTimeBreakdown(
                entry = myEntry,
                liveExtraSec = effectiveLiveExtraSec,
                nowMs = nowMs
            )

            // Quantize above 60s to minute boundaries since list rows show hours/minutes
            val sortTrigger = if (effectiveLiveExtraSec < 60L) effectiveLiveExtraSec else 60L + (effectiveLiveExtraSec / 60L)
            val ranked = remember(state.entries, selectedPeriod, filterBySelectedExam, selectedExamId, sortTrigger) {
                state.entries
                    .filter { !filterBySelectedExam || it.examId == selectedExamId }
                    .map { entry ->
                        val extra = if (entry.userId == currentUserId) effectiveLiveExtraSec else 0L
                        val activeSeconds = when (selectedPeriod) {
                            StudyTimePeriod.ONE_DAY ->
                                (if (entry.dayKey == dayKey) entry.daySeconds else 0L) + extra
                            StudyTimePeriod.ONE_WEEK ->
                                (if (entry.weekKey == weekKey) entry.weekSeconds else 0L) + extra
                            StudyTimePeriod.ONE_MONTH ->
                                (if (entry.monthKey == monthKey) entry.monthSeconds else 0L) + extra
                        }
                        entry to activeSeconds
                    }
                    .filter { it.second > 0L || it.first.userId == currentUserId }
                    .sortedByDescending { it.second }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Your Study Time Tracker",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TimeSummaryBox("1 Day", FocusCycleCalculator.formatHoursMinutes(myDaySec), ElectricCyan, Modifier.weight(1f))
                                TimeSummaryBox("1 Week", FocusCycleCalculator.formatHoursMinutes(myWeekSec), ForgeGold, Modifier.weight(1f))
                                TimeSummaryBox("1 Month", FocusCycleCalculator.formatHoursMinutes(myMonthSec), VioletAccent, Modifier.weight(1f))
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StudyTimePeriod.entries.forEach { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period },
                                label = { Text(period.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan
                                ),
                                modifier = Modifier.testTag("time_lb_period_${period.name.lowercase()}")
                            )
                        }
                        FilterChip(
                            selected = filterBySelectedExam,
                            onClick = { filterBySelectedExam = !filterBySelectedExam },
                            label = { Text(if (filterBySelectedExam) examName else "All Exams") }
                        )
                    }
                }

                if (ranked.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No study time logged for ${selectedPeriod.badgeText} yet. Complete a quiz or start Physical Study in the Focus tab!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    itemsIndexed(ranked, key = { _, pair -> pair.first.userId }) { idx, (entry, seconds) ->
                        val isMe = entry.userId == currentUserId
                        val accent = when (idx) {
                            0 -> ForgeGold
                            1 -> ElectricCyan
                            2 -> EmeraldSuccess
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            color = if (isMe) ElectricCyan.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    if (isMe) 1.5.dp else 1.dp,
                                    if (isMe) ElectricCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${idx + 1}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = accent
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isMe) "${entry.displayName} (You)" else entry.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "${entry.examName} • ${selectedPeriod.badgeText}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = FocusCycleCalculator.formatHoursMinutes(seconds),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = accent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeSummaryBox(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = accent
            )
        }
    }
}

