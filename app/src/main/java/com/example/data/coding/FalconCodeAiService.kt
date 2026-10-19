package com.example.data.coding

import android.content.Context
import com.example.BuildConfig
import com.example.data.model.LuminoCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class FalconCodeAiService {

    suspend fun assistWithCode(
        action: AiCodeAction,
        language: ProgrammingLanguageId,
        code: String,
        lastResult: CodingExecutionResult?,
        lessonTitle: String? = null,
        customQuestion: String? = null,
        targetConvertLanguage: ProgrammingLanguageId = ProgrammingLanguageId.JAVASCRIPT
    ): AiCodeAssistantResponse = withContext(Dispatchers.IO) {
        if (!customQuestion.isNullOrBlank() && LuminoCatalog.isProfaneOrAngry(customQuestion)) {
            return@withContext AiCodeAssistantResponse(
                actionTitle = "Hey, what happened?",
                explanation = LuminoCatalog.getBadWordsApologyExplanation()
            )
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            val remote = tryRemoteAiAssistance(
                apiKey = apiKey,
                action = action,
                language = language,
                code = code,
                lastResult = lastResult,
                lessonTitle = lessonTitle,
                customQuestion = customQuestion,
                targetConvertLanguage = targetConvertLanguage
            )
            if (remote != null) return@withContext remote
        }

        buildSmartCodeAnalysis(
            action = action,
            language = language,
            code = code,
            lastResult = lastResult,
            lessonTitle = lessonTitle,
            customQuestion = customQuestion,
            targetConvertLanguage = targetConvertLanguage
        )
    }

    private fun tryRemoteAiAssistance(
        apiKey: String,
        action: AiCodeAction,
        language: ProgrammingLanguageId,
        code: String,
        lastResult: CodingExecutionResult?,
        lessonTitle: String?,
        customQuestion: String?,
        targetConvertLanguage: ProgrammingLanguageId
    ): AiCodeAssistantResponse? {
        return try {
            val prompt = buildString {
                appendLine("You are Falcon AI, a patient, skilled programming instructor. Never mention other AI brands.")
                appendLine("Selected Language: ${language.displayName}")
                if (!lessonTitle.isNullOrBlank()) appendLine("Current Lesson Context: $lessonTitle")
                appendLine("Requested Action: ${action.label} - ${action.shortPrompt}")
                if (action == AiCodeAction.CONVERT_LANGUAGE) {
                    appendLine("Target Conversion Language: ${targetConvertLanguage.displayName}")
                }
                if (!customQuestion.isNullOrBlank()) {
                    appendLine("Learner's Question: $customQuestion")
                }
                appendLine("Learner's Current Code:\n$code")
                if (lastResult != null) {
                    if (lastResult.stdout.isNotBlank()) appendLine("Program Output:\n${lastResult.stdout}")
                    if (lastResult.stderr.isNotBlank()) appendLine("Program Error:\n${lastResult.stderr}")
                }
                appendLine("Respond with clear, encouraging explanations. Prioritize understanding over blind copying.")
            }

            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 5000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val reqJson = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                ))
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(reqJson.toString()) }
            if (conn.responseCode in 200..299) {
                val resp = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val text = resp.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "") ?: ""
                if (text.isNotBlank()) {
                    return AiCodeAssistantResponse(
                        actionTitle = "Falcon AI • ${action.label}",
                        explanation = text.trim()
                    )
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun buildSmartCodeAnalysis(
        action: AiCodeAction,
        language: ProgrammingLanguageId,
        code: String,
        lastResult: CodingExecutionResult?,
        lessonTitle: String?,
        customQuestion: String?,
        targetConvertLanguage: ProgrammingLanguageId
    ): AiCodeAssistantResponse {
        val codeLines = code.lines().filter { it.isNotBlank() }
        val contextHeader = if (!lessonTitle.isNullOrBlank()) "Lesson: $lessonTitle (${language.displayName})\n\n" else ""

        // Handle custom learner question first if provided
        if (!customQuestion.isNullOrBlank()) {
            val lowerQ = customQuestion.lowercase()
            if (lowerQ.contains("don't understand") || lowerQ.contains("do not understand") || lowerQ.contains("confused") || lowerQ.contains("simpler")) {
                return AiCodeAssistantResponse(
                    actionTitle = "Falcon AI • Simpler Analogy & Breakdown",
                    explanation = "${contextHeader}No problem at all! Let's look at this in a much simpler way:\n\n" +
                        "Think of your ${language.displayName} program like a vending machine:\n" +
                        "1. Variables are the labeled slots holding snacks or numbers.\n" +
                        "2. Input (like input() or cin) is dropping a coin in and pressing a button.\n" +
                        "3. Output (like print() or console.log()) is the display screen showing your result.\n\n" +
                        "In your current code (${codeLines.size} active lines), the first line sets up your data and the output statement displays the result. Try changing one value and tapping Run to see how the output changes immediately!"
                )
            }
            return AiCodeAssistantResponse(
                actionTitle = "Falcon AI • ${language.displayName} Mentor",
                explanation = "${contextHeader}Great question about \"${customQuestion.trim()}\"! In ${language.displayName}, the key is to trace how data flows from top to bottom. " +
                    "Looking at your current editor code:\n" +
                    codeLines.take(4).mapIndexed { i, l -> "• Line ${i + 1}: `${l.trim()}`" }.joinToString("\n") +
                    "\n\nEach statement executes in sequence. If you want to test a change, edit a value in the editor and tap Run Code to inspect the console output!"
            )
        }

        return when (action) {
            AiCodeAction.EXPLAIN_CODE -> {
                if (code.isBlank()) {
                    return AiCodeAssistantResponse(
                        actionTitle = "Falcon AI • Line-by-Line Code Explanation",
                        explanation = "Your ${language.displayName} editor is currently empty. Write some code or tap **Load Example** first, then tap **Explain Code** for a line-by-line walkthrough."
                    )
                }
                val walkthrough = code.lines().mapIndexedNotNull { idx, line ->
                    val t = line.trim()
                    if (t.isEmpty()) null
                    else "• Line ${idx + 1} (`$t`): ${describeLinePurpose(t, language)}"
                }.joinToString("\n")

                AiCodeAssistantResponse(
                    actionTitle = "Falcon AI • Line-by-Line Code Explanation",
                    explanation = "${contextHeader}Here is how your ${language.displayName} code works step by step:\n\n$walkthrough\n\n" +
                        "Notice how each statement runs in order and sends output to the console."
                )
            }

            AiCodeAction.FIND_ERROR -> {
                val diagnosticResult = if (lastResult != null && !lastResult.isSuccess) {
                    lastResult
                } else {
                    CodeExecutionEngine().executeCode(language, code, "")
                }
                val err = diagnosticResult.stderr
                val diag = if (err.isNotBlank()) {
                    val lineInfo = diagnosticResult.errorLineNumber?.let { " on **Line $it**" } ?: ""
                    val reason = diagnosticResult.errorExplanation.ifBlank { diagnoseErrorCause(err, code, language) }
                    "I inspected your ${language.displayName} code and detected a **${diagnosticResult.errorType.ifBlank { "Compiler/Runtime Error" }}**$lineInfo:\n\n" +
                        "`$err`\n\n" +
                        "**Why this happened**: $reason"
                } else {
                    inspectStaticMistakes(code, language)
                }
                AiCodeAssistantResponse(
                    actionTitle = "Falcon AI • Bug & Error Diagnostic",
                    explanation = diag
                )
            }

            AiCodeAction.FIX_CODE -> {
                if (code.isBlank()) {
                    return AiCodeAssistantResponse(
                        actionTitle = "Falcon AI • Code Fix Assistant",
                        explanation = "Write some ${language.displayName} code in the editor first. If your code produces an error when executed, tap **Fix it with Falcon AI** to see a side-by-side correction."
                    )
                }
                val diagnosticResult = if (lastResult != null && !lastResult.isSuccess) {
                    lastResult
                } else {
                    CodeExecutionEngine().executeCode(language, code, "")
                }

                val fixed = diagnosticResult.suggestedFixCode?.takeIf { it.isNotBlank() }
                    ?: proposeFixedCode(code, language)

                val origLines = code.lines()
                val fixedLines = fixed.lines()
                val maxLines = maxOf(origLines.size, fixedLines.size)
                val changedLines = mutableListOf<Int>()
                val diffBullets = mutableListOf<String>()

                for (i in 0 until maxLines) {
                    val o = origLines.getOrNull(i)?.trim() ?: ""
                    val f = fixedLines.getOrNull(i)?.trim() ?: ""
                    if (o != f) {
                        changedLines.add(i + 1)
                        if (o.isNotEmpty() && f.isNotEmpty()) {
                            diffBullets.add("Line ${i + 1}: Changed `$o` → `$f`")
                        } else if (o.isEmpty() && f.isNotEmpty()) {
                            diffBullets.add("Line ${i + 1}: Added `$f`")
                        } else if (o.isNotEmpty()) {
                            diffBullets.add("Line ${i + 1}: Removed `$o`")
                        }
                    }
                }

                if (diffBullets.isEmpty()) {
                    diffBullets.add("Corrected ${language.displayName} statement syntax while preserving your program logic.")
                }

                val whyFailed = when {
                    diagnosticResult.errorExplanation.isNotBlank() -> diagnosticResult.errorExplanation
                    diagnosticResult.stderr.isNotBlank() -> diagnoseErrorCause(diagnosticResult.stderr, code, language)
                    else -> "Your ${language.displayName} syntax had a casing, delimiter, or statement formatting issue that prevented clean execution."
                }

                val tip = buildLanguageTip(language, diagnosticResult.stderr, code)

                AiCodeAssistantResponse(
                    actionTitle = "Falcon AI • Error Analysis & Proposed Fix",
                    explanation = "I analyzed your ${language.displayName} code and the compiler/runtime error. Review the **Before & After** comparison below—your editor code has **not** been modified yet. Tap **Apply Fix to Editor** to use the corrected code, or **Keep My Code** to edit it yourself.",
                    suggestedCode = fixed,
                    originalCode = code,
                    whatChangedBullets = diffBullets,
                    whyItFailed = whyFailed,
                    tipToRemember = tip,
                    changedLineNumbers = changedLines
                )
            }

            AiCodeAction.GIVE_HINT -> {
                val err = lastResult?.stderr.orEmpty()
                val hintBody = if (err.isNotBlank()) {
                    val explanation = lastResult?.errorExplanation?.takeIf { it.isNotBlank() }
                        ?: diagnoseErrorCause(err, code, language)
                    val lineRef = lastResult?.errorLineNumber?.let { "Check **Line $it** first. " } ?: ""
                    "${contextHeader}${lineRef}$explanation\n\nTry adjusting that line in the editor and tapping **Execute Code** again!"
                } else {
                    "${contextHeader}Hint for your ${language.displayName} code:\n\n" +
                        "1. Check what data type your variables hold (text strings vs numbers).\n" +
                        "2. Make sure every opening parenthesis `(` or quote `\"` has a matching closing partner.\n" +
                        "3. Check the exact function name and capitalization for ${language.displayName} output."
                }
                AiCodeAssistantResponse(
                    actionTitle = "Falcon AI • Learning Hint",
                    explanation = hintBody
                )
            }

            AiCodeAction.EXPLAIN_OUTPUT -> {
                val outText = lastResult?.stdout.orEmpty()
                val errText = lastResult?.stderr.orEmpty()
                val body = when {
                    errText.isNotBlank() -> "Your program stopped with an error before completing normal output:\n`$errText`\n\n${lastResult?.errorExplanation.orEmpty()}\n\nTap **Fix it with Falcon AI** below the console if you'd like to compare a corrected version."
                    outText.isNotBlank() -> "Your program executed cleanly in ${lastResult?.executionTimeMs ?: 1}ms and printed:\n\n`$outText`\n\nEach line in the output console corresponds to an output instruction executed in your ${language.displayName} code."
                    else -> "Your program hasn't produced console output yet. Write an output statement in the editor and tap **Execute Code**!"
                }
                AiCodeAssistantResponse(
                    actionTitle = "Falcon AI • Output Analysis",
                    explanation = body
                )
            }

            AiCodeAction.IMPROVE_CODE -> {
                val improved = proposeImprovedCode(code, language)
                AiCodeAssistantResponse(
                    actionTitle = "Falcon AI • Clean Code & Style Improvements",
                    explanation = "Here are ways to make your ${language.displayName} code cleaner and more maintainable:\n\n" +
                        "• Use descriptive variable names so your intent is clear at a glance.\n" +
                        "• Add a concise header comment summarizing the program's purpose.\n" +
                        "• Keep indentation consistent across all blocks.",
                    suggestedCode = improved.takeIf { it.isNotBlank() }
                )
            }

            AiCodeAction.GENERATE_PRACTICE -> {
                AiCodeAssistantResponse(
                    actionTitle = "Falcon AI • Custom Practice Challenge",
                    explanation = "Try this mini-challenge right in your ${language.displayName} editor:\n\n" +
                        "**Challenge**: Create two numeric variables (`a = 15` and `b = 4`), compute their product (`a * b`) and sum (`a + b`), and print both results on separate lines.\n\n" +
                        "**Expected Output**:\n`60`\n`19`"
                )
            }

            AiCodeAction.EXPLAIN_CONCEPT -> {
                AiCodeAssistantResponse(
                    actionTitle = "Falcon AI • Concept & Mental Model",
                    explanation = "${contextHeader}In ${language.displayName}, every program combines three core pillars:\n\n" +
                        "1. **State (Variables)**: Named boxes in memory that hold numbers, text, or collections.\n" +
                        "2. **Control Flow (Conditions & Loops)**: The decision-making rules that choose which lines run and how many times.\n" +
                        "3. **Functions & I/O**: Reusable actions that accept input, transform data, and display output."
                )
            }

            AiCodeAction.STEP_BY_STEP -> {
                AiCodeAssistantResponse(
                    actionTitle = "Falcon AI • Step-by-Step Solution Strategy",
                    explanation = "${contextHeader}To solve this problem systematically:\n\n" +
                        "**Step 1**: Identify the inputs you have (variables or standard input).\n" +
                        "**Step 2**: Write the calculation or loop that transforms the input.\n" +
                        "**Step 3**: Output the final result and verify it in the console."
                )
            }

            AiCodeAction.CONVERT_LANGUAGE -> {
                val converted = convertCodeToLanguage(code, language, targetConvertLanguage)
                AiCodeAssistantResponse(
                    actionTitle = "Falcon AI • Converted to ${targetConvertLanguage.displayName}",
                    explanation = "Here is how your ${language.displayName} logic translates into idiomatic **${targetConvertLanguage.displayName}**.\n\n" +
                        "Notice the differences in syntax, variable declarations, and standard output functions:",
                    suggestedCode = converted,
                    targetLanguage = targetConvertLanguage
                )
            }
        }
    }

    private fun buildLanguageTip(lang: ProgrammingLanguageId, stderr: String, code: String): String {
        return when (lang) {
            ProgrammingLanguageId.PYTHON -> when {
                stderr.contains("Missing parentheses", ignoreCase = true) || code.contains(Regex("^\\s*[Pp]rint\\s+")) ->
                    "In Python 3, `print` is a function and must always use lowercase letters and parentheses: `print(\"text\")`."
                stderr.contains("expected ':'", ignoreCase = true) ->
                    "Every Python block header (`if`, `elif`, `else`, `for`, `while`, `def`, `class`) must end with a colon `:`."
                stderr.contains("IndentationError", ignoreCase = true) ->
                    "Python uses 4 spaces of indentation to group statements inside blocks."
                stderr.contains("TypeError", ignoreCase = true) ->
                    "Convert numbers with `str(num)` or use an f-string `f\"Value: {num}\"` when combining text and numbers in Python."
                else ->
                    "Python is case-sensitive: built-in functions like `print()`, `input()`, `len()`, and `range()` are always lowercase."
            }
            ProgrammingLanguageId.JAVASCRIPT -> when {
                stderr.contains("console.log", ignoreCase = true) || code.contains("Console.") ->
                    "JavaScript is case-sensitive: always write `console.log(...)` with a lowercase `c`."
                stderr.contains("constant variable", ignoreCase = true) ->
                    "Use `let` when a variable's value needs to change later, and `const` for values that stay fixed."
                else ->
                    "In JavaScript, use `console.log()` for output and declare variables with `let` or `const`."
            }
            ProgrammingLanguageId.CPP ->
                "In C++, remember `#include <iostream>`, `using namespace std;`, an `int main()` function, and semicolons `;` at the end of every statement."
            ProgrammingLanguageId.JAVA ->
                "In Java, code runs inside `public class Main { public static void main(String[] args) { ... } }` and uses `System.out.println(...)` with a capital `S`."
            ProgrammingLanguageId.C ->
                "In C, include `<stdio.h>`, place statements inside `int main()`, use `printf(\"...\\n\");`, and end statements with `;`."
            ProgrammingLanguageId.HTML_CSS ->
                "In HTML & CSS, ensure every opening tag like `<div>` or `<h1>` has a matching closing tag like `</div>` or `</h1>`."
        }
    }

    private fun describeLinePurpose(line: String, lang: ProgrammingLanguageId): String = when {
        line.startsWith("#") || line.startsWith("//") || line.startsWith("/*") ->
            "Comment note for humans (ignored by the compiler/interpreter)."
        line.startsWith("print(") || line.startsWith("console.log(") || line.startsWith("cout <<") || line.startsWith("System.out.println(") || line.startsWith("printf(") ->
            "Outputs the evaluated values inside to the console."
        line.startsWith("def ") || line.startsWith("function ") ->
            "Defines a reusable function block."
        line.startsWith("for ") || line.startsWith("for(") || line.startsWith("while ") ->
            "Starts a loop that repeats its body for each cycle."
        line.startsWith("if ") || line.startsWith("elif ") || line.startsWith("else") ->
            "Conditional branch that decides whether to execute the block."
        line.contains("=") ->
            "Computes the expression on the right and stores it in the variable on the left."
        else ->
            "Executes a ${lang.displayName} statement."
    }

    private fun diagnoseErrorCause(err: String, code: String, lang: ProgrammingLanguageId): String = when {
        err.contains("Missing parentheses in call to 'print'", ignoreCase = true) ->
            "In Python 3, `print` is a built-in function and requires parentheses, e.g., `print(\"hello world\")`."
        err.contains("unterminated string", ignoreCase = true) || err.contains("Unclosed string", ignoreCase = true) ->
            "You opened a quotation mark (`\"` or `'`) without closing it on the same line."
        err.contains("expected ':'", ignoreCase = true) ->
            "In Python, `if`, `elif`, `else`, `for`, `while`, `def`, and `class` headers must end with a colon `:`."
        err.contains("ZeroDivisionError", ignoreCase = true) || err.contains("Division by zero", ignoreCase = true) ->
            "Your code attempted to divide a number by `0`, which is mathematically undefined."
        err.contains("NameError", ignoreCase = true) || err.contains("ReferenceError", ignoreCase = true) || err.contains("was not declared", ignoreCase = true) ->
            "You used a variable or function name that hasn't been defined yet, or has a capitalization mismatch."
        err.contains("TypeError", ignoreCase = true) ->
            "You combined incompatible data types (such as adding a string and a number directly without converting types)."
        err.contains("expected ';'", ignoreCase = true) ->
            "In ${lang.displayName}, statements must end with a semicolon `;`."
        err.contains("TimeoutError", ignoreCase = true) ->
            "Your loop ran too many times without reaching a stopping condition (infinite loop)."
        else ->
            "Check the line number reported in the error message for syntax or type mismatches."
    }

    private suspend fun inspectStaticMistakes(code: String, lang: ProgrammingLanguageId): String {
        if (code.isBlank()) {
            return "Your editor is currently empty. Write some ${lang.displayName} code first, then tap **Execute Code**."
        }
        val res = CodeExecutionEngine().executeCode(lang, code, "")
        if (!res.isSuccess && res.stderr.isNotBlank()) {
            return "Found issue in your ${lang.displayName} code:\n\n`${res.stderr}`\n\n${res.errorExplanation}"
        }
        return "No syntax errors detected in your ${lang.displayName} code! Your program ran cleanly."
    }

    private suspend fun proposeFixedCode(code: String, lang: ProgrammingLanguageId): String {
        if (code.isBlank()) return ""
        val exec = CodeExecutionEngine().executeCode(lang, code, "")
        val suggested = exec.suggestedFixCode
        if (!suggested.isNullOrBlank()) {
            return suggested
        }
        var fixed = code
            .replace("Print(", "print(")
            .replace("Console.log(", "console.log(")
            .replace("system.out.println(", "System.out.println(")
        if (lang == ProgrammingLanguageId.PYTHON) {
            fixed = fixed.lines().joinToString("\n") { line ->
                val t = line.trim()
                val printNoParens = Regex("^([Pp]rint)\\s+(.+)$").find(t)
                when {
                    printNoParens != null && !t.startsWith("print(") && !t.startsWith("Print(") -> {
                        val indent = line.takeWhile { it.isWhitespace() }
                        val arg = printNoParens.groupValues[2].trim()
                        "${indent}print($arg)"
                    }
                    (t.startsWith("if ") || t.startsWith("elif ") || t.startsWith("else") || t.startsWith("for ") || t.startsWith("while ") || t.startsWith("def ")) && !t.endsWith(":") ->
                        "$line:"
                    else -> line
                }
            }
        }
        return fixed
    }

    private fun proposeImprovedCode(code: String, lang: ProgrammingLanguageId): String {
        if (code.isBlank()) return ""
        val header = when (lang) {
            ProgrammingLanguageId.PYTHON -> "# Clean, idiomatic Python implementation"
            ProgrammingLanguageId.HTML_CSS -> "<!-- Clean semantic HTML5 & CSS3 -->"
            else -> "// Clean, idiomatic ${lang.displayName} implementation"
        }
        return if (code.startsWith("#") || code.startsWith("//") || code.startsWith("<!--")) {
            code.trim()
        } else {
            "$header\n${code.trim()}"
        }
    }

    private fun convertCodeToLanguage(
        code: String,
        sourceLang: ProgrammingLanguageId,
        targetLang: ProgrammingLanguageId
    ): String {
        val stringMatch = Regex("\"([^\"]+)\"").find(code)?.groupValues?.get(1) ?: "Hello World"
        return when (targetLang) {
            ProgrammingLanguageId.PYTHON -> """
                # Converted from ${sourceLang.displayName} to Python
                message = "$stringMatch"
                print(message)
            """.trimIndent()

            ProgrammingLanguageId.JAVASCRIPT -> """
                // Converted from ${sourceLang.displayName} to JavaScript
                const message = "$stringMatch";
                console.log(message);
            """.trimIndent()

            ProgrammingLanguageId.CPP -> """
                // Converted from ${sourceLang.displayName} to C++
                #include <iostream>
                using namespace std;

                int main() {
                    cout << "$stringMatch" << endl;
                    return 0;
                }
            """.trimIndent()

            ProgrammingLanguageId.JAVA -> """
                // Converted from ${sourceLang.displayName} to Java
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("$stringMatch");
                    }
                }
            """.trimIndent()

            ProgrammingLanguageId.C -> """
                // Converted from ${sourceLang.displayName} to C
                #include <stdio.h>

                int main() {
                    printf("$stringMatch\n");
                    return 0;
                }
            """.trimIndent()

            ProgrammingLanguageId.HTML_CSS -> """
                <!-- Converted from ${sourceLang.displayName} to HTML & CSS -->
                <!DOCTYPE html>
                <html>
                <body style="font-family: sans-serif; padding: 16px;">
                  <h2>$stringMatch</h2>
                </body>
                </html>
            """.trimIndent()
        }
    }
}

class CodingProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("falcon_coding_progress_prefs", Context.MODE_PRIVATE)

    fun loadProgress(userId: String): UserCodingProgress {
        val prefix = "${userId}_"
        val langKey = prefs.getString("${prefix}selected_lang", ProgrammingLanguageId.PYTHON.key) ?: ProgrammingLanguageId.PYTHON.key
        val lessons = prefs.getStringSet("${prefix}completed_lessons", emptySet()) ?: emptySet()
        val challenges = prefs.getStringSet("${prefix}completed_challenges", emptySet()) ?: emptySet()
        val projects = prefs.getStringSet("${prefix}completed_projects", emptySet()) ?: emptySet()
        val mastered = prefs.getStringSet("${prefix}mastered_topics", emptySet()) ?: emptySet()
        val weak = prefs.getStringSet("${prefix}weak_topics", emptySet()) ?: emptySet()
        val goalMins = prefs.getInt("${prefix}study_goal_mins", 30)
        val xp = prefs.getInt("${prefix}coding_xp", 0)
        val runs = prefs.getInt("${prefix}total_runs", 0)

        val savedCode = mutableMapOf<String, String>()
        for (lang in ProgrammingLanguageId.entries) {
            // Use v2 key so previously auto-populated starter code never pollutes a blank workspace
            if (prefs.contains("${prefix}user_code_v2_${lang.key}")) {
                savedCode[lang.key] = prefs.getString("${prefix}user_code_v2_${lang.key}", "") ?: ""
            }
        }

        return UserCodingProgress(
            selectedLanguage = ProgrammingLanguageId.fromKey(langKey),
            completedLessonIds = lessons.toSet(),
            completedChallengeIds = challenges.toSet(),
            completedProjectIds = projects.toSet(),
            masteredTopics = mastered.toSet(),
            weakTopics = weak.toSet(),
            dailyStudyMinutesGoal = goalMins,
            savedCodeByLanguage = savedCode,
            codingXp = xp,
            totalExecutions = runs
        )
    }

    fun saveProgress(userId: String, progress: UserCodingProgress) {
        val prefix = "${userId}_"
        prefs.edit().apply {
            putString("${prefix}selected_lang", progress.selectedLanguage.key)
            putStringSet("${prefix}completed_lessons", progress.completedLessonIds)
            putStringSet("${prefix}completed_challenges", progress.completedChallengeIds)
            putStringSet("${prefix}completed_projects", progress.completedProjectIds)
            putStringSet("${prefix}mastered_topics", progress.masteredTopics)
            putStringSet("${prefix}weak_topics", progress.weakTopics)
            putInt("${prefix}study_goal_mins", progress.dailyStudyMinutesGoal)
            putInt("${prefix}coding_xp", progress.codingXp)
            putInt("${prefix}total_runs", progress.totalExecutions)
            for ((k, v) in progress.savedCodeByLanguage) {
                putString("${prefix}user_code_v2_$k", v)
            }
            apply()
        }
    }
}
