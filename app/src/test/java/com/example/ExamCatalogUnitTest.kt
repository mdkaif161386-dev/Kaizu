package com.example

import com.example.model.ExamCatalog
import com.example.model.QuizDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamCatalogUnitTest {
    @Test
    fun allExamsHaveSubjectsAndChapters() {
        assertTrue(ExamCatalog.exams.size >= 8)
        ExamCatalog.exams.forEach { exam ->
            assertTrue("Exam ${exam.id} must have subjects", exam.subjects.isNotEmpty())
            exam.subjects.forEach { subj ->
                assertTrue("Subject ${subj.id} must have chapters", subj.chapters.isNotEmpty())
            }
        }
    }

    @Test
    fun neetHasPhysicsChemistryBiology() {
        val neet = ExamCatalog.getExamById("NEET")
        val names = neet.subjects.map { it.name }
        assertEquals(listOf("Physics", "Chemistry", "Biology"), names)
    }

    @Test
    fun difficultyMultipliersIncreaseWithHardness() {
        assertTrue(QuizDifficulty.EASY.scoreMultiplier < QuizDifficulty.MODERATE.scoreMultiplier)
        assertTrue(QuizDifficulty.MODERATE.scoreMultiplier < QuizDifficulty.HARD.scoreMultiplier)
        assertTrue(QuizDifficulty.HARD.scoreMultiplier < QuizDifficulty.EXTREME.scoreMultiplier)
    }
}
