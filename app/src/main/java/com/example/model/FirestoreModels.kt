package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest

enum class QuizMode(val id: String) {
    CHAPTER("CHAPTER"),
    MEGA("MEGA")
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val dob: String = "",
    val phoneNumber: String = "",
    val selectedExamId: String = "NEET",
    val totalPoints: Long = 0L,
    val quizzesCompleted: Long = 0L,
    val totalQuestionsSolved: Long = 0L,
    val totalCorrectAnswers: Long = 0L,
    val megaQuizzesCompleted: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val averageAccuracyPercent: Double
        get() = if (totalQuestionsSolved > 0) {
            (totalCorrectAnswers.toDouble() / totalQuestionsSolved.toDouble()) * 100.0
        } else 0.0
}

data class GeneratedQuestion(
    val id: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val explanation: String = "",
    val subject: String = "",
    val chapter: String = ""
)

@IgnoreExtraProperties
data class QuestionReview(
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val selectedIndex: Int = -1,
    val explanation: String = "",
    val subject: String = "",
    val chapter: String = "",
    val isCorrect: Boolean = false
) {
    fun toMap(): Map<String, Any> = mapOf(
        "question" to question,
        "options" to options,
        "correctIndex" to correctIndex,
        "selectedIndex" to selectedIndex,
        "explanation" to explanation,
        "subject" to subject,
        "chapter" to chapter,
        "isCorrect" to isCorrect
    )
}

@IgnoreExtraProperties
data class QuizAttempt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val examId: String = "",
    val examName: String = "",
    val mode: String = QuizMode.CHAPTER.id,
    val subjectNames: List<String> = emptyList(),
    val chapterSummary: String = "",
    val difficulty: String = QuizDifficulty.MODERATE.id,
    val totalQuestions: Long = 0L,
    val correctCount: Long = 0L,
    val wrongCount: Long = 0L,
    val skippedCount: Long = 0L,
    val scorePoints: Long = 0L,
    val accuracyPercent: Double = 0.0,
    val timePerQuestionSec: Long = 45L,
    val timeTakenSec: Long = 0L,
    val questionReviews: List<QuestionReview> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudyInsight(
    @DocumentId val id: String = "",
    val userId: String = "",
    val examId: String = "",
    val headline: String = "",
    val recommendations: List<String> = emptyList(),
    val focusTopics: List<String> = emptyList(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val examId: String = "",
    val examName: String = "",
    val scorePoints: Long = 0L,
    val bestAccuracy: Double = 0.0,
    val quizzesCount: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MegaLeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val examId: String = "",
    val examName: String = "",
    val scorePoints: Long = 0L,
    val bestAccuracy: Double = 0.0,
    val megaQuizzesCount: Long = 0L,
    val highestDifficulty: String = QuizDifficulty.MODERATE.id,
    val bestTimeSec: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class QuizConfig(
    val exam: CompetitiveExam,
    val mode: QuizMode,
    val subjectChapters: Map<String, List<String>>,
    val questionCount: Int,
    val difficulty: QuizDifficulty,
    val timePerQuestionSec: Int
)

enum class FlashcardFocus(
    val id: String,
    val label: String,
    val description: String
) {
    BALANCED(
        id = "BALANCED",
        label = "Concepts & Formulas",
        description = "High-yield definitions, core principles, and essential equations"
    ),
    FORMULAS(
        id = "FORMULAS",
        label = "Formula Sheet",
        description = "Equations, constants, laws, SI units, and quick shortcuts"
    ),
    TRAPS(
        id = "TRAPS",
        label = "Exam Traps & Edge Cases",
        description = "Common misconceptions, exceptions, and memory hooks"
    );

    companion object {
        fun fromId(id: String): FlashcardFocus = entries.firstOrNull { it.id == id } ?: BALANCED
    }
}

@IgnoreExtraProperties
data class FlashcardItem(
    val id: String = "",
    val frontTitle: String = "",
    val prompt: String = "",
    val keyFormula: String = "",
    val summary: String = "",
    val memoryTip: String = "",
    val subject: String = "",
    val chapter: String = "",
    val mastered: Boolean = false
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "frontTitle" to frontTitle,
        "prompt" to prompt,
        "keyFormula" to keyFormula,
        "summary" to summary,
        "memoryTip" to memoryTip,
        "subject" to subject,
        "chapter" to chapter,
        "mastered" to mastered
    )
}

@IgnoreExtraProperties
data class FlashcardDeck(
    @DocumentId val id: String = "",
    val userId: String = "",
    val examId: String = "",
    val examName: String = "",
    val title: String = "",
    val subjectNames: List<String> = emptyList(),
    val chapters: List<String> = emptyList(),
    val focus: String = FlashcardFocus.BALANCED.id,
    val cards: List<FlashcardItem> = emptyList(),
    val masteredCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavedQuestion(
    @DocumentId val id: String = "",
    val userId: String = "",
    val examId: String = "",
    val examName: String = "",
    val subject: String = "",
    val chapter: String = "",
    val difficulty: String = QuizDifficulty.MODERATE.id,
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val explanation: String = "",
    @ServerTimestamp val savedAt: Timestamp? = null
)

fun savedQuestionDocId(question: String, subject: String, chapter: String): String {
    val raw = "${subject.trim().lowercase()}|${chapter.trim().lowercase()}|${question.trim()}"
    val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
    val hex = digest.joinToString("") { "%02x".format(it) }.take(32)
    return "q_$hex"
}
