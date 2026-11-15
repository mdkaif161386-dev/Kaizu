package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.CompetitiveExam
import com.example.model.ExamSubject
import com.example.model.QuizConfig
import com.example.model.QuizDifficulty
import com.example.model.QuizMode
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ForgeGold

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuizSetupScreen(
    exam: CompetitiveExam,
    subject: ExamSubject,
    chapter: String,
    onBack: () -> Unit,
    onStartQuiz: (QuizConfig) -> Unit
) {
    BackHandler(onBack = onBack)

    var questionCountInput by remember { mutableStateOf("10") }
    var selectedDifficulty by remember { mutableStateOf(QuizDifficulty.HARD) }
    var timePerQuestionSec by remember { mutableStateOf(45) }
    var customTimeInput by remember { mutableStateOf("45") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val presetCounts = listOf(5, 10, 15, 20, 25, 30)
    val presetTimes = listOf(20, 30, 45, 60, 90, 120)

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(text = "Configure Quiz", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "${exam.name} • ${subject.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("setup_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to chapters"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 620.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Selected Chapter Header
                Surface(
                    color = ElectricCyan.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SELECTED CHAPTER",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = chapter,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Step 1: Number of Questions
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "1. Number of Questions (1–40)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetCounts.forEach { count ->
                            val isSelected = questionCountInput == count.toString()
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    questionCountInput = count.toString()
                                    validationError = null
                                },
                                label = { Text("$count Qs") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ForgeGold.copy(alpha = 0.22f),
                                    selectedLabelColor = ForgeGold
                                ),
                                modifier = Modifier.testTag("count_chip_$count")
                            )
                        }
                    }
                    OutlinedTextField(
                        value = questionCountInput,
                        onValueChange = { raw ->
                            questionCountInput = raw.filter { it.isDigit() }.take(2)
                            validationError = null
                        },
                        label = { Text("Or enter custom number of questions") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_question_count_input")
                    )
                }

                // Step 2: Difficulty Level
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "2. Select Difficulty Level",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    QuizDifficulty.entries.forEach { diff ->
                        val isSelected = diff == selectedDifficulty
                        val accent = when (diff) {
                            QuizDifficulty.EASY -> EmeraldSuccess
                            QuizDifficulty.MODERATE -> ElectricCyan
                            QuizDifficulty.HARD -> ForgeGold
                            QuizDifficulty.EXTREME -> CrimsonError
                        }
                        Surface(
                            color = if (isSelected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedDifficulty = diff }
                                .minimumInteractiveComponentSize()
                                .testTag("difficulty_${diff.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = diff.label,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = diff.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = accent.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${diff.scoreMultiplier}x pts",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Step 3: Time Per Question
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "3. Time Per Question (Seconds)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetTimes.forEach { sec ->
                            val isSelected = timePerQuestionSec == sec && customTimeInput == sec.toString()
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    timePerQuestionSec = sec
                                    customTimeInput = sec.toString()
                                    validationError = null
                                },
                                label = { Text("${sec}s") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan
                                ),
                                modifier = Modifier.testTag("time_chip_$sec")
                            )
                        }
                    }
                    OutlinedTextField(
                        value = customTimeInput,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(3)
                            customTimeInput = digits
                            digits.toIntOrNull()?.let { timePerQuestionSec = it }
                            validationError = null
                        },
                        label = { Text("Custom seconds per question (10–600s)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_time_input")
                    )
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        val count = questionCountInput.toIntOrNull()
                        val secs = customTimeInput.toIntOrNull()
                        when {
                            count == null || count !in 1..40 -> {
                                validationError = "Please enter between 1 and 40 questions."
                            }
                            secs == null || secs !in 10..600 -> {
                                validationError = "Please set per-question time between 10 and 600 seconds."
                            }
                            else -> {
                                onStartQuiz(
                                    QuizConfig(
                                        exam = exam,
                                        mode = QuizMode.CHAPTER,
                                        subjectChapters = mapOf(subject.name to listOf(chapter)),
                                        questionCount = count,
                                        difficulty = selectedDifficulty,
                                        timePerQuestionSec = secs
                                    )
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForgeGold,
                        contentColor = Color(0xFF1A1000)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("generate_quiz_button")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Forge AI Quiz",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
