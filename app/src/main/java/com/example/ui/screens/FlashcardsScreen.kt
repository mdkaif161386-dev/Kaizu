package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CompetitiveExam
import com.example.model.FlashcardDeck
import com.example.model.FlashcardFocus
import com.example.model.FlashcardItem
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ForgeGold
import com.example.ui.theme.ForgeGoldBright
import com.example.ui.theme.MegaObsidian
import com.example.ui.theme.VioletAccent
import com.example.ui.viewmodel.FlashcardGenRequest
import com.example.ui.viewmodel.FlashcardUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    exam: CompetitiveExam,
    initialSubjectId: String? = null,
    initialChapter: String? = null,
    uiState: FlashcardUiState,
    savedDecks: List<FlashcardDeck>,
    onGenerateDeck: (FlashcardGenRequest) -> Unit,
    onOpenDeck: (FlashcardDeck) -> Unit,
    onDeleteDeck: (String) -> Unit,
    onFlipCard: () -> Unit,
    onNextCard: () -> Unit,
    onPrevCard: () -> Unit,
    onMarkMastered: (String, Boolean) -> Unit,
    onShuffleDeck: () -> Unit,
    onToggleFilter: () -> Unit,
    onResetMastery: () -> Unit,
    onBackToBuilder: () -> Unit,
    onExitScreen: () -> Unit
) {
    BackHandler {
        if (uiState is FlashcardUiState.Builder) {
            onExitScreen()
        } else {
            onBackToBuilder()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = when (uiState) {
                            is FlashcardUiState.Studying -> uiState.deck.title
                            else -> "AI Flashcards Studio"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (uiState) {
                            is FlashcardUiState.Studying ->
                                "${uiState.deck.examName} • ${uiState.deck.chapters.size} chapter(s)"
                            else -> "${exam.name} • Key Concepts & Formulas"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (uiState is FlashcardUiState.Builder) onExitScreen() else onBackToBuilder()
                    },
                    modifier = Modifier.testTag("flashcards_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
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
            when (uiState) {
                is FlashcardUiState.Builder -> {
                    FlashcardsBuilderContent(
                        exam = exam,
                        initialSubjectId = initialSubjectId,
                        initialChapter = initialChapter,
                        savedDecks = savedDecks,
                        onGenerateDeck = onGenerateDeck,
                        onOpenDeck = onOpenDeck,
                        onDeleteDeck = onDeleteDeck
                    )
                }

                is FlashcardUiState.Generating -> {
                    FlashcardsGeneratingContent(state = uiState, onCancel = onBackToBuilder)
                }

                is FlashcardUiState.Studying -> {
                    FlashcardsStudyContent(
                        state = uiState,
                        onFlipCard = onFlipCard,
                        onNextCard = onNextCard,
                        onPrevCard = onPrevCard,
                        onMarkMastered = onMarkMastered,
                        onShuffleDeck = onShuffleDeck,
                        onToggleFilter = onToggleFilter,
                        onResetMastery = onResetMastery,
                        onNewDeck = onBackToBuilder
                    )
                }

                is FlashcardUiState.Error -> {
                    FlashcardsErrorContent(
                        message = uiState.message,
                        canRetry = uiState.lastRequest != null,
                        onRetry = { uiState.lastRequest?.let(onGenerateDeck) },
                        onBack = onBackToBuilder
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlashcardsBuilderContent(
    exam: CompetitiveExam,
    initialSubjectId: String?,
    initialChapter: String?,
    savedDecks: List<FlashcardDeck>,
    onGenerateDeck: (FlashcardGenRequest) -> Unit,
    onOpenDeck: (FlashcardDeck) -> Unit,
    onDeleteDeck: (String) -> Unit
) {
    val selectedBySubject = remember(exam.id) { mutableStateMapOf<String, Set<String>>() }
    var activeSubjectId by remember(exam.id) {
        mutableStateOf(
            exam.subjects.firstOrNull { it.id == initialSubjectId }?.id
                ?: exam.subjects.first().id
        )
    }
    var cardCount by remember { mutableStateOf(8) }
    var selectedFocus by remember { mutableStateOf(FlashcardFocus.BALANCED) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(exam.id, initialSubjectId, initialChapter) {
        val subj = exam.subjects.firstOrNull { it.id == initialSubjectId }
        if (subj != null) {
            activeSubjectId = subj.id
            if (!initialChapter.isNullOrBlank() && initialChapter in subj.chapters) {
                selectedBySubject[subj.name] = setOf(initialChapter)
            }
        }
    }

    val activeSubject = remember(exam, activeSubjectId) {
        exam.subjects.firstOrNull { it.id == activeSubjectId } ?: exam.subjects.first()
    }
    val totalSelectedCount = selectedBySubject.values.sumOf { it.size }
    val examSavedDecks = remember(savedDecks, exam.id) {
        savedDecks.filter { it.examId == exam.id }.ifEmpty { savedDecks }
    }

    LazyColumn(
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (examSavedDecks.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your Saved Flashcard Decks",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(examSavedDecks, key = { it.id.ifBlank { it.title } }) { deck ->
                            val mastered = deck.cards.count { it.mastered }
                            val total = deck.cards.size.coerceAtLeast(1)
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(18.dp),
                                tonalElevation = 2.dp,
                                modifier = Modifier
                                    .width(240.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(
                                        1.dp,
                                        ElectricCyan.copy(alpha = 0.45f),
                                        RoundedCornerShape(18.dp)
                                    )
                                    .clickable { onOpenDeck(deck) }
                                    .minimumInteractiveComponentSize()
                                    .testTag("saved_deck_${deck.id}")
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
                                        Surface(
                                            color = ElectricCyan.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "$mastered/$total mastered",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ElectricCyan,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteDeck(deck.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "Delete deck ${deck.title}",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = deck.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    LinearProgressIndicator(
                                        progress = { mastered.toFloat() / total.toFloat() },
                                        color = EmeraldSuccess,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(50))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. Choose Chapters ($totalSelectedCount selected)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (totalSelectedCount > 0) {
                        TextButton(onClick = { selectedBySubject.clear() }) {
                            Text("Clear", color = ElectricCyan)
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    exam.subjects.forEach { subj ->
                        val countForSubject = selectedBySubject[subj.name]?.size ?: 0
                        FilterChip(
                            selected = subj.id == activeSubject.id,
                            onClick = { activeSubjectId = subj.id },
                            label = {
                                Text(
                                    if (countForSubject > 0) "${subj.name} ($countForSubject)"
                                    else subj.name
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForgeGold.copy(alpha = 0.22f),
                                selectedLabelColor = ForgeGold
                            ),
                            modifier = Modifier.testTag("flashcard_subject_tab_${subj.id}")
                        )
                    }
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        RoundedCornerShape(18.dp)
                    )
            ) {
                FlowRow(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeSubject.chapters.forEachIndexed { idx, ch ->
                        val currentSet = selectedBySubject[activeSubject.name].orEmpty()
                        val isSelected = ch in currentSet
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                validationError = null
                                val next = currentSet.toMutableSet()
                                if (isSelected) next.remove(ch) else next.add(ch)
                                if (next.isEmpty()) {
                                    selectedBySubject.remove(activeSubject.name)
                                } else {
                                    selectedBySubject[activeSubject.name] = next
                                }
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            label = { Text(ch) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = ElectricCyan,
                                selectedLeadingIconColor = ElectricCyan
                            ),
                            modifier = Modifier.testTag("flashcard_chapter_chip_$idx")
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "2. Study Focus",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                FlashcardFocus.entries.forEach { focus ->
                    val isSelected = focus == selectedFocus
                    Surface(
                        color = if (isSelected) ForgeGold.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) ForgeGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedFocus = focus }
                            .minimumInteractiveComponentSize()
                            .testTag("flashcard_focus_${focus.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = focus.label,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) ForgeGold else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = focus.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "3. Number of Flashcards",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(6, 8, 12, 16).forEach { count ->
                        FilterChip(
                            selected = cardCount == count,
                            onClick = { cardCount = count },
                            label = { Text("$count Cards") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VioletAccent.copy(alpha = 0.22f),
                                selectedLabelColor = VioletAccent
                            ),
                            modifier = Modifier.testTag("flashcard_count_$count")
                        )
                    }
                }
            }
        }

        if (validationError != null) {
            item {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            Button(
                onClick = {
                    val map = selectedBySubject
                        .filterValues { it.isNotEmpty() }
                        .mapValues { it.value.toList() }
                    if (map.isEmpty()) {
                        validationError = "Please select at least one chapter to generate flashcards."
                    } else {
                        onGenerateDeck(
                            FlashcardGenRequest(
                                exam = exam,
                                subjectChapters = map,
                                cardCount = cardCount,
                                focus = selectedFocus
                            )
                        )
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
                    .testTag("generate_flashcards_button")
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Generate AI Flashcards",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FlashcardsGeneratingContent(
    state: FlashcardUiState.Generating,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(ElectricCyan.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(62.dp),
                    color = ElectricCyan,
                    strokeWidth = 4.dp
                )
                Icon(
                    imageVector = Icons.Filled.Style,
                    contentDescription = null,
                    tint = ForgeGold,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = "Crafting Concept & Formula Cards...",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Distilling ${state.cardCount} ${state.examName} flashcards for ${state.chapterSummary}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.testTag("cancel_flashcards_generation")
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun FlashcardsStudyContent(
    state: FlashcardUiState.Studying,
    onFlipCard: () -> Unit,
    onNextCard: () -> Unit,
    onPrevCard: () -> Unit,
    onMarkMastered: (String, Boolean) -> Unit,
    onShuffleDeck: () -> Unit,
    onToggleFilter: () -> Unit,
    onResetMastery: () -> Unit,
    onNewDeck: () -> Unit
) {
    val totalCards = state.deck.cards.size
    val masteredCount = state.deck.cards.count { it.mastered }
    val visibleCards = state.visibleCards

    Column(
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mastery Progress & Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mastered $masteredCount / $totalCards",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = EmeraldSuccess
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = state.filterUnmasteredOnly,
                    onClick = onToggleFilter,
                    label = { Text(if (state.filterUnmasteredOnly) "Still Learning" else "All Cards") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForgeGold.copy(alpha = 0.22f),
                        selectedLabelColor = ForgeGold
                    ),
                    modifier = Modifier.testTag("flashcards_filter_chip")
                )
                IconButton(
                    onClick = onShuffleDeck,
                    modifier = Modifier.testTag("flashcards_shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle flashcards",
                        tint = ElectricCyan
                    )
                }
            }
        }

        LinearProgressIndicator(
            progress = {
                if (totalCards > 0) masteredCount.toFloat() / totalCards.toFloat() else 0f
            },
            color = EmeraldSuccess,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(50))
        )

        if (state.syncError != null) {
            Text(
                text = state.syncError,
                style = MaterialTheme.typography.bodySmall,
                color = ForgeGold
            )
        }

        if (visibleCards.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EmeraldSuccess.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Every Card Mastered!",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "You've cleared all $totalCards concepts & formulas in this deck.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = onResetMastery) {
                                Text("Practice Again")
                            }
                            Button(
                                onClick = onNewDeck,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForgeGold,
                                    contentColor = Color(0xFF1A1000)
                                )
                            ) {
                                Text("New Deck")
                            }
                        }
                    }
                }
            }
            return
        }

        val safeIndex = state.currentIndex.coerceIn(0, visibleCards.lastIndex)
        val card = visibleCards[safeIndex]

        // Card Index Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Card ${safeIndex + 1} of ${visibleCards.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (card.mastered) {
                Surface(
                    color = EmeraldSuccess.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "MASTERED",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess
                        )
                    }
                }
            }
        }

        // Interactive 3D Flippable Card
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            FlippableStudyCard(
                card = card,
                isFlipped = state.isFlipped,
                onFlip = onFlipCard,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Mastery Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { onMarkMastered(card.id, false) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("flashcard_needs_review_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = ForgeGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Review Again", color = ForgeGold)
            }
            Button(
                onClick = { onMarkMastered(card.id, true) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldSuccess,
                    contentColor = Color(0xFF04140D)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("flashcard_mastered_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Got It", fontWeight = FontWeight.Bold)
            }
        }

        // Prev / Flip / Next Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPrevCard,
                enabled = safeIndex > 0,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("flashcard_prev_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous card")
                Spacer(Modifier.width(4.dp))
                Text("Prev")
            }

            TextButton(
                onClick = onFlipCard,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("flashcard_flip_button")
            ) {
                Icon(Icons.Filled.Cached, contentDescription = null, tint = ElectricCyan)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (state.isFlipped) "Show Question" else "Reveal Answer",
                    color = ElectricCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onNextCard,
                enabled = safeIndex < visibleCards.lastIndex,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForgeGold,
                    contentColor = Color(0xFF1A1000)
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("flashcard_next_button")
            ) {
                Text("Next", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next card")
            }
        }
    }
}

@Composable
private fun FlippableStudyCard(
    card: FlashcardItem,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "flashcardRotation"
    )
    val density = LocalDensity.current.density
    val showingBack = rotation > 90f

    Surface(
        color = if (showingBack) MegaObsidian else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 4.dp,
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clip(RoundedCornerShape(26.dp))
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    if (showingBack) listOf(ElectricCyan, ForgeGoldBright)
                    else listOf(ForgeGold.copy(alpha = 0.65f), VioletAccent.copy(alpha = 0.55f))
                ),
                shape = RoundedCornerShape(26.dp)
            )
            .clickable(onClick = onFlip)
            .testTag("flashcard_interactive_card")
    ) {
        if (!showingBack) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = ElectricCyan.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${card.subject} • ${card.chapter}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "CONCEPT RECALL",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForgeGold
                    )
                    Text(
                        text = card.frontTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = card.prompt,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cached,
                        contentDescription = null,
                        tint = ForgeGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Tap card to reveal key formula & summary",
                        style = MaterialTheme.typography.labelMedium,
                        color = ForgeGold
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.frontTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = ForgeGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = card.subject,
                            style = MaterialTheme.typography.labelSmall,
                            color = ForgeGoldBright,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (card.keyFormula.isNotBlank()) {
                    Surface(
                        color = Color(0xFF0A1424),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "KEY FORMULA / RULE",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                            Text(
                                text = card.keyFormula,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ForgeGoldBright
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CONCEPT SUMMARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = card.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFE2E8F0)
                    )
                }

                if (card.memoryTip.isNotBlank()) {
                    Surface(
                        color = EmeraldSuccess.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, EmeraldSuccess.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = card.memoryTip,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD1FAE5)
                            )
                        }
                    }
                }

                Spacer(Modifier.heightIn(min = 4.dp))
                Text(
                    text = "Tap card to flip back",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FlashcardsErrorContent(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
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
                text = "Flashcard Generation Failed",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) {
                    Text("Back")
                }
                if (canRetry) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForgeGold,
                            contentColor = Color(0xFF1A1000)
                        )
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
