package com.example.data.remote

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.LuminoCatalog
import com.example.data.model.QuizQuestionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: Content? = null
)

@Serializable
data class StructuredHomeworkExplanation(
    val questionTitle: String = "",
    val detectedSubject: String = "AI Tutor",
    val explanationParagraph: String = "",
    val steps: List<String> = emptyList(),
    val finalAnswer: String = "",
    val keyConcept: String = ""
)

@Serializable
data class StructuredQuizQuestion(
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val stepByStepExplanation: String = "",
    val conceptTip: String = "",
    val curriculumStandard: String = "",
    val hint: String = ""
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

class GeminiTutorService {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    private val preferredModels = listOf(
        "gemini-3.5-flash",
        "gemini-flash-latest",
        "gemini-3.1-pro-preview"
    )

    private fun Bitmap.toBase64Jpeg(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun explainHomeworkStepByStep(
        questionText: String,
        homeworkBitmap: Bitmap?,
        gradeLevel: Int,
        language: String
    ): StructuredHomeworkExplanation = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val inferredSubject = LuminoCatalog.inferSubjectFromText(questionText)
        val prompt = buildString {
            appendLine("You are Falcon AI, a friendly, helpful, highly articulate AI tutor and learning companion for Grade $gradeLevel students.")
            appendLine("Your identity is strictly Falcon AI. NEVER call yourself ChatGPT, Gemini, or any other assistant.")
            appendLine("Automatically understand the subject and topic directly from the student's text or attached image — do NOT ask the student to pick a subject.")
            appendLine("Target Explanation Language: $language (write the ENTIRE explanation in fluent $language).")
            appendLine("CRITICAL FORMATTING RULE: Write a comprehensive, beautifully written, natural explanation in fluent, conversational PARAGRAPH form. Do NOT use 'Step 1', 'Step 2', or numbered step breakdown. Instead, explain the context, reasoning, and resolution in smooth, coherent, and engaging paragraphs.")
            appendLine("CRITICAL USER EMPATHY RULE: If the student uses bad words, vulgarity, swearing, insults, or expresses anger/frustration (such as 'what the hell', 'you suck', 'stupid', 'fuck', 'shit', 'idiot', 'useless', 'wrong answer', etc.):")
            appendLine("DO NOT scold, punish, or be defensive. Instead, respond with genuine warmth, care, and humility. Ask them what happened and whether Falcon AI made any mistake or gave an incorrect or confusing explanation earlier. Apologize sincerely for any confusion, invite them to tell Falcon AI what went wrong so it can be fixed immediately, and reassure them that Falcon AI is here to help them succeed.")
            appendLine("Adjust vocabulary and depth appropriately for a Grade $gradeLevel student (${LuminoCatalog.gradeStageDescription(gradeLevel)}).")
            if (questionText.isNotBlank()) {
                appendLine("Student's Message: $questionText")
            } else {
                appendLine("Solve and explain the problem shown in the attached camera/gallery image in clear paragraphs.")
            }
            appendLine("Provide a concise 'questionTitle', the automatically inferred 'detectedSubject', and the complete, well-reasoned solution in 'explanationParagraph'. Do NOT include any quiz questions.")
        }

        val parts = mutableListOf<Part>()
        parts.add(Part(text = prompt))
        if (homeworkBitmap != null) {
            parts.add(
                Part(
                    inlineData = InlineData(
                        mimeType = "image/jpeg",
                        data = homeworkBitmap.toBase64Jpeg()
                    )
                )
            )
        }

        val schema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("questionTitle") { put("type", "STRING") }
                putJsonObject("detectedSubject") { put("type", "STRING") }
                putJsonObject("explanationParagraph") { put("type", "STRING") }
            }
            putJsonArray("required") {
                add(JsonPrimitive("questionTitle"))
                add(JsonPrimitive("detectedSubject"))
                add(JsonPrimitive("explanationParagraph"))
            }
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            val requestBody = GenerateContentRequest(
                contents = listOf(Content(parts = parts)),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = schema,
                    temperature = 0.4f
                )
            )
            for (modelName in preferredModels) {
                try {
                    val response = apiService.generateContent(
                        model = modelName,
                        apiKey = apiKey,
                        request = requestBody
                    )
                    val rawText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!rawText.isNullOrBlank()) {
                        val parsed = json.decodeFromString<StructuredHomeworkExplanation>(rawText)
                        if (parsed.explanationParagraph.isNotBlank() || parsed.steps.isNotEmpty()) {
                            val paragraph = parsed.explanationParagraph.ifBlank {
                                parsed.steps.joinToString("\n\n")
                            }
                            return@withContext parsed.copy(
                                detectedSubject = parsed.detectedSubject.ifBlank { inferredSubject },
                                explanationParagraph = paragraph,
                                steps = listOf(paragraph),
                                finalAnswer = paragraph
                            )
                        }
                    }
                } catch (_: Exception) {
                    // Try next Gemini model in chain
                }
            }
        }

        buildCuratedExplanation(
            questionText = questionText,
            hasImage = homeworkBitmap != null,
            gradeLevel = gradeLevel,
            detectedSubject = inferredSubject,
            language = language
        )
    }

    suspend fun generateDailyQuiz(
        gradeLevel: Int,
        subjectTag: String,
        language: String,
        difficulty: String = "Medium",
        questionCount: Int = 5
    ): List<QuizQuestionItem> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val targetCount = questionCount.coerceIn(1, 20)
        val stageDesc = LuminoCatalog.gradeStageDescription(gradeLevel)
        val difficultyInstruction = when (difficulty.lowercase()) {
            "easy" -> "Easy difficulty: foundational concepts, warm-up confidence builders, direct 1-step recall or calculation."
            "medium" -> "Medium difficulty: standard grade-level curriculum application with 2-step reasoning."
            "hard" -> "Hard difficulty: challenging multi-step word problems requiring deeper analytical thinking."
            "extreme" -> "Extreme difficulty: honours & Olympiad-level multi-concept synthesis problems."
            "impossible" -> "Impossible difficulty: ultimate grandmaster challenge questions that test deep conceptual mastery and edge cases."
            else -> "Medium difficulty: standard curriculum mastery."
        }

        val prompt = """
            You are Falcon AI. Generate a $targetCount-question multiple-choice Quiz for a Grade $gradeLevel student ($stageDesc).
            Subject: "$subjectTag"
            Difficulty Level: "$difficulty" ($difficultyInstruction)
            Write all $targetCount questions, 4 options per question, stepByStepExplanation, conceptTip, curriculumStandard (e.g. "Grade $gradeLevel • $subjectTag • $difficulty"), and hint in $language.
        """.trimIndent()

        val schema = buildJsonObject {
            put("type", "ARRAY")
            putJsonObject("items") {
                put("type", "OBJECT")
                putJsonObject("properties") {
                    putJsonObject("question") { put("type", "STRING") }
                    putJsonObject("options") {
                        put("type", "ARRAY")
                        putJsonObject("items") { put("type", "STRING") }
                    }
                    putJsonObject("correctIndex") { put("type", "INTEGER") }
                    putJsonObject("stepByStepExplanation") { put("type", "STRING") }
                    putJsonObject("conceptTip") { put("type", "STRING") }
                    putJsonObject("curriculumStandard") { put("type", "STRING") }
                    putJsonObject("hint") { put("type", "STRING") }
                }
                putJsonArray("required") {
                    add(JsonPrimitive("question"))
                    add(JsonPrimitive("options"))
                    add(JsonPrimitive("correctIndex"))
                    add(JsonPrimitive("stepByStepExplanation"))
                    add(JsonPrimitive("conceptTip"))
                    add(JsonPrimitive("curriculumStandard"))
                    add(JsonPrimitive("hint"))
                }
            }
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            val requestBody = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = schema,
                    temperature = 0.5f
                )
            )
            for (modelName in preferredModels) {
                try {
                    val response = apiService.generateContent(
                        model = modelName,
                        apiKey = apiKey,
                        request = requestBody
                    )
                    val rawText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!rawText.isNullOrBlank()) {
                        val parsed = json.decodeFromString<List<StructuredQuizQuestion>>(rawText)
                        if (parsed.isNotEmpty()) {
                            return@withContext parsed.take(targetCount).map {
                                QuizQuestionItem(
                                    question = it.question,
                                    options = if (it.options.size >= 2) it.options else listOf("Option A", "Option B", "Option C", "Option D"),
                                    correctIndex = it.correctIndex.coerceIn(0, (it.options.size - 1).coerceAtLeast(0)),
                                    stepByStepExplanation = it.stepByStepExplanation,
                                    conceptTip = it.conceptTip,
                                    curriculumStandard = it.curriculumStandard.ifBlank { "Grade $gradeLevel • $subjectTag • $difficulty" },
                                    hint = it.hint.ifBlank { "Break the question into steps and check the units." }
                                )
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Try next Gemini model in chain
                }
            }
        }

        LuminoCatalog.getCurriculumQuizForGrade(
            gradeLevel = gradeLevel,
            subjectTag = subjectTag,
            language = language,
            difficulty = difficulty,
            questionCount = targetCount
        )
    }

    private fun buildCuratedExplanation(
        questionText: String,
        hasImage: Boolean,
        gradeLevel: Int,
        detectedSubject: String,
        language: String
    ): StructuredHomeworkExplanation {
        if (LuminoCatalog.isProfaneOrAngry(questionText)) {
            val apology = LuminoCatalog.getBadWordsApologyExplanation(language)
            return StructuredHomeworkExplanation(
                questionTitle = "Hey, what happened?",
                detectedSubject = "Falcon AI Support",
                explanationParagraph = apology,
                steps = listOf(apology),
                finalAnswer = apology,
                keyConcept = "Falcon AI Student Care"
            )
        }

        val cleanTitle = when {
            questionText.isNotBlank() && hasImage -> "📷 Photo + Question: ${questionText.trim()}"
            questionText.isNotBlank() -> questionText.trim()
            hasImage -> "📷 Camera Problem (Grade $gradeLevel)"
            else -> "Homework Question (Grade $gradeLevel)"
        }
        val paragraph = when {
            cleanTitle.contains("photosynthesis", ignoreCase = true) || cleanTitle.contains("plant", ignoreCase = true) ->
                "Plants make their own food through a wonderful natural process known as photosynthesis. Inside their green leaves, tiny cell structures called chloroplasts use the green pigment chlorophyll to capture energy directly from sunlight. At the same time, roots drink up water from the soil, and microscopic leaf pores called stomata absorb carbon dioxide from the surrounding air. Using solar energy, the plant recombines these ingredients into glucose—a rich sugar that fuels the plant's growth—while releasing clean oxygen back into the atmosphere for living creatures to breathe."
            cleanTitle.contains("3x", ignoreCase = true) || cleanTitle.contains("solve", ignoreCase = true) || cleanTitle.contains("=", ignoreCase = true) ->
                "To solve an algebraic equation like 3x + 7 = 22, the core principle is to isolate the variable x by keeping both sides balanced. We begin by undoing the added constant: subtracting 7 from both sides gives 3x = 15. Next, since 3 is multiplied by x, we perform the inverse operation and divide both sides by 3. Because 15 divided by 3 equals 5, we arrive at x = 5. We can verify our solution immediately by substituting 5 back into the original expression: 3(5) + 7 equals 15 + 7 = 22, confirming the solution is exactly right."
            cleanTitle.contains("newton", ignoreCase = true) || cleanTitle.contains("motion", ignoreCase = true) ->
                "Sir Isaac Newton described how things move through three fundamental laws of motion. The first law, known as the law of inertia, states that any object will remain at rest or keep moving in a straight line at constant speed unless an unbalanced force pushes or pulls on it. The second law explains that acceleration depends directly on the net force applied and inversely on the object's mass (F = ma), which is why pushing a heavy cart takes far more effort than pushing a light ball. Finally, the third law shows that forces always come in matched pairs: whenever one object pushes against another, the second object pushes back with equal force in the opposite direction."
            cleanTitle.contains("fraction", ignoreCase = true) ->
                "When adding fractions with different denominators like 2/3 and 1/4, the first step is finding a common denominator so the pieces represent equal parts of a whole. The least common multiple of 3 and 4 is 12. We convert 2/3 into twelfths by multiplying the top and bottom by 4, which gives 8/12. We convert 1/4 into twelfths by multiplying the top and bottom by 3, yielding 3/12. Now that the parts match, we simply add the numerators together: 8/12 plus 3/12 equals 11/12."
            else ->
                "In $detectedSubject for Grade $gradeLevel, understanding \"$cleanTitle\" comes down to connecting the core principles with clear reasoning. When we look closely at what the problem is asking, we can organize our known information and apply the foundational rules systematically. Every part of the concept connects logically to the final conclusion, giving you a deep and intuitive grasp of how the solution works."
        }
        return StructuredHomeworkExplanation(
            questionTitle = cleanTitle,
            detectedSubject = detectedSubject,
            explanationParagraph = paragraph,
            steps = listOf(paragraph),
            finalAnswer = paragraph,
            keyConcept = "$detectedSubject Insight ($language)"
        )
    }
}
