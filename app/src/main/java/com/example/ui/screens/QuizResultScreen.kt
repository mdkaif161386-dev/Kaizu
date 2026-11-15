package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.QuestionReview
import com.example.model.QuizAttempt
import com.example.model.QuizDifficulty
import com.example.model.QuizMode
import com.example.model.savedQuestionDocId
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ForgeGold
import com.example.ui.theme.MegaObsidian

@Composable
fun QuizResultScreen(
    attempt: QuizAttempt,
    saveError: String? = null,
    bookmarkedQuestionIds: Set<String> = emptySet(),
    onToggleBookmark: (QuestionReview) -> Unit = {},
    onViewLeaderboard: () -> Unit,
    onDone: () -> Unit
) {
    BackHandler(onBack = onDone)

    val wrongOrSkipped = remember(attempt) {
        attempt.questionReviews.filter { !it.isCorrect }
    }
    val subjectStats = remember(attempt) {
        attempt.questionReviews.groupBy { it.subject }.map { (subj, items) ->
            val correct = items.count { it.isCorrect }
            Triple(subj, correct, items.size)
        }
    }
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    // Instant Score & Performance Hero Card
                    Surface(
                        color = MegaObsidian,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.5.dp,
                                Brush.linearGradient(listOf(ForgeGold, ElectricCyan)),
                                RoundedCornerShape(24.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (attempt.mode == QuizMode.MEGA.id) {
                                        "${attempt.examName} • MEGA RESULT"
                                    } else {
                                        "${attempt.examName} • QUIZ RESULT"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ForgeGold
                                )
                                Surface(
                                    color = ElectricCyan.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = QuizDifficulty.fromId(attempt.difficulty).label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "${attempt.accuracyPercent.toInt()}%",
                                    style = MaterialTheme.typography.displayLarge,
                                    color = Color.White
                                )
                                Column(modifier = Modifier.padding(bottom = 6.dp)) {
                                    Text(
                                        text = "+${attempt.scorePoints} pts",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = ForgeGold
                                    )
                                    Text(
                                        text = performanceVerdict(attempt.accuracyPercent),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ResultStatPill("Correct", "${attempt.correctCount}", EmeraldSuccess)
                                ResultStatPill("Wrong", "${attempt.wrongCount}", CrimsonError)
                                ResultStatPill("Skipped", "${attempt.skippedCount}", ForgeGold)
                                ResultStatPill("Time", formatDuration(attempt.timeTakenSec), ElectricCyan)
                            }
                        }
                    }
                }

                if (saveError != null) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = saveError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = ForgeGold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Wrong (${wrongOrSkipped.size})") },
                            modifier = Modifier.testTag("result_tab_wrong")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("All Solutions (${attempt.questionReviews.size})") },
                            modifier = Modifier.testTag("result_tab_all")
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Analysis") },
                            modifier = Modifier.testTag("result_tab_analysis")
                        )
                    }
                }

                when (selectedTab) {
                    0 -> {
                        if (wrongOrSkipped.isEmpty()) {
                            item {
                                Surface(
                                    color = EmeraldSuccess.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, EmeraldSuccess.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(18.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Verified,
                                            contentDescription = null,
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Zero Mistakes — Flawless Run!",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = EmeraldSuccess
                                            )
                                            Text(
                                                text = "Every question was answered accurately. Step up to a higher difficulty next!",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = wrongOrSkipped,
                                key = { idx, item -> "wrong_${idx}_${item.question.hashCode()}" }
                            ) { idx, item ->
                                val isSaved = remember(
                                    item.question,
                                    item.subject,
                                    item.chapter,
                                    bookmarkedQuestionIds
                                ) {
                                    savedQuestionDocId(
                                        item.question,
                                        item.subject,
                                        item.chapter
                                    ) in bookmarkedQuestionIds
                                }
                                QuestionSolutionCard(
                                    index = idx + 1,
                                    review = item,
                                    highlightWrongSection = true,
                                    isBookmarked = isSaved,
                                    onToggleBookmark = { onToggleBookmark(item) }
                                )
                            }
                        }
                    }

                    1 -> {
                        itemsIndexed(
                            items = attempt.questionReviews,
                            key = { idx, item -> "all_${idx}_${item.question.hashCode()}" }
                        ) { idx, item ->
                            val isSaved = remember(
                                item.question,
                                item.subject,
                                item.chapter,
                                bookmarkedQuestionIds
                            ) {
                                savedQuestionDocId(
                                    item.question,
                                    item.subject,
                                    item.chapter
                                ) in bookmarkedQuestionIds
                            }
                            QuestionSolutionCard(
                                index = idx + 1,
                                review = item,
                                highlightWrongSection = false,
                                isBookmarked = isSaved,
                                onToggleBookmark = { onToggleBookmark(item) }
                            )
                        }
                    }

                    2 -> {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(18.dp),
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Subject Accuracy Breakdown",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    subjectStats.forEach { (subject, correct, total) ->
                                        val pct = if (total > 0) (correct.toFloat() / total.toFloat()) else 0f
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(subject, style = MaterialTheme.typography.bodyMedium)
                                                Text(
                                                    text = "$correct / $total (${(pct * 100).toInt()}%)",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (pct >= 0.7f) EmeraldSuccess else ForgeGold
                                                )
                                            }
                                            LinearProgressIndicator(
                                                progress = { pct },
                                                color = if (pct >= 0.7f) EmeraldSuccess else ForgeGold,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(50))
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Chapters Covered: ${attempt.chapterSummary}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onViewLeaderboard,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("result_leaderboard_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = ForgeGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Leaderboard")
                }

                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForgeGold,
                        contentColor = Color(0xFF1A1000)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("result_done_button")
                ) {
                    Text(
                        text = "Back to Arena",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultStatPill(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun QuestionSolutionCard(
    index: Int,
    review: QuestionReview,
    highlightWrongSection: Boolean,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit
) {
    val statusColor = if (review.isCorrect) EmeraldSuccess else CrimsonError
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
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
                Text(
                    text = if (highlightWrongSection) "Mistake #$index • ${review.subject}" else "Q$index • ${review.subject}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (review.isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Error,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = when {
                            review.isCorrect -> "Correct"
                            review.selectedIndex == -1 -> "Skipped"
                            else -> "Wrong"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor
                    )
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("result_bookmark_$index")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Remove bookmark" else "Save question",
                            tint = if (isBookmarked) ForgeGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = review.question,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            review.options.forEachIndexed { optIdx, text ->
                val isCorrectOpt = optIdx == review.correctIndex
                val isUserWrongOpt = optIdx == review.selectedIndex && !review.isCorrect
                val optBg = when {
                    isCorrectOpt -> EmeraldSuccess.copy(alpha = 0.14f)
                    isUserWrongOpt -> CrimsonError.copy(alpha = 0.14f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                }
                val optTextColor = when {
                    isCorrectOpt -> EmeraldSuccess
                    isUserWrongOpt -> CrimsonError
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Surface(
                    color = optBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${'A' + optIdx}. $text",
                            style = MaterialTheme.typography.bodyMedium,
                            color = optTextColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (isCorrectOpt) {
                            Text("Correct", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                        } else if (isUserWrongOpt) {
                            Text("Your Pick", style = MaterialTheme.typography.labelSmall, color = CrimsonError)
                        }
                    }
                }
            }

            Surface(
                color = ForgeGold.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ForgeGold.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = null,
                        tint = ForgeGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Detailed Solution",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ForgeGold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = review.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private fun performanceVerdict(accuracy: Double): String = when {
    accuracy >= 90.0 -> "Top-1% Ranker Form!"
    accuracy >= 75.0 -> "Strong Command — Push Difficulty!"
    accuracy >= 50.0 -> "Solid Base — Drill Mistakes"
    else -> "Review Solutions & Retry"
}

fun formatDuration(totalSec: Long): String {
    val mins = totalSec / 60
    val secs = totalSec % 60
    return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}
