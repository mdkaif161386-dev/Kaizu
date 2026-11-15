package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.model.CompetitiveExam
import com.example.model.FlashcardDeck
import com.example.model.FlashcardFocus
import com.example.model.FlashcardItem
import com.example.model.GeneratedQuestion
import com.example.model.QuizAttempt
import com.example.model.QuizConfig
import com.example.model.QuizDifficulty
import com.example.model.QuizMode
import com.example.model.StudyInsight
import com.example.util.ChatPolicyFilter
import com.example.util.PolicyCheckResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

private const val TAG = "AiQuizRepository"

class AiQuizRepository {

    private val quizSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "question" to Schema.string(description = "Clear, exam-grade MCQ stem"),
                        "options" to Schema.array(
                            items = Schema.string(),
                            description = "Exactly 4 distinct answer choices (A, B, C, D text without prefix letters)"
                        ),
                        "correctIndex" to Schema.integer(description = "0-based index (0..3) of the single correct option"),
                        "explanation" to Schema.string(description = "Step-by-step solution explaining why the correct option is right and why key distractors are wrong"),
                        "subject" to Schema.string(description = "Subject name"),
                        "chapter" to Schema.string(description = "Chapter name")
                    )
                ),
                description = "List of generated MCQ questions"
            )
        )
    )

    private val insightSchema = Schema.obj(
        properties = mapOf(
            "headline" to Schema.string(description = "Concise 1-sentence personalized performance diagnosis"),
            "recommendations" to Schema.array(
                items = Schema.string(),
                description = "3 to 5 concrete, actionable study steps tailored to weak areas and difficulty level"
            ),
            "focusTopics" to Schema.array(
                items = Schema.string(),
                description = "Up to 5 high-priority chapters or concepts to revise next"
            )
        )
    )

    private val flashcardSchema = Schema.obj(
        properties = mapOf(
            "deckTitle" to Schema.string(description = "Concise, descriptive study deck title for the selected chapters"),
            "cards" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "frontTitle" to Schema.string(description = "Short concept or law title shown on the front face (2 to 7 words)"),
                        "prompt" to Schema.string(description = "Active-recall prompt or question shown on the card front to test memory before flipping"),
                        "keyFormula" to Schema.string(description = "Core formula, equation, reaction, rule, or constant in clean readable notation; empty string if purely conceptual"),
                        "summary" to Schema.string(description = "Clear, crisp 2-to-4 sentence concept summary or derivation shown on the card back"),
                        "memoryTip" to Schema.string(description = "1-line exam mnemonic, unit check, or trap-avoidance tip"),
                        "subject" to Schema.string(description = "Subject name from the selected list"),
                        "chapter" to Schema.string(description = "Chapter name from the selected list")
                    )
                ),
                description = "List of interactive study flashcards covering key concepts and formulas"
            )
        )
    )

    private val quizModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quizSchema
                temperature = 0.7f
            }
        )
    }

    private val insightModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = insightSchema
                temperature = 0.5f
            }
        )
    }

    private val flashcardModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = flashcardSchema
                temperature = 0.6f
            }
        )
    }

    private val moderationSchema = Schema.obj(
        properties = mapOf(
            "isSexualViolation" to Schema.boolean(
                description = "True ONLY if the message/media contains sexual, pornographic, adult-video, or sexually harassing content; false for genuine academic study messages including Biology reproduction."
            ),
            "reason" to Schema.string(
                description = "Brief 1-sentence policy reason if isSexualViolation is true, otherwise empty string."
            )
        )
    )

    private val moderationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = moderationSchema
                temperature = 0.1f
            }
        )
    }

    suspend fun generateQuiz(
        config: QuizConfig,
        onProgress: (completedBatches: Int, totalBatches: Int) -> Unit = { _, _ -> }
    ): List<GeneratedQuestion> = withContext(Dispatchers.Default) {
        val total = config.questionCount.coerceIn(1, 50)
        val batchSize = 10
        val totalBatches = (total + batchSize - 1) / batchSize
        val collected = mutableListOf<GeneratedQuestion>()

        for (batchIdx in 0 until totalBatches) {
            onProgress(batchIdx, totalBatches)
            val remaining = total - collected.size
            val countInBatch = minOf(batchSize, remaining)
            val prompt = buildQuizPrompt(config, countInBatch, batchIdx + 1, totalBatches, collected)
            val batchQuestions = try {
                withTimeout(60_000L) {
                    val response = quizModel.generateContent(prompt)
                    val jsonString = response.text
                        ?: throw IllegalStateException("AI returned an empty response.")
                    parseQuestionsJson(jsonString, config)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Quiz generation timed out on batch ${batchIdx + 1}", e)
                throw IllegalStateException("Quiz generation timed out. Please try fewer questions or retry.", e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Quiz generation failed on batch ${batchIdx + 1}", e)
                throw IllegalStateException(
                    e.localizedMessage ?: "Failed to generate quiz questions.",
                    e
                )
            }
            collected.addAll(batchQuestions.take(countInBatch))
        }
        onProgress(totalBatches, totalBatches)

        if (collected.isEmpty()) {
            throw IllegalStateException("No questions were generated. Please try again.")
        }
        collected
    }

    private fun buildQuizPrompt(
        config: QuizConfig,
        count: Int,
        batchNumber: Int,
        totalBatches: Int,
        alreadyGenerated: List<GeneratedQuestion>
    ): String {
        val syllabusText = config.subjectChapters.entries.joinToString("\n") { (subject, chapters) ->
            "- $subject: ${chapters.joinToString(", ")}"
        }
        val difficultyInstruction = when (config.difficulty) {
            QuizDifficulty.EASY -> "Foundation level: clear conceptual recall and single-step numericals aligned to core syllabus."
            QuizDifficulty.MODERATE -> "Standard competitive exam level: authentic exam-pattern conceptual and multi-step problems."
            QuizDifficulty.HARD -> "Hard Ranker level: multi-concept synthesis, subtle assertion/trap distractors, demanding analytical rigor."
            QuizDifficulty.EXTREME -> "Extreme Olympiad/Top-1% level: deep conceptual traps, multi-stage derivations, tricky edge cases, and non-trivial problem solving."
        }
        val modeNote = if (config.mode == QuizMode.MEGA) {
            "This is a multi-subject MEGA QUIZ. Distribute the $count questions evenly across all listed subjects and chapters."
        } else {
            "Focus strictly on the listed subject and chapter."
        }
        val avoidList = if (alreadyGenerated.isNotEmpty()) {
            "\nAvoid repeating concepts/stems from these already generated questions:\n" +
                alreadyGenerated.takeLast(12).joinToString("\n") { "- ${it.question.take(90)}" }
        } else ""

        return """
            You are an expert ${config.exam.name} (${config.exam.subtitle}) exam question setter.
            Generate exactly $count original, high-accuracy multiple-choice questions (batch $batchNumber of $totalBatches).

            Exam: ${config.exam.name}
            Difficulty: ${config.difficulty.label} — $difficultyInstruction
            $modeNote

            Target Subjects & Chapters:
            $syllabusText

            Strict Quality Rules:
            1. Each question must have exactly 4 distinct, plausible options (do NOT prefix options with "A)", "B)", etc.).
            2. `correctIndex` MUST be 0, 1, 2, or 3 corresponding accurately to the single correct option.
            3. `explanation` must provide a complete, crystal-clear step-by-step solution (formulas, logic, elimination of wrong options) so a student learns deeply from mistakes.
            4. Verify all calculations, chemical formulas, biological facts, legal/constitutional articles, and numerical answers carefully before finalizing.
            5. Fill `subject` and `chapter` accurately from the provided list.
            $avoidList
        """.trimIndent()
    }

    private fun parseQuestionsJson(jsonString: String, config: QuizConfig): List<GeneratedQuestion> {
        val root = JSONObject(jsonString)
        val arr = root.getJSONArray("questions")
        val fallbackSubject = config.subjectChapters.keys.firstOrNull() ?: "General"
        val fallbackChapter = config.subjectChapters.values.flatten().firstOrNull() ?: "Mixed"

        return (0 until arr.length()).mapNotNull { idx ->
            val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
            val qText = obj.optString("question").trim()
            val optsArr = obj.optJSONArray("options") ?: return@mapNotNull null
            val options = (0 until optsArr.length()).map { optsArr.optString(it).trim() }
                .filter { it.isNotEmpty() }
            if (qText.isEmpty() || options.size < 2) return@mapNotNull null

            val normalizedOptions = if (options.size >= 4) {
                options.take(4)
            } else {
                options + List(4 - options.size) { "None of the above" }
            }
            val correctIndex = obj.optInt("correctIndex", 0).coerceIn(0, 3)
            val explanation = obj.optString("explanation").ifBlank {
                "The correct answer is Option ${'A' + correctIndex}: ${normalizedOptions[correctIndex]}."
            }
            val subject = obj.optString("subject").ifBlank { fallbackSubject }
            val chapter = obj.optString("chapter").ifBlank { fallbackChapter }

            GeneratedQuestion(
                id = UUID.randomUUID().toString(),
                question = qText,
                options = normalizedOptions,
                correctIndex = correctIndex,
                explanation = explanation,
                subject = subject,
                chapter = chapter
            )
        }
    }

    suspend fun generateStudyInsights(
        userId: String,
        examName: String,
        examId: String,
        recentAttempts: List<QuizAttempt>
    ): StudyInsight = withContext(Dispatchers.Default) {
        val summaryLines = recentAttempts.take(8).joinToString("\n") { attempt ->
            val wrongChapters = attempt.questionReviews
                .filter { !it.isCorrect }
                .map { "${it.subject}: ${it.chapter}" }
                .distinct()
                .joinToString(", ")
            "- [${attempt.mode}] ${attempt.subjectNames.joinToString("/")} (${attempt.chapterSummary}) | Difficulty: ${attempt.difficulty} | Accuracy: ${attempt.accuracyPercent.toInt()}% (${attempt.correctCount}/${attempt.totalQuestions}) | Weak areas: ${wrongChapters.ifBlank { "None" }}"
        }

        val prompt = """
            Analyze this student's recent competitive exam practice history for $examName and generate a concise, personalized study action plan.

            Recent Attempts:
            $summaryLines

            Requirements:
            - `headline`: Encouraging, sharp 1-sentence diagnosis of their current readiness.
            - `recommendations`: 4 concrete, specific study tactics (e.g., which chapters to revise, speed vs accuracy strategy, next difficulty step).
            - `focusTopics`: Top 3 to 5 specific chapters/topics to prioritize next based on mistakes.
        """.trimIndent()

        try {
            withTimeout(45_000L) {
                val response = insightModel.generateContent(prompt)
                val jsonString = response.text
                    ?: throw IllegalStateException("Empty study insight response.")
                val json = JSONObject(jsonString)
                val headline = json.optString("headline").ifBlank {
                    "Steady progress in $examName — focus on converting weak chapters into high-accuracy streaks."
                }
                val recsArr = json.optJSONArray("recommendations")
                val recs = if (recsArr != null) {
                    (0 until recsArr.length()).map { recsArr.optString(it).trim() }.filter { it.isNotEmpty() }
                } else emptyList()
                val topicsArr = json.optJSONArray("focusTopics")
                val topics = if (topicsArr != null) {
                    (0 until topicsArr.length()).map { topicsArr.optString(it).trim() }.filter { it.isNotEmpty() }
                } else emptyList()

                StudyInsight(
                    id = "latest",
                    userId = userId,
                    examId = examId,
                    headline = headline,
                    recommendations = recs.ifEmpty {
                        listOf("Review wrong-answer solutions from your latest attempt and retest at Moderate/Hard difficulty.")
                    },
                    focusTopics = topics
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Study insight generation timed out", e)
            throw IllegalStateException("Study plan generation timed out.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Study insight generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to generate study plan.", e)
        }
    }

    suspend fun generateFlashcards(
        userId: String,
        exam: CompetitiveExam,
        subjectChapters: Map<String, List<String>>,
        cardCount: Int,
        focus: FlashcardFocus
    ): FlashcardDeck = withContext(Dispatchers.Default) {
        val safeCount = cardCount.coerceIn(4, 20)
        val syllabusLines = subjectChapters.entries.joinToString("\n") { (subj, chs) ->
            "- $subj: ${chs.joinToString(", ")}"
        }
        val focusInstruction = when (focus) {
            FlashcardFocus.BALANCED ->
                "Balance essential conceptual summaries, governing laws, and core formulas so students master both theory and problem-solving."
            FlashcardFocus.FORMULAS ->
                "Prioritize key equations, mathematical relations, constants, SI units, conditions of validity, and quick calculation formulas."
            FlashcardFocus.TRAPS ->
                "Prioritize high-frequency exam traps, exception rules, sign conventions, edge cases, and memorable comparison hooks."
        }

        val prompt = """
            You are an expert ${exam.name} (${exam.subtitle}) mentor creating rapid-revision flashcards.
            Generate exactly $safeCount high-yield interactive flashcards covering the selected syllabus chapters.

            Exam: ${exam.name}
            Focus Mode: ${focus.label} — $focusInstruction

            Selected Subjects & Chapters:
            $syllabusLines

            Strict Quality Rules:
            1. Distribute cards fairly across all listed subjects and chapters.
            2. `frontTitle`: Crisp 2–7 word topic headline (e.g., "Gauss's Law in Dielectrics", "SN1 vs SN2 Reactivity").
            3. `prompt`: A sharp active-recall question or cue for the front of the card (do NOT give away the formula or answer on the front).
            4. `keyFormula`: Write the exact formula/equation/rule using clean Unicode symbols (e.g., "Φ = q_enclosed / ε₀", "ΔG° = -RT ln K") when applicable; use "" only if the concept has no symbolic formula.
            5. `summary`: Concise, exam-accurate explanation (2–4 sentences) clarifying variables, conditions, and applications.
            6. `memoryTip`: One short practical exam tip, dimensional check, or mnemonic.
            7. Fill `subject` and `chapter` accurately from the provided list.
        """.trimIndent()

        try {
            withTimeout(60_000L) {
                val response = flashcardModel.generateContent(prompt)
                val jsonString = response.text
                    ?: throw IllegalStateException("AI returned an empty flashcard response.")
                val root = JSONObject(jsonString)
                val fallbackSubject = subjectChapters.keys.firstOrNull() ?: "General"
                val allChapters = subjectChapters.values.flatten().distinct()
                val fallbackChapter = allChapters.firstOrNull() ?: "Core Concepts"

                val deckTitle = root.optString("deckTitle").trim().ifBlank {
                    if (allChapters.size == 1) "${allChapters.first()} Flashcards"
                    else "${exam.name} Rapid Revision (${allChapters.size} Chapters)"
                }.take(160)

                val cardsArr = root.getJSONArray("cards")
                val parsedCards = (0 until cardsArr.length()).mapNotNull { idx ->
                    val obj = cardsArr.optJSONObject(idx) ?: return@mapNotNull null
                    val frontTitle = obj.optString("frontTitle").trim()
                    val cardPrompt = obj.optString("prompt").trim()
                    val summary = obj.optString("summary").trim()
                    if (frontTitle.isEmpty() || summary.isEmpty()) return@mapNotNull null

                    FlashcardItem(
                        id = UUID.randomUUID().toString(),
                        frontTitle = frontTitle,
                        prompt = cardPrompt.ifBlank { "Recall the key concept and formula for $frontTitle." },
                        keyFormula = obj.optString("keyFormula").trim(),
                        summary = summary,
                        memoryTip = obj.optString("memoryTip").trim()
                            .ifBlank { "Double-check units and boundary conditions under exam pressure." },
                        subject = obj.optString("subject").trim().ifBlank { fallbackSubject },
                        chapter = obj.optString("chapter").trim().ifBlank { fallbackChapter },
                        mastered = false
                    )
                }

                if (parsedCards.isEmpty()) {
                    throw IllegalStateException("No flashcards were generated. Please try again.")
                }

                FlashcardDeck(
                    userId = userId,
                    examId = exam.id,
                    examName = exam.name,
                    title = deckTitle,
                    subjectNames = subjectChapters.keys.toList(),
                    chapters = allChapters,
                    focus = focus.id,
                    cards = parsedCards.take(safeCount),
                    masteredCount = 0L
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Flashcard generation timed out", e)
            throw IllegalStateException("Flashcard generation timed out. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Flashcard generation failed", e)
            throw IllegalStateException(
                e.localizedMessage ?: "Failed to generate AI flashcards.",
                e
            )
        }
    }

    suspend fun moderateStudyChatMessage(
        text: String,
        attachmentLabel: String = "",
        attachmentType: String = "NONE",
        examName: String = "Competitive Exam",
        imageBitmap: Bitmap? = null
    ): PolicyCheckResult = withContext(Dispatchers.Default) {
        val localResult = ChatPolicyFilter.evaluateLocal(text, attachmentLabel)
        if (localResult.isViolation) return@withContext localResult

        // Skip remote call for very simple academic greetings or when only non-suspicious short text
        val trimmed = text.trim()
        if (trimmed.isEmpty() && imageBitmap == null && attachmentLabel.isBlank()) {
            return@withContext PolicyCheckResult(isViolation = false)
        }

        val promptText = """
            You are a strict child-safety & educational chat moderator for Kaizu, an Indian competitive-exam study app ($examName).
            Evaluate whether this student chat message or attachment violates the Sexual Content & Adult Video Policy:
            - Mark isSexualViolation = true if it sends, requests, links, or implies adult/pornographic videos, nudity, sexual solicitation, flirting/dating harassment, or sexually explicit slang (in English, Hindi, or Hinglish).
            - Mark isSexualViolation = false for legitimate academic questions (including NCERT Biology chapters like Human Reproduction, Reproductive Health, Gametogenesis, etc.), study notes, quiz doubts, and encouragement.

            Message text: "${trimmed.take(1000)}"
            Attachment type: "$attachmentType"
            Attachment description/name: "${attachmentLabel.take(200)}"
        """.trimIndent()

        try {
            withTimeout(15_000L) {
                val inputContent = withContext(Dispatchers.IO) {
                    content {
                        if (imageBitmap != null) image(imageBitmap)
                        text(promptText)
                    }
                }
                val response = moderationModel.generateContent(inputContent)
                val jsonString = response.text ?: return@withTimeout PolicyCheckResult(isViolation = false)
                val root = JSONObject(jsonString)
                val violated = root.optBoolean("isSexualViolation", false)
                val reason = root.optString("reason").trim()
                PolicyCheckResult(
                    isViolation = violated,
                    reason = if (violated) reason.ifBlank {
                        "Sexual content or adult media is strictly prohibited in Kaizu Study Chat."
                    } else ""
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "AI chat moderation unavailable, falling back to local policy filter", e)
            PolicyCheckResult(isViolation = false)
        }
    }
}
