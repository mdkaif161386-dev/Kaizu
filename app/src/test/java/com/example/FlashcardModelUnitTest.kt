package com.example

import com.example.model.FlashcardDeck
import com.example.model.FlashcardFocus
import com.example.model.FlashcardItem
import com.example.ui.viewmodel.FlashcardUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashcardModelUnitTest {

    @Test
    fun flashcardFocus_fromIdResolvesAndFallsBack() {
        assertEquals(FlashcardFocus.FORMULAS, FlashcardFocus.fromId("FORMULAS"))
        assertEquals(FlashcardFocus.TRAPS, FlashcardFocus.fromId("TRAPS"))
        assertEquals(FlashcardFocus.BALANCED, FlashcardFocus.fromId("UNKNOWN"))
    }

    @Test
    fun flashcardItem_toMapPreservesAllFields() {
        val item = FlashcardItem(
            id = "c1",
            frontTitle = "Gauss Law",
            prompt = "State electric flux through closed surface",
            keyFormula = "Phi = q / e0",
            summary = "Total electric flux depends only on enclosed charge.",
            memoryTip = "Independent of surface shape",
            subject = "Physics",
            chapter = "Electrostatics",
            mastered = true
        )
        val map = item.toMap()
        assertEquals("c1", map["id"])
        assertEquals("Gauss Law", map["frontTitle"])
        assertEquals("Phi = q / e0", map["keyFormula"])
        assertTrue(map["mastered"] as Boolean)
    }

    @Test
    fun studyingState_filtersUnmasteredCardsAccurately() {
        val cards = listOf(
            FlashcardItem(id = "1", frontTitle = "Card 1", mastered = true),
            FlashcardItem(id = "2", frontTitle = "Card 2", mastered = false),
            FlashcardItem(id = "3", frontTitle = "Card 3", mastered = false)
        )
        val deck = FlashcardDeck(id = "d1", title = "Electrostatics", cards = cards, masteredCount = 1L)

        val allState = FlashcardUiState.Studying(deck = deck, filterUnmasteredOnly = false)
        assertEquals(3, allState.visibleCards.size)

        val unmasteredState = FlashcardUiState.Studying(deck = deck, filterUnmasteredOnly = true)
        assertEquals(2, unmasteredState.visibleCards.size)
        assertFalse(unmasteredState.visibleCards.any { it.mastered })
    }
}
