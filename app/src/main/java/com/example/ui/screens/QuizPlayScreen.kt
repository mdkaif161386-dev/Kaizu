package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.GeneratedQuestion
import com.example.model.QuizMode
import com.example.model.savedQuestionDocId
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ForgeGold
import com.example.ui.viewmodel.QuizSessionState
import com.example.util.QuizTimerAlertPolicy
import com.example.util.QuizTimerSoundPlayer

@Composable
fun QuizPlayScreen(
    sessionState: QuizSessionState,
    bookmarkedQuestionIds: Set<String> = emptySet(),
    onToggleBookmark: (GeneratedQuestion) -> Unit = {},
    onSelectOption: (Int) -> Unit,
    onAdvanceNext: () -> Unit,
    onFinishEarly: () -> Unit,
    onRetryGeneration: () -> Unit,
    onCancelAndExit: () -> Unit
) {
    BackHandler {
        onCancelAndExit()
    }

    val soundPlayer = remember { QuizTimerSoundPlayer() }
    var isSoundEnabled by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose { soundPlayer.release() }
    }

    if (sessionState is QuizSessionState.Playing) {
        LaunchedEffect(
            sessionState.currentIndex,
            sessionState.remainingSecForQuestion,
            isSoundEnabled
        ) {
            if (isSoundEnabled) {
                val tone = QuizTimerAlertPolicy.resolveTone(
                    remainingSec = sessionState.remainingSecForQuestion,
                    totalSecForQuestion = sessionState.config.timePerQuestionSec
                )
                soundPlayer.play(tone)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        when (sessionState) {
            is QuizSessionState.Generating -> {
                Column(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(ForgeGold.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            color = ForgeGold,
                            strokeWidth = 4.dp
                        )
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = ForgeGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = "Forging Your AI Quiz...",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Please wait a few moments while AI crafts ${sessionState.config.questionCount} ${sessionState.config.difficulty.label} MCQs with step-by-step solutions for ${sessionState.config.exam.name}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    LinearProgressIndicator(
                        progress = {
                            if (sessionState.totalBatches > 0) {
                                (sessionState.completedBatches.toFloat() / sessionState.totalBatches.toFloat())
                                    .coerceIn(0.15f, 1f)
                            } else 0.25f
                        },
                        color = ForgeGold,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )

                    Text(
                        text = "Batch ${minOf(sessionState.completedBatches + 1, sessionState.totalBatches)} of ${sessionState.totalBatches} • ${sessionState.config.timePerQuestionSec}s/question",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )

                    OutlinedButton(
                        onClick = onCancelAndExit,
                        modifier = Modifier.testTag("cancel_generation_button")
                    ) {
                        Text("Cancel")
                    }
                }
            }

            is QuizSessionState.Submitting -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(color = ForgeGold)
                    Text(
                        text = "Calculating rank score & syncing leaderboard...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            is QuizSessionState.Error -> {
                Column(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = CrimsonError,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "Quiz Generation Failed",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = sessionState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onCancelAndExit) {
                            Text("Back")
                        }
                        Button(
                            onClick = onRetryGeneration,
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeGold, contentColor = Color(0xFF1A1000))
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            is QuizSessionState.Playing -> {
                val question = sessionState.questions[sessionState.currentIndex]
                val selectedOption = sessionState.selectedAnswers[sessionState.currentIndex]
                val isLastQuestion = sessionState.currentIndex == sessionState.questions.lastIndex
                val remainingSec = sessionState.remainingSecForQuestion
                val isThirtySecWarning = QuizTimerAlertPolicy.isWarningActive(remainingSec)
                val isFinalTenUrgent = QuizTimerAlertPolicy.isUrgentActive(remainingSec)
                val timeFraction = (remainingSec.toFloat() /
                    sessionState.config.timePerQuestionSec.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                val timerColor by animateColorAsState(
                    targetValue = when {
                        isFinalTenUrgent -> CrimsonError
                        isThirtySecWarning -> ForgeGold
                        else -> ElectricCyan
                    },
                    label = "timerColor"
                )
                val pulseScale = if (isThirtySecWarning) {
                    val pulseTransition = rememberInfiniteTransition(label = "timerWarningPulse")
                    val animatedScale by pulseTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.06f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = if (isFinalTenUrgent) 420 else 750),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "timerPulseScale"
                    )
                    animatedScale
                } else {
                    1f
                }

                Column(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Top Status Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onCancelAndExit,
                            modifier = Modifier.testTag("quit_quiz_button")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Exit quiz")
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Q ${sessionState.currentIndex + 1} / ${sessionState.questions.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isSoundEnabled = !isSoundEnabled },
                                modifier = Modifier.testTag("quiz_sound_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isSoundEnabled) {
                                        Icons.AutoMirrored.Filled.VolumeUp
                                    } else {
                                        Icons.AutoMirrored.Filled.VolumeOff
                                    },
                                    contentDescription = if (isSoundEnabled) {
                                        "Mute timer warning sounds"
                                    } else {
                                        "Unmute timer warning sounds"
                                    },
                                    tint = if (isSoundEnabled) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = timerColor.copy(alpha = if (isThirtySecWarning) 0.24f else 0.16f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                    }
                                    .border(
                                        width = if (isThirtySecWarning) 1.5.dp else 1.dp,
                                        color = timerColor.copy(alpha = if (isThirtySecWarning) 0.85f else 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .testTag("quiz_timer_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Timer,
                                        contentDescription = "Remaining seconds",
                                        tint = timerColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${remainingSec}s",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = timerColor
                                    )
                                }
                            }
                            Spacer(Modifier.width(4.dp))
                            TextButton(
                                onClick = onFinishEarly,
                                modifier = Modifier.testTag("finish_early_button")
                            ) {
                                Text("Submit", color = ForgeGold)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { timeFraction },
                        color = timerColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                    )

                    AnimatedVisibility(
                        visible = isThirtySecWarning,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            color = timerColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .border(
                                    width = 1.dp,
                                    color = timerColor.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("quiz_timer_warning_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = "Timer warning",
                                    tint = timerColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isFinalTenUrgent) {
                                        "Hurry! Only ${remainingSec}s left before auto-advance"
                                    } else {
                                        "Final ${remainingSec}s on this question — pace yourself"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = timerColor
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Question & Options Scroll Area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val isBookmarked = remember(
                            question.id,
                            question.question,
                            question.subject,
                            question.chapter,
                            bookmarkedQuestionIds
                        ) {
                            savedQuestionDocId(
                                question.question,
                                question.subject,
                                question.chapter
                            ) in bookmarkedQuestionIds
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (sessionState.config.mode == QuizMode.MEGA) {
                                    Surface(
                                        color = ForgeGold.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "MEGA",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ForgeGold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Surface(
                                    color = ElectricCyan.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${question.subject} • ${question.chapter}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onToggleBookmark(question) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("quiz_bookmark_button")
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = if (isBookmarked) {
                                        "Remove question bookmark"
                                    } else {
                                        "Bookmark question for revision"
                                    },
                                    tint = if (isBookmarked) ForgeGold else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                    RoundedCornerShape(20.dp)
                                )
                        ) {
                            Text(
                                text = question.question,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(18.dp)
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        question.options.forEachIndexed { idx, optText ->
                            val isSelected = selectedOption == idx
                            val borderColor = if (isSelected) ForgeGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            val bgColor = if (isSelected) ForgeGold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                            val badgeLetter = ('A' + idx).toString()

                            Surface(
                                color = bgColor,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
                                    .clickable { onSelectOption(idx) }
                                    .minimumInteractiveComponentSize()
                                    .testTag("quiz_option_$idx")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) ForgeGold else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Selected option $badgeLetter",
                                                tint = Color(0xFF1A1000),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text(
                                                text = badgeLetter,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = optText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }

                    // Bottom Early-Advance CTA
                    Button(
                        onClick = onAdvanceNext,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedOption != null) EmeraldSuccess else ForgeGold,
                            contentColor = Color(0xFF0A120D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("quiz_next_button")
                    ) {
                        Text(
                            text = when {
                                isLastQuestion -> "Submit & View Analysis"
                                selectedOption != null -> "Confirm & Next Question"
                                else -> "Skip to Next Question"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    }
                }
            }

            else -> Unit
        }
    }
}
