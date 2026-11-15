package com.example

import com.example.model.savedQuestionDocId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedQuestionUnitTest {

    @Test
    fun savedQuestionDocId_isDeterministicAndNormalized() {
        val id1 = savedQuestionDocId(
            question = "What is the escape velocity from Earth?",
            subject = "Physics",
            chapter = "Gravitation"
        )
        val id2 = savedQuestionDocId(
            question = "  What is the escape velocity from Earth?  ",
            subject = "  physics ",
            chapter = " GRAVITATION "
        )
        assertEquals(id1, id2)
    }

    @Test
    fun savedQuestionDocId_differsAcrossDistinctQuestionsOrChapters() {
        val q1 = savedQuestionDocId(
            question = "What is Newton's second law?",
            subject = "Physics",
            chapter = "Laws of Motion"
        )
        val q2 = savedQuestionDocId(
            question = "What is Newton's third law?",
            subject = "Physics",
            chapter = "Laws of Motion"
        )
        val q3 = savedQuestionDocId(
            question = "What is Newton's second law?",
            subject = "Physics",
            chapter = "Work, Energy and Power"
        )
        assertNotEquals(q1, q2)
        assertNotEquals(q1, q3)
    }

    @Test
    fun savedQuestionDocId_matchesFirestoreIsValidIdRule() {
        val id = savedQuestionDocId(
            question = "Calculate ∆G° when K_eq = 100 at 298 K (in kJ/mol).",
            subject = "Chemistry",
            chapter = "Chemical Thermodynamics & Equilibrium"
        )
        assertTrue(id.startsWith("q_"))
        assertTrue(id.length in 1..128)
        assertTrue(Regex("^[a-zA-Z0-9_\\-]+$").matches(id))
    }
}
