package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CompetitiveExam
import com.example.model.ExamCatalog
import com.example.model.ExamSubject
import com.example.model.QuizAttempt
import com.example.model.UserProfile
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ForgeGold
import com.example.ui.theme.ForgeGoldBright
import com.example.ui.theme.MegaObsidian
import com.example.ui.theme.VioletAccent
import com.example.util.StreakCalculator

@Composable
fun SubjectsScreen(
    profile: UserProfile,
    attempts: List<QuizAttempt>,
    onSubjectClick: (ExamSubject) -> Unit,
    onMegaQuizClick: () -> Unit,
    onFlashcardsClick: () -> Unit,
    onSwitchExam: (CompetitiveExam) -> Unit,
    onStreakClick: () -> Unit,
    onProfileClick: () -> Unit = {}
) {
    val exam = remember(profile.selectedExamId) {
        ExamCatalog.getExamById(profile.selectedExamId)
    }
    val streakSummary = remember(attempts) {
        StreakCalculator.calculate(attempts)
    }
    var showExamDropdown by remember { mutableStateOf(false) }

    val subjectAccents = listOf(ElectricCyan, ForgeGold, EmeraldSuccess, VioletAccent)

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
                // Header Greeting + Active Exam Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Welcome back, ${profile.fullName.substringBefore(" ")}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Pick a subject to drill syllabus chapters or launch a Mega Quiz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box {
                            Surface(
                                color = ForgeGold.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, ForgeGold.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                                    .clickable { showExamDropdown = true }
                                    .minimumInteractiveComponentSize()
                                    .testTag("switch_exam_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = exam.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ForgeGold
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Filled.SwapHoriz,
                                        contentDescription = "Switch Competitive Exam",
                                        tint = ForgeGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showExamDropdown,
                                onDismissRequest = { showExamDropdown = false }
                            ) {
                                ExamCatalog.exams.forEach { candidate ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = candidate.name,
                                                    fontWeight = if (candidate.id == exam.id) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = candidate.category,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            showExamDropdown = false
                                            onSwitchExam(candidate)
                                        }
                                    )
                                }
                            }
                        }

                        Surface(
                            color = ElectricCyan.copy(alpha = 0.16f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.dp, ElectricCyan.copy(alpha = 0.55f), CircleShape)
                                .clickable(onClick = onProfileClick)
                                .minimumInteractiveComponentSize()
                                .testTag("subjects_profile_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = profile.fullName.trim().take(1).uppercase().ifBlank { "S" },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ElectricCyan
                                )
                            }
                        }
                    }
                }
            }

            item {
                // Daily Streak Quick Pill (taps through to Dashboard streak card)
                val isStreakActive = streakSummary.currentStreak > 0
                val flameTint = if (isStreakActive) Color(0xFFFF6B2B) else MaterialTheme.colorScheme.onSurfaceVariant
                Surface(
                    color = if (isStreakActive) MegaObsidian else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            color = if (isStreakActive) Color(0xFFFF6B2B).copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(onClick = onStreakClick)
                        .minimumInteractiveComponentSize()
                        .testTag("subjects_streak_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(flameTint.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = "Daily practice streak flame",
                                    tint = flameTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${streakSummary.currentStreak} Day Streak",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isStreakActive) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when {
                                        streakSummary.practicedToday -> "Practiced today • Best ${streakSummary.longestStreak}d"
                                        streakSummary.currentStreak > 0 -> "Complete a quiz today to extend your streak"
                                        else -> "Complete 1 quiz today to ignite your streak"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isStreakActive) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Dashboard streak details",
                            tint = if (isStreakActive) ForgeGoldBright else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            item {
                // Quick Stats Strip
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(18.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MiniStatItem(
                            label = "Points",
                            value = profile.totalPoints.toString(),
                            accent = ForgeGold
                        )
                        MiniStatItem(
                            label = "Quizzes",
                            value = profile.quizzesCompleted.toString(),
                            accent = ElectricCyan
                        )
                        MiniStatItem(
                            label = "Accuracy",
                            value = "${profile.averageAccuracyPercent.toInt()}%",
                            accent = EmeraldSuccess
                        )
                        MiniStatItem(
                            label = "Mega Runs",
                            value = profile.megaQuizzesCompleted.toString(),
                            accent = VioletAccent
                        )
                    }
                }
            }

            item {
                // AI Flashcards Studio Entry Card
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            1.dp,
                            VioletAccent.copy(alpha = 0.5f),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable(onClick = onFlashcardsClick)
                        .minimumInteractiveComponentSize()
                        .testTag("subjects_flashcards_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(VioletAccent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Style,
                                contentDescription = "AI Flashcards Studio",
                                tint = VioletAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Flashcards Studio",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Generate interactive concept & formula decks for any chapters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open AI Flashcards Studio",
                            tint = VioletAccent
                        )
                    }
                }
            }

            item {
                Text(
                    text = "${exam.name} Subjects",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            itemsIndexed(exam.subjects, key = { _, s -> s.id }) { index, subject ->
                val accent = subjectAccents[index % subjectAccents.size]
                SubjectCard(
                    subject = subject,
                    accent = accent,
                    onClick = { onSubjectClick(subject) }
                )
            }

            // Last item: Mega Quiz Option as explicitly requested!
            item {
                Spacer(Modifier.height(4.dp))
                MegaQuizEntryCard(
                    exam = exam,
                    onClick = onMegaQuizClick
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun MiniStatItem(
    label: String,
    value: String,
    accent: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SubjectCard(
    subject: ExamSubject,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .testTag("subject_card_${subject.id}")
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subject.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${subject.chapters.size} Syllabus Chapters",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open ${subject.name} chapters",
                tint = accent
            )
        }
    }
}

@Composable
private fun MegaQuizEntryCard(
    exam: CompetitiveExam,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(ForgeGold, ElectricCyan)),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .testTag("mega_quiz_entry_card")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_mega_arena),
            contentDescription = "Mega Quiz Arena",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MegaObsidian.copy(alpha = 0.92f),
                            MegaObsidian.copy(alpha = 0.72f)
                        )
                    )
                )
        )
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ForgeGold.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = ForgeGold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Surface(
                    color = ForgeGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = ForgeGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "MEGA QUIZ ARENA",
                            style = MaterialTheme.typography.labelSmall,
                            color = ForgeGold
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Cross-Subject Mega Quiz",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Pick chapters across all ${exam.subjects.size} ${exam.name} subjects, set your hard level & timer, and compete on the exclusive Mega Leaderboard.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1)
            )
            Spacer(Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Build Mega Quiz",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ForgeGold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = ForgeGold,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
