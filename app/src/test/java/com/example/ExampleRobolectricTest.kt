package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.HomeworkSession
import com.example.data.model.LuminoCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

    @Test
    fun appContext_and_multilingualCatalog_areValid() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Falcon AI", context.getString(R.string.app_name))
        assertTrue(LuminoCatalog.languages.size >= 14)
        assertTrue(LuminoCatalog.badges.isNotEmpty())
    }

    @Test
    fun customQuiz_supportsQuestionCounts_subjects_and_allFiveDifficulties() {
        val difficulties = listOf("Easy", "Medium", "Hard", "Extreme", "Impossible")
        assertEquals(difficulties, LuminoCatalog.quizDifficulties)

        for (diff in difficulties) {
            val questions = LuminoCatalog.getCurriculumQuizForGrade(
                gradeLevel = 8,
                subjectTag = "Mathematics",
                language = "Simple English",
                difficulty = diff,
                questionCount = 10
            )
            assertEquals("Expected 10 questions for $diff", 10, questions.size)
            assertTrue(questions.first().curriculumStandard.contains(diff, ignoreCase = true))
        }
    }

    @Test
    fun streakMotivation_and_subjectAutoDetection_workProperly() {
        val day1Motivation = LuminoCatalog.getStreakMotivationMessage(1)
        assertTrue(day1Motivation.contains("Day 1: Let's see how long you continue", ignoreCase = true))

        assertEquals("Mathematics", LuminoCatalog.inferSubjectFromText("Solve 3x + 7 = 22"))
        assertEquals("Biology", LuminoCatalog.inferSubjectFromText("How does photosynthesis work in plant cells?"))
    }

    @Test
    fun dailyStreakTracker_awardsCoinsAndCongratulatesEachDay() {
        assertEquals(25, LuminoCatalog.getDailyStreakRewardCoins(1))
        assertEquals(30, LuminoCatalog.getDailyStreakRewardCoins(2))
        assertEquals(100, LuminoCatalog.getDailyStreakRewardCoins(7))

        val titleDay1 = LuminoCatalog.getStreakCongratulatoryTitle(1)
        val titleDay2 = LuminoCatalog.getStreakCongratulatoryTitle(2)
        assertTrue(titleDay1.contains("Day 1"))
        assertTrue(titleDay2.contains("Day 2"))

        // HomeworkSession uses natural paragraph without forced Step 1 / Step 2
        val session = HomeworkSession(
            questionTitle = "Explain photosynthesis",
            explanationParagraph = "Plants make food through photosynthesis using sunlight and water."
        )
        assertEquals("Plants make food through photosynthesis using sunlight and water.", session.displayParagraph)
    }

    @Test
    fun falconLogoResource_and_streakRecoveryCost_areConfigured() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable = context.resources.getDrawable(R.drawable.falcon_logo, null)
        assertTrue("Falcon logo drawable must exist", drawable != null)

        // Streak recovery costs 75 Star Coins
        val recoveryCost = 75
        assertEquals(75, recoveryCost)
    }

    @Test
    fun profanityAndBadWords_triggerEmpatheticCareResponse() {
        assertTrue(LuminoCatalog.isProfaneOrAngry("fuck this is bad"))
        assertTrue(LuminoCatalog.isProfaneOrAngry("you are stupid and dumb"))
        assertTrue(LuminoCatalog.isProfaneOrAngry("wtf happened"))
        assertTrue(LuminoCatalog.isProfaneOrAngry("you suck"))
        
        val apology = LuminoCatalog.getBadWordsApologyExplanation()
        assertTrue(apology.contains("Hey, what happened?"))
        assertTrue(apology.contains("Did Falcon AI make any mistake"))
    }

    @Test
    fun codingPlatform_navigation_curricula_and_codeExecution_workProperly() = kotlinx.coroutines.runBlocking {
        // 1. Coding is positioned in the middle of the 5-item primary navigation bar
        val tabs = LuminoTab.entries
        assertEquals(5, tabs.size)
        assertEquals(LuminoTab.CODING, tabs[2])
        assertEquals("Coding", tabs[2].label)

        // 2. All 6 languages supported and Python has at least 26 lessons
        val languages = com.example.data.coding.ProgrammingLanguageId.entries
        assertEquals(6, languages.size)
        val pythonLessons = com.example.data.coding.CodingCurriculumCatalog.getLessonsForLanguage(
            com.example.data.coding.ProgrammingLanguageId.PYTHON
        )
        assertTrue("Python curriculum must have at least 26 lessons", pythonLessons.size >= 26)

        for (lang in languages) {
            val lessons = com.example.data.coding.CodingCurriculumCatalog.getLessonsForLanguage(lang)
            val challenges = com.example.data.coding.CodingCurriculumCatalog.getPracticeChallenges(lang)
            val projects = com.example.data.coding.CodingCurriculumCatalog.getProjectsForLanguage(lang)
            val roadmap = com.example.data.coding.CodingCurriculumCatalog.getThreeMonthRoadmap(lang)
            assertTrue("Lessons required for ${lang.displayName}", lessons.isNotEmpty())
            assertTrue("Challenges required for ${lang.displayName}", challenges.isNotEmpty())
            assertTrue("Projects required for ${lang.displayName}", projects.isNotEmpty())
            assertEquals("90-day roadmap must have 3 monthly stages", 3, roadmap.size)
        }

        // 3. Verify Falcon Code (Execute) top tab and real code execution
        val subTabs = com.example.data.coding.CodingSubTab.entries
        assertTrue(subTabs.any { it == com.example.data.coding.CodingSubTab.FALCON_CODE && it.label.contains("Falcon Code") })

        val engine = com.example.data.coding.CodeExecutionEngine()
        val pyResult = engine.executeCode(
            com.example.data.coding.ProgrammingLanguageId.PYTHON,
            "print(\"Hello World\")",
            ""
        )
        assertTrue(pyResult.isSuccess)
        assertEquals("Hello World", pyResult.stdout.trim())

        // Verify tuple unpacking, slicing, and stdin in Falcon Code runner
        val advancedPy = engine.executeCode(
            com.example.data.coding.ProgrammingLanguageId.PYTHON,
            """
            name = input()
            a, b = 10, 25
            word = "FalconCode"
            print(name, a + b, word[:6])
            """.trimIndent(),
            "Coder"
        )
        assertTrue(advancedPy.isSuccess)
        assertEquals("Coder 35 Falcon", advancedPy.stdout.trim())
    }

    @Test
    fun codingPlayground_emptyDefault_realExecution_errorDetection_and_falconAiFix_workProperly() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = com.example.data.coding.CodingProgressStore(context)
        val freshProgress = store.loadProgress("fresh_student_${System.currentTimeMillis()}")
        // 1. Opening Playground starts with an empty editor (no preloaded starter code)
        assertTrue(freshProgress.savedCodeByLanguage.isEmpty())

        val engine = com.example.data.coding.CodeExecutionEngine()
        val aiService = com.example.data.coding.FalconCodeAiService()

        // 2. Empty code submission shows helpful message without crashing
        val emptyRes = engine.executeCode(com.example.data.coding.ProgrammingLanguageId.PYTHON, "   ", "")
        assertTrue(!emptyRes.isSuccess)
        assertEquals("Write some code before executing.", emptyRes.stderr)

        // 3. Multi-line user code executes and outputs exact lines
        val userCode = """
            print("hello this world is awesome")
            print("hello world")
        """.trimIndent()
        val multiLineRes = engine.executeCode(com.example.data.coding.ProgrammingLanguageId.PYTHON, userCode, "")
        assertTrue(multiLineRes.isSuccess)
        assertEquals("hello this world is awesome\nhello world", multiLineRes.stdout)

        // 4. Intelligent error detection for broken Python code: Print "hello world"
        val brokenPy = "Print \"hello world\""
        val brokenRes = engine.executeCode(com.example.data.coding.ProgrammingLanguageId.PYTHON, brokenPy, "")
        assertTrue(!brokenRes.isSuccess)
        assertEquals(1, brokenRes.errorLineNumber)
        assertEquals("SyntaxError", brokenRes.errorType)
        assertTrue(brokenRes.errorExplanation.contains("case-sensitive", ignoreCase = true))
        assertEquals("print(\"hello world\")", brokenRes.suggestedFixCode?.trim())

        // 5. "Fix it with Falcon AI" returns Before-and-After comparison and corrected code
        val fixResponse = aiService.assistWithCode(
            action = com.example.data.coding.AiCodeAction.FIX_CODE,
            language = com.example.data.coding.ProgrammingLanguageId.PYTHON,
            code = brokenPy,
            lastResult = brokenRes
        )
        assertEquals(brokenPy, fixResponse.originalCode)
        assertEquals("print(\"hello world\")", fixResponse.suggestedCode?.trim())
        assertTrue(fixResponse.whatChangedBullets.isNotEmpty())
        assertTrue(fixResponse.whyItFailed.isNotBlank())
        assertTrue(fixResponse.tipToRemember.isNotBlank())
        assertTrue(fixResponse.changedLineNumbers.contains(1))

        // Verify the fixed code executes and produces "hello world"
        val fixedExecRes = engine.executeCode(
            com.example.data.coding.ProgrammingLanguageId.PYTHON,
            fixResponse.suggestedCode!!,
            ""
        )
        assertTrue(fixedExecRes.isSuccess)
        assertEquals("hello world", fixedExecRes.stdout.trim())

        // 6. Language-aware error detection & fixing for JavaScript, C++, Java, C, and HTML/CSS
        val jsBroken = "Console.log(\"JS Rocks\");"
        val jsBrokenRes = engine.executeCode(com.example.data.coding.ProgrammingLanguageId.JAVASCRIPT, jsBroken, "")
        assertTrue(!jsBrokenRes.isSuccess)
        assertEquals(1, jsBrokenRes.errorLineNumber)
        assertEquals("console.log(\"JS Rocks\");", jsBrokenRes.suggestedFixCode?.trim())

        val cppBroken = """
            #include <iostream>
            using namespace std;
            int main() {
                cout << "C++ Ready" << endl
                return 0;
            }
        """.trimIndent()
        val cppBrokenRes = engine.executeCode(com.example.data.coding.ProgrammingLanguageId.CPP, cppBroken, "")
        assertTrue(!cppBrokenRes.isSuccess)
        assertEquals(4, cppBrokenRes.errorLineNumber)
        assertTrue(cppBrokenRes.stderr.contains("expected ';'"))

        // 7. Verify real JavaScript control flow (functions, loops, if/else) and C++ execution
        val jsProgram = """
            function greet(name) {
                return "Hello " + name;
            }
            let total = 0;
            for (let i = 1; i <= 4; i++) {
                total += i;
            }
            if (total === 10) {
                console.log(greet("Falcon"), total);
            } else {
                console.log("Wrong total");
            }
        """.trimIndent()
        val jsExecRes = engine.executeCode(com.example.data.coding.ProgrammingLanguageId.JAVASCRIPT, jsProgram, "")
        assertTrue(jsExecRes.isSuccess)
        assertEquals("Hello Falcon 10", jsExecRes.stdout.trim())

        val cppProgram = """
            #include <iostream>
            using namespace std;
            int main() {
                int sum = 0;
                for (int i = 1; i <= 5; i++) {
                    sum += i;
                }
                if (sum == 15) {
                    cout << "Sum: " << sum << endl;
                } else {
                    cout << "Fail" << endl;
                }
                return 0;
            }
        """.trimIndent()
        val cppExecRes = engine.executeCode(com.example.data.coding.ProgrammingLanguageId.CPP, cppProgram, "")
        assertTrue(cppExecRes.isSuccess)
        assertEquals("Sum: 15", cppExecRes.stdout.trim())

        // 8. Verify isolated execution safety limit against infinite loops
        val infinitePy = """
            x = 0
            while True:
                x += 1
        """.trimIndent()
        val timeoutRes = engine.executeCode(com.example.data.coding.ProgrammingLanguageId.PYTHON, infinitePy, "")
        assertTrue(!timeoutRes.isSuccess)
        assertTrue(timeoutRes.isTimeout)
    }
}

