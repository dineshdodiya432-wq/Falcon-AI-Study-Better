package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Falcon Scholar",
    val selectedGrade: Int = 6,
    val selectedLanguage: String = "Simple English",
    val curriculumTags: List<String> = listOf("Mathematics", "Science", "History & Geography"),
    val coins: Int = 150,
    val xp: Int = 120,
    val studyStreak: Int = 1,
    val quizStreak: Int = 1,
    val brokenStudyStreakToRecover: Int = 0,
    val brokenQuizStreakToRecover: Int = 0,
    val streakShields: Int = 1,
    val unlockedBadges: List<String> = listOf("first_launch", "curious_spark"),
    val reminderEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val reminderHour: Int = 17,
    val reminderMinute: Int = 30,
    val reminderDays: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
    val lastStudyDate: String = "2026-10-19",
    val lastQuizDate: String = "2026-10-19",
    val lastStreakCheckInDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class HomeworkSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val questionTitle: String = "",
    val subjectTag: String = "General",
    val gradeLevel: Int = 6,
    val language: String = "Simple English",
    val steps: List<String> = emptyList(),
    val finalAnswer: String = "",
    val keyConcept: String = "",
    val practiceQuestion: String = "",
    val practiceOptions: List<String> = emptyList(),
    val practiceCorrectIndex: Int = 0,
    val practiceCompleted: Boolean = false,
    val coinsEarned: Int = 20,
    val explanationParagraph: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val displayParagraph: String
        get() = when {
            explanationParagraph.isNotBlank() -> explanationParagraph
            finalAnswer.isNotBlank() -> finalAnswer
            steps.isNotEmpty() -> steps.joinToString("\n\n")
            else -> "I'm ready to answer your questions and explain any subject clearly!"
        }
}

@IgnoreExtraProperties
data class QuizAttempt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectTag: String = "Mathematics",
    val gradeLevel: Int = 6,
    val language: String = "Simple English",
    val score: Int = 0,
    val totalQuestions: Int = 5,
    val coinsEarned: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class SupportedLanguage(
    val name: String,
    val nativeLabel: String,
    val regionGroup: String,
    val flagEmoji: String,
    val localeCode: String = "en-US",
    val isRtl: Boolean = false,
    val greeting: String,
    val dashboardSubtitle: String,
    val askTutorPromptHint: String
)

data class CurriculumTopic(
    val tag: String,
    val category: String,
    val minGrade: Int,
    val maxGrade: Int,
    val samplePrompt: String
)

data class CosmicBadge(
    val id: String,
    val title: String,
    val description: String,
    val rewardCoins: Int
)

data class QuizQuestionItem(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val stepByStepExplanation: String,
    val conceptTip: String,
    val curriculumStandard: String = "Core Curriculum Standard",
    val hint: String = "Break the question into smaller parts and eliminate unlikely choices."
)

object LuminoCatalog {
    val quizDifficulties: List<String> = listOf(
        "Easy",
        "Medium",
        "Hard",
        "Extreme",
        "Impossible"
    )

    val quizQuestionCounts: List<Int> = listOf(3, 5, 10, 15, 20)

    val quizSubjects: List<String> = listOf(
        "Mathematics",
        "Science",
        "Physics",
        "Chemistry",
        "Biology",
        "English & Grammar",
        "History & Geography",
        "Coding & Logic",
        "General Knowledge"
    )

    fun coinsPerCorrectForDifficulty(difficulty: String): Int = when (difficulty.lowercase()) {
        "easy" -> 10
        "medium" -> 15
        "hard" -> 25
        "extreme" -> 40
        "impossible" -> 65
        else -> 15
    }

    fun getDailyStreakRewardCoins(day: Int): Int = when (day.coerceAtLeast(1)) {
        1 -> 25
        2 -> 30
        3 -> 40
        4 -> 50
        5 -> 60
        6 -> 75
        7 -> 100
        else -> 100
    }

    fun getStreakCongratulatoryTitle(streakDays: Int): String = when {
        streakDays <= 0 -> "Start Your Streak Today!"
        streakDays == 1 -> "Day 1 Complete — Great Start!"
        streakDays == 2 -> "Day 2 Milestone Reached! 🔥"
        streakDays == 3 -> "Day 3 Hat-Trick Streak! ⚡"
        streakDays == 4 -> "Day 4 Unstoppable Force! 🚀"
        streakDays == 5 -> "Day 5 High-Five Champion! 🌟"
        streakDays == 6 -> "Day 6 Solar Orbit Prep! 🪐"
        streakDays == 7 -> "7-Day Orbit Grand Master! 👑"
        else -> "Day $streakDays Grandmaster Legend! 🏆"
    }

    fun getStreakMotivationMessage(streakDays: Int): String = when {
        streakDays <= 0 -> "Complete a chat, quiz, or daily check-in today to ignite Day 1 of your streak!"
        streakDays == 1 -> "Day 1: Let's see how long you continue! Great start — you earned Star Coins to protect your journey."
        streakDays == 2 -> "Day 2: Two days in a row! You're building real momentum — keep the flame alive!"
        streakDays == 3 -> "Day 3: Hat-trick streak! 3 days of focused learning — your brain is leveling up fast."
        streakDays == 4 -> "Day 4: Four days strong! Consistency is turning into your superpower."
        streakDays == 5 -> "Day 5: High-five streak! 5 straight days of excellence — you're unstoppable!"
        streakDays == 6 -> "Day 6: Almost a full week! Just 1 more day to unlock the 7-Day Orbit badge!"
        streakDays == 7 -> "Day 7: One Full Week! You proved your dedication — how far can you push your record?"
        else -> "Day $streakDays: Unbelievable $streakDays-day streak! You're in the top 1% of Falcon AI scholars!"
    }

    fun inferSubjectFromText(text: String): String {
        val lower = text.lowercase()
        return when {
            listOf("solve", "equation", "math", "algebra", "fraction", "triangle", "area", "number", "add", "multiply", "calculus", "derivative", "+", "=", "x").any { lower.contains(it) } -> "Mathematics"
            listOf("force", "motion", "velocity", "gravity", "newton", "energy", "circuit", "physics").any { lower.contains(it) } -> "Physics"
            listOf("atom", "molecule", "acid", "bond", "periodic", "reaction", "chemistry", "element").any { lower.contains(it) } -> "Chemistry"
            listOf("plant", "cell", "photosynthesis", "dna", "animal", "ecosystem", "organ", "biology").any { lower.contains(it) } -> "Biology"
            listOf("planet", "moon", "solar", "science", "water", "weather").any { lower.contains(it) } -> "Science"
            listOf("code", "python", "loop", "algorithm", "function", "program", "javascript", "kotlin").any { lower.contains(it) } -> "Coding & Logic"
            listOf("history", "war", "ancient", "empire", "president", "country", "continent", "geography").any { lower.contains(it) } -> "History & Geography"
            listOf("grammar", "verb", "noun", "essay", "metaphor", "poem", "english", "sentence").any { lower.contains(it) } -> "English & Grammar"
            else -> "AI Tutor"
        }
    }

    fun isProfaneOrAngry(text: String): Boolean {
        if (text.isBlank()) return false
        val clean = text.lowercase().replace(Regex("[^a-z0-9\\s]"), " ")
        val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        val badWords = setOf(
            "fuck", "fucking", "fucked", "fucker", "fuk", "fck",
            "shit", "shitty", "bullshit",
            "bitch", "bitches",
            "ass", "asshole", "arse", "arsehole",
            "bastard", "dick", "piss", "crap",
            "damn", "dammit",
            "stfu", "wtf", "wth",
            "idiot", "idiots", "stupid", "dumb",
            "shut up", "shutup",
            "you suck", "u suck", "sucks",
            "trash", "garbage", "useless",
            "liar", "scam", "moron", "fool", "rubbish"
        )
        if (words.any { it in badWords }) return true
        val phrases = listOf(
            "shut up", "you suck", "u suck", "hate you", "hate u",
            "worst ai", "bad ai", "bad bot", "wrong answer",
            "are you dumb", "so dumb", "what the hell", "what the fuck"
        )
        return phrases.any { clean.contains(it) }
    }

    fun getBadWordsApologyExplanation(language: String = "Simple English"): String {
        return "Hey, what happened? 😟 Did Falcon AI make any mistake or give you an incorrect answer earlier?\n\nI am really sorry if something went wrong or felt frustrating! Falcon AI is here to support you and help you learn. Please tell me what happened or what went wrong with my previous response, and I'll do my best to fix it and solve it with you step-by-step! ✨"
    }

    val languages: List<SupportedLanguage> = listOf(
        SupportedLanguage(
            name = "Simple English",
            nativeLabel = "Simple English",
            regionGroup = "Global",
            flagEmoji = "🌍",
            localeCode = "en-US",
            greeting = "Welcome back to Falcon AI!",
            dashboardSubtitle = "Ask Falcon AI anything or test your skills in the Quiz Arena.",
            askTutorPromptHint = "Message Falcon AI..."
        ),
        SupportedLanguage(
            name = "Spanish",
            nativeLabel = "Español",
            regionGroup = "European",
            flagEmoji = "🇪🇸",
            localeCode = "es-ES",
            greeting = "¡Bienvenido de nuevo a Falcon AI!",
            dashboardSubtitle = "¿Listo para el descubrimiento paso a paso de hoy?",
            askTutorPromptHint = "Escribe cualquier pregunta a Falcon AI..."
        ),
        SupportedLanguage(
            name = "French",
            nativeLabel = "Français",
            regionGroup = "European",
            flagEmoji = "🇫🇷",
            localeCode = "fr-FR",
            greeting = "Bon retour sur Falcon AI !",
            dashboardSubtitle = "Prêt pour la découverte étape par étape d'aujourd'hui ?",
            askTutorPromptHint = "Posez une question à Falcon AI..."
        ),
        SupportedLanguage(
            name = "German",
            nativeLabel = "Deutsch",
            regionGroup = "European",
            flagEmoji = "🇩🇪",
            localeCode = "de-DE",
            greeting = "Willkommen zurück bei Falcon AI!",
            dashboardSubtitle = "Bereit für die heutige Schritt-für-Schritt-Entdeckung?",
            askTutorPromptHint = "Frage Falcon AI etwas..."
        ),
        SupportedLanguage(
            name = "Portuguese",
            nativeLabel = "Português",
            regionGroup = "European",
            flagEmoji = "🇵🇹",
            localeCode = "pt-BR",
            greeting = "Bem-vindo de volta ao Falcon AI!",
            dashboardSubtitle = "Pronto para a descoberta passo a passo de hoje?",
            askTutorPromptHint = "Pergunte qualquer coisa ao Falcon AI..."
        ),
        SupportedLanguage(
            name = "Hindi",
            nativeLabel = "हिन्दी (Hindi)",
            regionGroup = "South Asian",
            flagEmoji = "🇮🇳",
            localeCode = "hi-IN",
            greeting = "Falcon AI में आपका स्वागत है!",
            dashboardSubtitle = "आज के चरण-दर-चरण सीखने के लिए तैयार हैं?",
            askTutorPromptHint = "Falcon AI से कुछ भी पूछें..."
        ),
        SupportedLanguage(
            name = "Bengali",
            nativeLabel = "বাংলা (Bengali)",
            regionGroup = "South Asian",
            flagEmoji = "🇧🇩",
            localeCode = "bn-IN",
            greeting = "Falcon AI-তে স্বাগতম!",
            dashboardSubtitle = "আজকের ধাপে ধাপে শেখার জন্য প্রস্তুত?",
            askTutorPromptHint = "Falcon AI-কে যেকোনো প্রশ্ন করুন..."
        ),
        SupportedLanguage(
            name = "Tamil",
            nativeLabel = "தமிழ் (Tamil)",
            regionGroup = "South Asian",
            flagEmoji = "🇮🇳",
            localeCode = "ta-IN",
            greeting = "Falcon AI-க்கு மீண்டும் வருக!",
            dashboardSubtitle = "இன்றைய படிப்படியான கற்றலுக்குத் தயாரா?",
            askTutorPromptHint = "Falcon AI-யிடம் கேள்வி கேளுங்கள்..."
        ),
        SupportedLanguage(
            name = "Telugu",
            nativeLabel = "తెలుగు (Telugu)",
            regionGroup = "South Asian",
            flagEmoji = "🇮🇳",
            localeCode = "te-IN",
            greeting = "Falcon AI కి తిరిగి స్వాగతం!",
            dashboardSubtitle = "ఈ రోజు దశలవారీగా నేర్చుకోవడానికి సిద్ధంగా ఉన్నారా?",
            askTutorPromptHint = "Falcon AI ని ఏదైనా అడగండి..."
        ),
        SupportedLanguage(
            name = "Marathi",
            nativeLabel = "मराठी (Marathi)",
            regionGroup = "South Asian",
            flagEmoji = "🇮🇳",
            localeCode = "mr-IN",
            greeting = "Falcon AI मध्ये पुन्हा स्वागत आहे!",
            dashboardSubtitle = "आजच्या पायरी-पायरीने शिकण्यासाठी तयार आहात का?",
            askTutorPromptHint = "Falcon AI ला काहीही विचारा..."
        ),
        SupportedLanguage(
            name = "Mandarin",
            nativeLabel = "中文 (Mandarin)",
            regionGroup = "East Asian & Middle Eastern",
            flagEmoji = "🇨🇳",
            localeCode = "zh-CN",
            greeting = "欢迎回到 Falcon AI！",
            dashboardSubtitle = "准备好开始今天的分步探索了吗？",
            askTutorPromptHint = "向 Falcon AI 提问..."
        ),
        SupportedLanguage(
            name = "Japanese",
            nativeLabel = "日本語 (Japanese)",
            regionGroup = "East Asian & Middle Eastern",
            flagEmoji = "🇯🇵",
            localeCode = "ja-JP",
            greeting = "Falcon AI へおかえりなさい！",
            dashboardSubtitle = "今日のステップバイステップ学習を始めましょう！",
            askTutorPromptHint = "Falcon AI に質問する..."
        ),
        SupportedLanguage(
            name = "Korean",
            nativeLabel = "한국어 (Korean)",
            regionGroup = "East Asian & Middle Eastern",
            flagEmoji = "🇰🇷",
            localeCode = "ko-KR",
            greeting = "Falcon AI에 돌아온 것을 환영해요!",
            dashboardSubtitle = "오늘의 단계별 학습을 시작할 준비가 되었나요?",
            askTutorPromptHint = "Falcon AI에게 무엇이든 물어보세요..."
        ),
        SupportedLanguage(
            name = "Arabic",
            nativeLabel = "العربية (Arabic)",
            regionGroup = "East Asian & Middle Eastern",
            flagEmoji = "🇸🇦",
            localeCode = "ar-SA",
            isRtl = true,
            greeting = "مرحباً بعودتك إلى Falcon AI!",
            dashboardSubtitle = "هل أنت مستعد لاكتشاف اليوم خطوة بخطوة؟",
            askTutorPromptHint = "اسأل Falcon AI أي سؤال..."
        )
    )

    val curriculumTopics: List<CurriculumTopic> = listOf(
        CurriculumTopic("Mathematics", "Mathematics", 1, 12, "Solve 3x + 7 = 22 and explain every step clearly."),
        CurriculumTopic("Science", "Science", 1, 12, "How do plants turn sunlight into food during photosynthesis?"),
        CurriculumTopic("Physics", "Science", 6, 12, "Explain Newton's Three Laws of Motion using a skateboard example."),
        CurriculumTopic("Chemistry", "Science", 7, 12, "How do covalent bonds differ from ionic bonds in water and salt?"),
        CurriculumTopic("Biology", "Science", 4, 12, "How do mitochondria generate ATP energy inside cells?"),
        CurriculumTopic("English & Grammar", "Languages", 1, 12, "What is the difference between a metaphor, a simile, and personification?"),
        CurriculumTopic("History & Geography", "Humanities", 3, 12, "Why was the printing press such an important invention in history?"),
        CurriculumTopic("Coding & Logic", "STEM", 3, 12, "How does a 'for loop' work in programming? Explain with a robot counting steps."),
        CurriculumTopic("General Knowledge", "General", 1, 12, "Why does the Moon have phases as it orbits Earth?")
    )

    val badges: List<CosmicBadge> = listOf(
        CosmicBadge("first_launch", "First Falcon Flight", "Started your learning journey on Falcon AI.", 25),
        CosmicBadge("curious_spark", "Curious Spark", "Asked your first question in Falcon AI Chat.", 30),
        CosmicBadge("homework_hero", "Homework Hero", "Used Falcon AI Chat to master step-by-step explanations.", 50),
        CosmicBadge("quiz_supernova", "Quiz Supernova", "Scored 80% or higher on a Custom or Daily Quiz.", 60),
        CosmicBadge("polyglot_scholar", "Polyglot Star", "Explored lessons or quizzes in 2 or more languages.", 45),
        CosmicBadge("streak_guardian", "Time-Warp Guardian", "Protected or recovered a learning streak using Star Coins.", 50),
        CosmicBadge("seven_day_orbit", "7-Day Solar Orbit", "Reached a 7-day Study or Quiz Streak.", 100)
    )

    fun gradeStageDescription(grade: Int): String = when (grade) {
        in 1..2 -> "Early Primary Foundations (Ages 6–8)"
        in 3..5 -> "Upper Elementary Discovery (Ages 8–11)"
        in 6..8 -> "Middle School Core Mastery (Ages 11–14)"
        in 9..10 -> "High School Honours Prep (Ages 14–16)"
        else -> "Senior Advanced & Pre-University (Ages 16–18)"
    }

    fun getCurriculumQuizForGrade(
        gradeLevel: Int,
        subjectTag: String,
        language: String,
        difficulty: String = "Medium",
        questionCount: Int = 5
    ): List<QuizQuestionItem> {
        val g = gradeLevel.coerceIn(1, 12)
        val targetCount = questionCount.coerceIn(1, 20)
        val diffBadge = "[$difficulty • Grade $g • $subjectTag]"

        val basePool = listOf(
            when (difficulty.lowercase()) {
                "easy" -> QuizQuestionItem(
                    question = "$diffBadge ($language) If you have ${g * 3} star coins and earn ${g * 2} more in $subjectTag, how many coins do you have in total?",
                    options = listOf("${g * 5}", "${g * 4}", "${g * 6}", "${g * 3 + 1}"),
                    correctIndex = 0,
                    stepByStepExplanation = "Step 1: Add the starting coins (${g * 3}) and the new coins (${g * 2}). Step 2: ${g * 3} + ${g * 2} = ${g * 5}.",
                    conceptTip = "Foundational $subjectTag Reasoning ($difficulty • $language)",
                    curriculumStandard = "Grade $g • $subjectTag • $difficulty Level",
                    hint = "Add ${g * 3} and ${g * 2} together step by step."
                )
                "hard" -> QuizQuestionItem(
                    question = "$diffBadge ($language) Solve for x in the multi-step Grade $g $subjectTag challenge: 3(x + $g) = ${3 * g + 21}",
                    options = listOf("x = 7", "x = $g", "x = 9", "x = 14"),
                    correctIndex = 0,
                    stepByStepExplanation = "Step 1: Divide both sides by 3 -> x + $g = ${g + 7}. Step 2: Subtract $g from both sides -> x = 7.",
                    conceptTip = "Multi-Step $subjectTag Problem Solving ($difficulty • $language)",
                    curriculumStandard = "Grade $g • $subjectTag • $difficulty Level",
                    hint = "First divide both sides by 3, then isolate x."
                )
                "extreme" -> QuizQuestionItem(
                    question = "$diffBadge ($language) In an Extreme $subjectTag Olympiad problem for Grade $g: If f(n) = n² + 2n + 1, what is the value of f(${g + 2})?",
                    options = listOf("${(g + 3) * (g + 3)}", "${(g + 2) * (g + 2)}", "${g * g + 4}", "${(g + 4) * (g + 4)}"),
                    correctIndex = 0,
                    stepByStepExplanation = "Step 1: Notice that n² + 2n + 1 factors into (n + 1)². Step 2: For n = ${g + 2}, (n + 1)² = (${g + 3})² = ${(g + 3) * (g + 3)}.",
                    conceptTip = "Algebraic Identities & Olympiad Shortcuts ($difficulty • $language)",
                    curriculumStandard = "Grade $g • $subjectTag • $difficulty Challenge",
                    hint = "Factor n² + 2n + 1 as (n + 1)² before plugging in the number!"
                )
                "impossible" -> QuizQuestionItem(
                    question = "$diffBadge ($language) IMPOSSIBLE BOSS QUESTION: A sequence satisfies a₁ = 2 and aₙ = 2·aₙ₋₁ + 1. What is the closed-form value of a₅ in this Grade $g $subjectTag master test?",
                    options = listOf("47", "31", "63", "39"),
                    correctIndex = 0,
                    stepByStepExplanation = "Step 1: Compute term by term: a₁ = 2, a₂ = 2(2)+1 = 5, a₃ = 2(5)+1 = 11, a₄ = 2(11)+1 = 23. Step 2: a₅ = 2(23)+1 = 47.",
                    conceptTip = "Non-Homogeneous Recurrence Relations ($difficulty • $language)",
                    curriculumStandard = "Grade $g • $subjectTag • Impossible Grandmaster Tier",
                    hint = "Calculate a₂, a₃, a₄ step by step by doubling the previous term and adding 1!"
                )
                else -> when (g) {
                    1 -> QuizQuestionItem(
                        question = "$diffBadge ($language) Maya has 8 red apples and her friend gives her 6 green apples. How many apples does Maya have in all?",
                        options = listOf("14 apples", "12 apples", "15 apples", "10 apples"),
                        correctIndex = 0,
                        stepByStepExplanation = "Step 1: Start at 8 and make 10 by adding 2 from the 6 (8 + 2 = 10). Step 2: Add the remaining 4 (10 + 4 = 14).",
                        conceptTip = "Make-Ten Addition Strategy ($language)",
                        curriculumStandard = "Grade 1 • $subjectTag • $difficulty Level",
                        hint = "First add 2 to 8 to reach 10, then add what is left from 6!"
                    )
                    2 -> QuizQuestionItem(
                        question = "$diffBadge ($language) What is the value of the digit 7 in the number 473?",
                        options = listOf("70 (Seventy)", "7 (Seven)", "700 (Seven Hundred)", "47"),
                        correctIndex = 0,
                        stepByStepExplanation = "Step 1: Look at the place value columns (Hundreds = 4, Tens = 7, Ones = 3). Step 2: 7 tens equals 70.",
                        conceptTip = "Place Value: Hundreds, Tens & Ones ($language)",
                        curriculumStandard = "Grade 2 • $subjectTag • $difficulty Level",
                        hint = "Check whether 7 is in the ones, tens, or hundreds spot."
                    )
                    else -> QuizQuestionItem(
                        question = "$diffBadge ($language) Solve the linear relationship for Grade $g $subjectTag: 2x + ${g * 2} = ${g * 6}",
                        options = listOf("x = ${g * 2}", "x = $g", "x = ${g * 3}", "x = ${g * 4}"),
                        correctIndex = 0,
                        stepByStepExplanation = "Step 1: Subtract ${g * 2} from both sides -> 2x = ${g * 4}. Step 2: Divide by 2 -> x = ${g * 2}.",
                        conceptTip = "Core $subjectTag Mastery ($difficulty • $language)",
                        curriculumStandard = "Grade $g • $subjectTag • $difficulty Level",
                        hint = "First subtract ${g * 2} from ${g * 6}, then divide by 2."
                    )
                }
            },
            QuizQuestionItem(
                question = "$diffBadge ($language) Which statement best explains how energy is conserved or transformed in a Grade $g $subjectTag system?",
                options = listOf(
                    "Total energy remains constant as it transforms from one form to another",
                    "Energy vanishes completely after a single reaction",
                    "Energy is created out of nothing without a source",
                    "Systems never exchange energy with their surroundings"
                ),
                correctIndex = 0,
                stepByStepExplanation = "Step 1: By the Law of Conservation, energy cannot be created or destroyed. Step 2: It only converts between forms (such as potential, kinetic, thermal, or chemical).",
                conceptTip = "Conservation & Systems Analysis ($difficulty • $language)",
                curriculumStandard = "Grade $g • $subjectTag • $difficulty Standard",
                hint = "Think of the fundamental conservation law in science and logic."
            ),
            QuizQuestionItem(
                question = "$diffBadge ($language) A scholar solves $g problems every 10 minutes at $difficulty difficulty. How many problems do they complete in 40 minutes?",
                options = listOf(
                    "${g * 4} problems",
                    "${g * 2} problems",
                    "${g + 4} problems",
                    "${g * 5} problems"
                ),
                correctIndex = 0,
                stepByStepExplanation = "Step 1: 40 minutes contains four 10-minute intervals (40 ÷ 10 = 4). Step 2: Multiply $g problems by 4 = ${g * 4} problems.",
                conceptTip = "Proportional Scaling ($difficulty • $language)",
                curriculumStandard = "Grade $g • $subjectTag • $difficulty Standard",
                hint = "Find how many 10-minute blocks fit inside 40 minutes, then multiply by $g."
            ),
            QuizQuestionItem(
                question = "$diffBadge ($language) Why is breaking a complex $subjectTag problem into smaller logical steps effective at $difficulty difficulty?",
                options = listOf(
                    "Each step can be verified independently, preventing compounding errors",
                    "It removes the need to read the question",
                    "It changes the constants of the problem",
                    "It skips the final conclusion"
                ),
                correctIndex = 0,
                stepByStepExplanation = "Step 1: Decomposition isolates each logical operation. Step 2: Verifying intermediate results ensures the final solution is accurate.",
                conceptTip = "Analytical Decomposition ($difficulty • $language)",
                curriculumStandard = "Grade $g • $subjectTag • $difficulty Standard",
                hint = "Think about how step-by-step reasoning makes hard problems manageable."
            ),
            QuizQuestionItem(
                question = "$diffBadge ($language) In a Grade $g $subjectTag experiment, a value doubles every step starting from 3. What is its value after 3 doublings?",
                options = listOf("24", "12", "18", "9"),
                correctIndex = 0,
                stepByStepExplanation = "Step 1: Start with 3. First doubling: 3 × 2 = 6. Step 2: Second doubling: 6 × 2 = 12. Step 3: Third doubling: 12 × 2 = 24.",
                conceptTip = "Exponential Growth Patterns ($difficulty • $language)",
                curriculumStandard = "Grade $g • $subjectTag • $difficulty Standard",
                hint = "Multiply 3 by 2 three times: 3 -> 6 -> 12 -> ?"
            )
        )

        return (0 until targetCount).map { idx ->
            val template = basePool[idx % basePool.size]
            if (idx < basePool.size) {
                template
            } else {
                val multiplier = idx + 2
                QuizQuestionItem(
                    question = "$diffBadge ($language • Q${idx + 1}) In a $difficulty $subjectTag challenge for Grade $g, if a pattern grows by ${g + multiplier} each step starting from 0, what is its value at step 4?",
                    options = listOf(
                        "${(g + multiplier) * 4}",
                        "${(g + multiplier) * 3}",
                        "${(g + multiplier) * 5}",
                        "${g + multiplier + 4}"
                    ),
                    correctIndex = 0,
                    stepByStepExplanation = "Step 1: Each step adds ${g + multiplier}. Step 2: After 4 steps, multiply ${g + multiplier} × 4 = ${(g + multiplier) * 4}.",
                    conceptTip = "Linear Sequences & Patterns ($difficulty • $language)",
                    curriculumStandard = "Grade $g • $subjectTag • $difficulty Question #${idx + 1}",
                    hint = "Multiply ${g + multiplier} by 4!"
                )
            }
        }
    }
}
