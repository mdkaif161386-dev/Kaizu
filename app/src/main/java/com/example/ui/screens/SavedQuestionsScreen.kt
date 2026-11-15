package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.QuizDifficulty
import com.example.model.SavedQuestion
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ForgeGold

@Composable
fun SavedQuestionsScreen(
    savedQuestions: List<SavedQuestion>,
    onRemoveBookmark: (String) -> Unit,
    onGoToArena: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("All") }

    val availableSubjects = remember(savedQuestions) {
        listOf("All") + savedQuestions.map { it.subject }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredQuestions = remember(savedQuestions, searchQuery, selectedSubject) {
        savedQuestions.filter { item ->
            val matchesSubject = selectedSubject == "All" || item.subject == selectedSubject
            val matchesQuery = searchQuery.isBlank() ||
                item.question.contains(searchQuery, ignoreCase = true) ||
                item.chapter.contains(searchQuery, ignoreCase = true) ||
                item.subject.contains(searchQuery, ignoreCase = true)
            matchesSubject && matchesQuery
        }
    }

    val revealedMap = remember { mutableStateMapOf<String, Boolean>() }
    val selfTestSelection = remember { mutableStateMapOf<String, Int>() }

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
                            text = "Saved Questions",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Revise difficult & important MCQs bookmarked during quizzes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = ForgeGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.border(
                            1.dp,
                            ForgeGold.copy(alpha = 0.45f),
                            RoundedCornerShape(50)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = null,
                                tint = ForgeGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${savedQuestions.size}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = ForgeGold
                            )
                        }
                    }
                }
            }

            if (savedQuestions.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(22.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                RoundedCornerShape(22.dp)
                            )
                            .testTag("saved_questions_empty_state")
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(ForgeGold.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = ForgeGold,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "No Saved Questions Yet",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap the bookmark icon during any quiz or solution review to save tricky questions here for focused revision.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = onGoToArena,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForgeGold,
                                    contentColor = Color(0xFF1A1000)
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("saved_empty_go_arena_button")
                            ) {
                                Text("Practice a Quiz", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search saved questions or chapters...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("saved_questions_search")
                    )
                }

                if (availableSubjects.size > 2) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableSubjects) { subj ->
                                FilterChip(
                                    selected = selectedSubject == subj,
                                    onClick = { selectedSubject = subj },
                                    label = { Text(subj) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = ElectricCyan
                                    ),
                                    modifier = Modifier.testTag("saved_subject_filter_$subj")
                                )
                            }
                        }
                    }
                }

                if (filteredQuestions.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No saved questions match your current filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                } else {
                    items(filteredQuestions, key = { it.id }) { item ->
                        val isRevealed = revealedMap[item.id] == true
                        val selectedPick = selfTestSelection[item.id]
                        SavedQuestionRevisionCard(
                            item = item,
                            isSolutionRevealed = isRevealed,
                            selectedOptionIndex = selectedPick,
                            onSelectOption = { optIdx ->
                                selfTestSelection[item.id] = optIdx
                                revealedMap[item.id] = true
                            },
                            onToggleReveal = {
                                revealedMap[item.id] = !isRevealed
                            },
                            onRemove = { onRemoveBookmark(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedQuestionRevisionCard(
    item: SavedQuestion,
    isSolutionRevealed: Boolean,
    selectedOptionIndex: Int?,
    onSelectOption: (Int) -> Unit,
    onToggleReveal: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                ForgeGold.copy(alpha = 0.4f),
                RoundedCornerShape(20.dp)
            )
            .testTag("saved_question_card_${item.id}")
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
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = ElectricCyan.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${item.subject} • ${item.chapter}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = ForgeGold.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = QuizDifficulty.fromId(item.difficulty).label,
                            style = MaterialTheme.typography.labelSmall,
                            color = ForgeGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("remove_bookmark_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.BookmarkRemove,
                        contentDescription = "Remove from Saved Questions",
                        tint = ForgeGold
                    )
                }
            }

            Text(
                text = item.question,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            item.options.forEachIndexed { idx, optText ->
                val isCorrectOpt = idx == item.correctIndex
                val isUserWrongOpt = selectedOptionIndex == idx && !isCorrectOpt
                val bgColor = when {
                    isSolutionRevealed && isCorrectOpt -> EmeraldSuccess.copy(alpha = 0.16f)
                    isSolutionRevealed && isUserWrongOpt -> CrimsonError.copy(alpha = 0.16f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                }
                val borderColor = when {
                    isSolutionRevealed && isCorrectOpt -> EmeraldSuccess
                    isSolutionRevealed && isUserWrongOpt -> CrimsonError
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                }
                val textColor = when {
                    isSolutionRevealed && isCorrectOpt -> EmeraldSuccess
                    isSolutionRevealed && isUserWrongOpt -> CrimsonError
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable { onSelectOption(idx) }
                        .minimumInteractiveComponentSize()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${'A' + idx}. $optText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSolutionRevealed && isCorrectOpt) {
                            Text(
                                text = "Answer",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess
                            )
                        } else if (isSolutionRevealed && isUserWrongOpt) {
                            Text(
                                text = "Your Try",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonError
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onToggleReveal,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("toggle_solution_${item.id}")
            ) {
                Icon(
                    imageVector = if (isSolutionRevealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isSolutionRevealed) "Hide Answer & Solution" else "Reveal Answer & Solution",
                    color = ElectricCyan
                )
            }

            AnimatedVisibility(visible = isSolutionRevealed) {
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
                                text = item.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
