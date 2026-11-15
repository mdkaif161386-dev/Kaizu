package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CompetitiveExam
import com.example.model.QuizConfig
import com.example.model.QuizDifficulty
import com.example.model.QuizMode
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ForgeGold
import com.example.ui.theme.MegaObsidian

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MegaQuizSetupScreen(
    exam: CompetitiveExam,
    onStartMegaQuiz: (QuizConfig) -> Unit
) {
    val selectedBySubject = remember(exam.id) {
        mutableStateMapOf<String, Set<String>>().apply {
            exam.subjects.forEach { put(it.name, setOf(it.chapters.first())) }
        }
    }
    var questionCountInput by remember { mutableStateOf("15") }
    var selectedDifficulty by remember { mutableStateOf(QuizDifficulty.HARD) }
    var timePerQuestionInput by remember { mutableStateOf("45") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val presetCounts = listOf(9, 15, 20, 30)
    val presetTimes = listOf(30, 45, 60, 90)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mega Arena Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        1.5.dp,
                        Brush.linearGradient(listOf(ForgeGold, ElectricCyan)),
                        RoundedCornerShape(22.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_mega_arena),
                    contentDescription = "Mega Quiz Arena Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(MegaObsidian.copy(alpha = 0.45f), MegaObsidian.copy(alpha = 0.92f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = ForgeGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${exam.name} MEGA QUIZ",
                            style = MaterialTheme.typography.labelSmall,
                            color = ForgeGold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Multi-Subject Championship",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Select chapters from every subject below to enter the Mega Leaderboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "1. Pick Chapters Per Subject",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedButton(
                    onClick = {
                        exam.subjects.forEach { subj ->
                            val randomTwo = subj.chapters.shuffled().take(2).toSet()
                            selectedBySubject[subj.name] = randomTwo
                        }
                        validationError = null
                    },
                    modifier = Modifier.testTag("mega_random_chapters_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Quick Mix")
                }
            }

            exam.subjects.forEach { subject ->
                val chosen = selectedBySubject[subject.name].orEmpty()
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (chosen.isNotEmpty()) ElectricCyan.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(18.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = subject.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${chosen.size} selected",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (chosen.isNotEmpty()) ElectricCyan else CrimsonError
                            )
                        }

                        subject.chapters.forEach { ch ->
                            val isChecked = ch in chosen
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val next = if (isChecked) chosen - ch else chosen + ch
                                        selectedBySubject[subject.name] = next
                                        validationError = null
                                    }
                                    .padding(horizontal = 6.dp, vertical = 8.dp)
                                    .minimumInteractiveComponentSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                    contentDescription = if (isChecked) "Deselect $ch" else "Select $ch",
                                    tint = if (isChecked) ForgeGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = ch,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 2. Question Count
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "2. Total Mega Questions (3–40)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetCounts.forEach { count ->
                        FilterChip(
                            selected = questionCountInput == count.toString(),
                            onClick = { questionCountInput = count.toString() },
                            label = { Text("$count Qs") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForgeGold.copy(alpha = 0.22f),
                                selectedLabelColor = ForgeGold
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = questionCountInput,
                    onValueChange = { questionCountInput = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Custom question count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mega_count_input")
                )
            }

            // 3. Difficulty
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "3. Difficulty Tier",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuizDifficulty.entries.forEach { diff ->
                        val accent = when (diff) {
                            QuizDifficulty.EASY -> EmeraldSuccess
                            QuizDifficulty.MODERATE -> ElectricCyan
                            QuizDifficulty.HARD -> ForgeGold
                            QuizDifficulty.EXTREME -> CrimsonError
                        }
                        FilterChip(
                            selected = selectedDifficulty == diff,
                            onClick = { selectedDifficulty = diff },
                            label = { Text("${diff.label} (${diff.scoreMultiplier}x)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent.copy(alpha = 0.2f),
                                selectedLabelColor = accent
                            ),
                            modifier = Modifier.testTag("mega_diff_${diff.id}")
                        )
                    }
                }
            }

            // 4. Per Question Timer
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "4. Time Per Question (Seconds)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetTimes.forEach { sec ->
                        FilterChip(
                            selected = timePerQuestionInput == sec.toString(),
                            onClick = { timePerQuestionInput = sec.toString() },
                            label = { Text("${sec}s") }
                        )
                    }
                }
                OutlinedTextField(
                    value = timePerQuestionInput,
                    onValueChange = { timePerQuestionInput = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Custom seconds per question (10–600s)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mega_time_input")
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
                    val missingSubjects = exam.subjects.filter { selectedBySubject[it.name].isNullOrEmpty() }
                    val count = questionCountInput.toIntOrNull()
                    val secs = timePerQuestionInput.toIntOrNull()

                    when {
                        missingSubjects.isNotEmpty() -> {
                            validationError = "Please select at least 1 chapter from every subject (${missingSubjects.joinToString { it.name }})."
                        }
                        count == null || count !in 3..40 -> {
                            validationError = "Please enter between 3 and 40 questions for Mega Quiz."
                        }
                        secs == null || secs !in 10..600 -> {
                            validationError = "Please set per-question time between 10 and 600 seconds."
                        }
                        else -> {
                            val map = exam.subjects.associate { subj ->
                                subj.name to selectedBySubject[subj.name].orEmpty().toList()
                            }
                            onStartMegaQuiz(
                                QuizConfig(
                                    exam = exam,
                                    mode = QuizMode.MEGA,
                                    subjectChapters = map,
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
                    .testTag("start_mega_quiz_button")
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Launch Mega Quiz (1.4x Arena Bonus)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
