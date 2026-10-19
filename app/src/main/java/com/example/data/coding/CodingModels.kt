package com.example.data.coding

enum class ProgrammingLanguageId(
    val key: String,
    val displayName: String,
    val shortBadge: String,
    val fileExtension: String,
    val runtimeLabel: String,
    val isPrimaryFeatured: Boolean,
    val tagline: String,
    val accentHex: Long
) {
    PYTHON(
        key = "python",
        displayName = "Python",
        shortBadge = "PY",
        fileExtension = "main.py",
        runtimeLabel = "Python 3.12 Interpreter",
        isPrimaryFeatured = true,
        tagline = "Clean syntax for AI, data science, automation & backend",
        accentHex = 0xFF3776ABL
    ),
    JAVASCRIPT(
        key = "javascript",
        displayName = "JavaScript",
        shortBadge = "JS",
        fileExtension = "app.js",
        runtimeLabel = "Isolated JS Engine (ES2024)",
        isPrimaryFeatured = true,
        tagline = "The language of interactive web apps & full-stack systems",
        accentHex = 0xFFF7DF1EL
    ),
    CPP(
        key = "cpp",
        displayName = "C++",
        shortBadge = "C++",
        fileExtension = "main.cpp",
        runtimeLabel = "GCC C++20 Sandboxed Compiler",
        isPrimaryFeatured = true,
        tagline = "High-performance systems, game engines & competitive coding",
        accentHex = 0xFF00599CL
    ),
    JAVA(
        key = "java",
        displayName = "Java",
        shortBadge = "JAVA",
        fileExtension = "Main.java",
        runtimeLabel = "OpenJDK 21 Runtime",
        isPrimaryFeatured = false,
        tagline = "Enterprise backends, Android foundations & object-oriented design",
        accentHex = 0xFFED8B00L
    ),
    C(
        key = "c",
        displayName = "C",
        shortBadge = "C",
        fileExtension = "main.c",
        runtimeLabel = "GCC C17 Sandboxed Compiler",
        isPrimaryFeatured = false,
        tagline = "Low-level memory mastery, operating systems & embedded hardware",
        accentHex = 0xFF5C6BC0L
    ),
    HTML_CSS(
        key = "html_css",
        displayName = "HTML & CSS",
        shortBadge = "WEB",
        fileExtension = "index.html",
        runtimeLabel = "Sandboxed Web Preview Renderer",
        isPrimaryFeatured = false,
        tagline = "Structure, responsive layouts & modern visual styling for the web",
        accentHex = 0xFFE44D26L
    );

    companion object {
        fun fromKey(key: String): ProgrammingLanguageId {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: PYTHON
        }
    }
}

enum class CodingSubTab(val label: String) {
    LEARN("Languages & Learn"),
    FALCON_CODE("Falcon Code (Execute)"),
    PRACTICE("Practice"),
    PROJECTS("Projects"),
    ROADMAP("90-Day Roadmap")
}

data class CodingLesson(
    val id: String,
    val languageId: ProgrammingLanguageId,
    val moduleNumber: Int,
    val moduleTitle: String,
    val lessonNumber: Int,
    val title: String,
    val difficulty: String,
    val conceptExplanation: String,
    val whyItExists: String,
    val simplerExplanation: String,
    val syntaxGuide: String,
    val workingExampleCode: String,
    val lineByLineWalkthrough: List<String>,
    val expectedOutput: String,
    val commonMistakes: List<String>,
    val realWorldExample: String,
    val exercisePrompt: String,
    val exerciseStarterCode: String,
    val exerciseSolutionCode: String,
    val exerciseExpectedOutputContains: List<String>,
    val exerciseRequiredInput: String = "",
    val exerciseHint: String,
    val quizQuestion: String,
    val quizOptions: List<String>,
    val quizCorrectIndex: Int,
    val quizWrongExplanation: String,
    val quizHint: String,
    val topicTag: String,
    val xpReward: Int = 30,
    val coinsReward: Int = 20
)

enum class ChallengeType(val displayTitle: String) {
    PREDICT_OUTPUT("Predict the Output"),
    FIND_THE_BUG("Find the Bug"),
    COMPLETE_THE_CODE("Complete the Code"),
    WRITE_FROM_SCRATCH("Write Code from Scratch"),
    MULTIPLE_CHOICE("Concept Check")
}

data class CodingTestCase(
    val input: String,
    val expectedOutput: String,
    val description: String,
    val isHidden: Boolean = false
)

data class TestCaseRunResult(
    val testCase: CodingTestCase,
    val passed: Boolean,
    val actualOutput: String,
    val errorDetail: String = ""
)

data class PracticeChallenge(
    val id: String,
    val languageId: ProgrammingLanguageId,
    val type: ChallengeType,
    val title: String,
    val difficulty: String,
    val topicTag: String,
    val prompt: String,
    val starterCode: String,
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = 0,
    val testCases: List<CodingTestCase> = emptyList(),
    val hint: String,
    val explanation: String,
    val solutionCode: String,
    val xpReward: Int = 35,
    val coinsReward: Int = 25
)

data class CodingProject(
    val id: String,
    val languageId: ProgrammingLanguageId,
    val title: String,
    val stage: String,
    val difficulty: String,
    val description: String,
    val objectives: List<String>,
    val starterCode: String,
    val sampleStdin: String = "",
    val expectedOutputKeywords: List<String>,
    val referenceSolution: String,
    val xpReward: Int = 80,
    val coinsReward: Int = 50
)

data class RoadmapStage(
    val monthNumber: Int,
    val title: String,
    val subtitle: String,
    val daysRange: String,
    val coreTopics: List<String>,
    val milestones: List<String>,
    val targetLessonCount: Int
)

data class CodeSnippetTemplate(
    val title: String,
    val description: String,
    val code: String,
    val sampleStdin: String = ""
)

data class CodingExecutionResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val executionTimeMs: Long,
    val runtimeUsed: String,
    val isTimeout: Boolean = false,
    val isHtmlPreview: Boolean = false,
    val htmlContent: String = "",
    val compilationStatus: String = if (exitCode == 0 && stderr.isBlank()) "Executed" else "Error",
    val errorLineNumber: Int? = null,
    val errorColumnNumber: Int? = null,
    val errorType: String = "",
    val errorSummary: String = "",
    val offendingLineText: String = "",
    val suggestedFixCode: String? = null,
    val suggestedFixExplanation: String? = null,
    val isEmptyCodeWarning: Boolean = false
) {
    val isSuccess: Boolean
        get() = exitCode == 0 && stderr.isBlank() && !isTimeout && !isEmptyCodeWarning

    val errorExplanation: String
        get() = suggestedFixExplanation?.takeIf { it.isNotBlank() } ?: errorSummary

    val errorLineContent: String
        get() = offendingLineText
}

enum class AiCodeAction(val label: String, val shortPrompt: String) {
    EXPLAIN_CODE("Explain Code", "Explain how this code works line by line in simple terms."),
    FIND_ERROR("Find the Error", "Analyze this code and its output/error to identify the exact bug."),
    FIX_CODE("Fix My Code", "Show how to fix the bug in this code and explain the root cause."),
    GIVE_HINT("Give Me a Hint", "Give a constructive hint without spoiling the entire answer."),
    EXPLAIN_OUTPUT("Explain Output", "Explain why the program produced this exact output or error."),
    IMPROVE_CODE("Improve Code", "Suggest cleaner, more idiomatic ways to write this code."),
    GENERATE_PRACTICE("Practice Question", "Create a targeted practice problem based on this code."),
    EXPLAIN_CONCEPT("Explain Concept", "Explain the underlying programming concept with an analogy."),
    STEP_BY_STEP("Step-by-Step Guide", "Walk through solving this problem step by step."),
    CONVERT_LANGUAGE("Convert Language", "Convert this code to another supported programming language.")
}

data class AiCodeAssistantResponse(
    val actionTitle: String,
    val explanation: String,
    val suggestedCode: String? = null,
    val targetLanguage: ProgrammingLanguageId? = null,
    val originalCode: String = "",
    val whatChangedBullets: List<String> = emptyList(),
    val whyItFailed: String = "",
    val tipToRemember: String = "",
    val changedLineNumbers: List<Int> = emptyList(),
    val errorLineNumber: Int? = null
)

data class UserCodingProgress(
    val selectedLanguage: ProgrammingLanguageId = ProgrammingLanguageId.PYTHON,
    val completedLessonIds: Set<String> = emptySet(),
    val completedChallengeIds: Set<String> = emptySet(),
    val completedProjectIds: Set<String> = emptySet(),
    val masteredTopics: Set<String> = emptySet(),
    val weakTopics: Set<String> = emptySet(),
    val dailyStudyMinutesGoal: Int = 30,
    val savedCodeByLanguage: Map<String, String> = emptyMap(),
    val codingXp: Int = 0,
    val totalExecutions: Int = 0
)
