package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ExamCatalog
import com.example.model.QuizAttempt
import com.example.model.QuizMode
import com.example.model.StudyInsight
import com.example.model.UserProfile
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ForgeGold
import com.example.ui.theme.ForgeGoldBright
import com.example.ui.theme.MegaObsidian
import com.example.ui.theme.VioletAccent
import com.example.ui.viewmodel.InsightGenerationState
import com.example.util.StreakCalculator
import com.example.util.StreakSummary

@Composable
fun DashboardScreen(
    profile: UserProfile,
    attempts: List<QuizAttempt>,
    studyInsight: StudyInsight?,
    insightGenState: InsightGenerationState,
    onGenerateStudyPlan: () -> Unit,
    onOpenAttemptReview: (QuizAttempt) -> Unit,
    onGoToSubjects: () -> Unit
) {
    val exam = remember(profile.selectedExamId) { ExamCatalog.getExamById(profile.selectedExamId) }
    val subjectMastery = remember(attempts) {
        val map = mutableMapOf<String, Pair<Int, Int>>()
        attempts.forEach { attempt ->
            attempt.questionReviews.forEach { q ->
                val cur = map[q.subject] ?: (0 to 0)
                map[q.subject] = (cur.first + if (q.isCorrect) 1 else 0) to (cur.second + 1)
            }
        }
        map.entries.map { (subj, pair) ->
            val pct = if (pair.second > 0) (pair.first.toFloat() / pair.second.toFloat()) else 0f
            Triple(subj, pair.first, pair.second) to pct
        }.sortedByDescending { it.second }
    }

    val trendPoints = remember(attempts) {
        attempts.take(10).reversed().map { it.accuracyPercent.toFloat().coerceIn(0f, 100f) }
    }
    val streakSummary = remember(attempts) {
        StreakCalculator.calculate(attempts)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Performance Analytics",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${exam.name} Aspirant • Live Progress & AI Coach",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Daily Streak Counter Card
            item {
                DailyStreakCard(
                    streak = streakSummary,
                    onPracticeNow = onGoToSubjects
                )
            }

            // KPI 2x2 Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            title = "Total Score",
                            value = "${profile.totalPoints}",
                            subtitle = "Rank points",
                            accent = ForgeGold,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Accuracy",
                            value = "${profile.averageAccuracyPercent.toInt()}%",
                            subtitle = "${profile.totalCorrectAnswers}/${profile.totalQuestionsSolved} solved",
                            accent = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            title = "Quizzes",
                            value = "${profile.quizzesCompleted}",
                            subtitle = "Completed runs",
                            accent = ElectricCyan,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Mega Runs",
                            value = "${profile.megaQuizzesCompleted}",
                            subtitle = "Multi-subject arenas",
                            accent = VioletAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Accuracy Trend Chart
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Analytics,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Accuracy Trend (Last ${trendPoints.size.coerceAtLeast(1)} Quizzes)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (trendPoints.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Complete your first AI quiz to unlock accuracy curves.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            AccuracySparkChart(
                                points = trendPoints,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            )
                        }
                    }
                }
            }

            // Personalized AI Study Recommendations Card
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ForgeGold.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Psychology,
                                    contentDescription = null,
                                    tint = ForgeGold,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "AI Study Recommendations",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (attempts.isNotEmpty() && insightGenState !is InsightGenerationState.Loading) {
                                OutlinedButton(
                                    onClick = onGenerateStudyPlan,
                                    modifier = Modifier.testTag("refresh_ai_plan_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (studyInsight == null) "Analyze" else "Refresh")
                                }
                            }
                        }

                        if (insightGenState is InsightGenerationState.Loading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = ForgeGold,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "AI Coach is analyzing your wrong answers & speed...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (insightGenState is InsightGenerationState.Error) {
                            Text(
                                text = insightGenState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (studyInsight != null) {
                            Text(
                                text = studyInsight.headline,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = ForgeGold
                            )
                            studyInsight.recommendations.forEachIndexed { idx, rec ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "${idx + 1}.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan
                                    )
                                    Text(
                                        text = rec,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (studyInsight.focusTopics.isNotEmpty()) {
                                Text(
                                    text = "Priority Focus: ${studyInsight.focusTopics.joinToString(" • ")}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldSuccess
                                )
                            }
                        } else if (attempts.isEmpty()) {
                            Text(
                                text = "Take a chapter or Mega Quiz first — then tap Analyze for a personalized AI study roadmap.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onGoToSubjects,
                                colors = ButtonDefaults.buttonColors(containerColor = ForgeGold, contentColor = Color(0xFF1A1000)),
                                modifier = Modifier.testTag("dashboard_start_quiz_cta")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Take Your First Quiz")
                            }
                        } else if (insightGenState !is InsightGenerationState.Loading) {
                            Text(
                                text = "You have ${attempts.size} completed quiz(zes). Tap Analyze to generate tailored recommendations for ${exam.name}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Subject Mastery Breakdown
            if (subjectMastery.isNotEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Subject Mastery",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            subjectMastery.forEach { (triple, pct) ->
                                val (subj, correct, total) = triple
                                val barColor = when {
                                    pct >= 0.75f -> EmeraldSuccess
                                    pct >= 0.5f -> ForgeGold
                                    else -> ElectricCyan
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(subj, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = "$correct/$total (${(pct * 100).toInt()}%)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = barColor
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        color = barColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(50))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Recent Quiz Attempts",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (attempts.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No quiz attempts yet. Start a chapter drill or Mega Quiz from the Arena tab.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(attempts, key = { it.id.ifBlank { it.hashCode().toString() } }) { attempt ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onOpenAttemptReview(attempt) }
                            .minimumInteractiveComponentSize()
                            .testTag("attempt_history_${attempt.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (attempt.mode == QuizMode.MEGA.id) ForgeGold.copy(alpha = 0.18f)
                                        else ElectricCyan.copy(alpha = 0.16f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (attempt.mode == QuizMode.MEGA.id) {
                                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = ForgeGold)
                                } else {
                                    Text(
                                        text = "${attempt.accuracyPercent.toInt()}%",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ElectricCyan
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (attempt.mode == QuizMode.MEGA.id) {
                                        "${attempt.examName} Mega Quiz"
                                    } else {
                                        "${attempt.subjectNames.joinToString()} • ${attempt.chapterSummary}"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${attempt.correctCount}/${attempt.totalQuestions} correct • ${attempt.difficulty} • +${attempt.scorePoints} pts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View solutions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccuracySparkChart(
    points: List<Float>,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val pad = 16f

        // Horizontal reference lines (0%, 50%, 100%)
        listOf(0f, 0.5f, 1f).forEach { ratio ->
            val y = pad + (h - 2 * pad) * ratio
            drawLine(
                color = gridColor,
                start = Offset(pad, y),
                end = Offset(w - pad, y),
                strokeWidth = 1.5f
            )
        }

        if (points.size == 1) {
            val y = pad + (h - 2 * pad) * (1f - points.first() / 100f)
            drawCircle(
                color = ForgeGold,
                radius = 9f,
                center = Offset(w / 2f, y)
            )
            return@Canvas
        }

        val stepX = (w - 2 * pad) / (points.size - 1).coerceAtLeast(1)
        val path = Path()
        val coords = points.mapIndexed { idx, pct ->
            val x = pad + idx * stepX
            val y = pad + (h - 2 * pad) * (1f - pct / 100f)
            Offset(x, y)
        }

        coords.forEachIndexed { idx, pt ->
            if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(ElectricCyan, ForgeGold)),
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )

        coords.forEach { pt ->
            drawCircle(color = ForgeGold, radius = 7f, center = pt)
            drawCircle(color = Color.White, radius = 3f, center = pt)
        }
    }
}

@Composable
private fun DailyStreakCard(
    streak: StreakSummary,
    onPracticeNow: () -> Unit
) {
    val isActive = streak.currentStreak > 0
    val flameColor = if (isActive) Color(0xFFFF6B2B) else MaterialTheme.colorScheme.onSurfaceVariant
    val borderBrush = if (isActive) {
        Brush.linearGradient(listOf(Color(0xFFFF6B2B), ForgeGoldBright))
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
    }

    Surface(
        color = if (isActive) MegaObsidian else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderBrush, RoundedCornerShape(22.dp))
            .testTag("daily_streak_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        flameColor.copy(alpha = 0.32f),
                                        ForgeGold.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                flameColor.copy(alpha = if (isActive) 0.65f else 0.25f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "Daily practice streak flame",
                            tint = flameColor,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${streak.currentStreak}",
                                style = MaterialTheme.typography.displayLarge,
                                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("daily_streak_count")
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (streak.currentStreak == 1) "day streak" else "days streak",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isActive) ForgeGoldBright else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 5.dp)
                            )
                        }
                        Text(
                            text = when {
                                streak.practicedToday && streak.currentStreak >= 3 ->
                                    "Unstoppable momentum! Come back tomorrow."
                                streak.practicedToday ->
                                    "Today's goal locked in! Keep the fire lit."
                                streak.currentStreak > 0 ->
                                    "Complete 1 quiz today to extend your streak!"
                                else ->
                                    "Finish a quiz today to ignite your streak!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isActive) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = ForgeGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "BEST",
                            style = MaterialTheme.typography.labelSmall,
                            color = ForgeGold
                        )
                        Text(
                            text = "${streak.longestStreak}d",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 7-day consistency indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                streak.lastSevenDays.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        day.completed -> Color(0xFFFF6B2B).copy(alpha = 0.24f)
                                        day.isToday -> ForgeGold.copy(alpha = 0.12f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    }
                                )
                                .border(
                                    width = if (day.isToday) 1.5.dp else 1.dp,
                                    color = when {
                                        day.completed -> Color(0xFFFF6B2B)
                                        day.isToday -> ForgeGold
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day.completed) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Practiced",
                                    tint = ForgeGoldBright,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = day.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                day.isToday -> ForgeGold
                                isActive -> Color(0xFF94A3B8)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            if (!streak.practicedToday) {
                Button(
                    onClick = onPracticeNow,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForgeGold,
                        contentColor = Color(0xFF1A1000)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("streak_practice_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (streak.currentStreak > 0) "Extend Streak Now" else "Start Streak Today",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

