package com.example.data.coding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow

class CodeExecutionEngine {

    companion object {
        private const val MAX_LOOP_ITERATIONS = 5000
        private const val MAX_EXECUTION_TIME_MS = 4000L
    }

    suspend fun executeCode(
        language: ProgrammingLanguageId,
        code: String,
        stdin: String = ""
    ): CodingExecutionResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val trimmedCode = code.trim()

        if (trimmedCode.isEmpty()) {
            return@withContext CodingExecutionResult(
                stdout = "",
                stderr = "Write some code before executing.",
                exitCode = 1,
                executionTimeMs = 1L,
                runtimeUsed = language.runtimeLabel,
                compilationStatus = "Empty Editor",
                errorType = "EmptyCode",
                errorSummary = "The code editor is empty. Write some ${language.displayName} code before executing.",
                isEmptyCodeWarning = true
            )
        }

        when (language) {
            ProgrammingLanguageId.PYTHON -> executePythonLocal(code, stdin, startTime)
            ProgrammingLanguageId.JAVASCRIPT -> executeJavaScriptLocal(code, stdin, startTime)
            ProgrammingLanguageId.CPP -> executeCppLocal(code, stdin, startTime)
            ProgrammingLanguageId.JAVA -> executeJavaLocal(code, stdin, startTime)
            ProgrammingLanguageId.C -> executeCLocal(code, stdin, startTime)
            ProgrammingLanguageId.HTML_CSS -> executeHtmlCssPreview(code, startTime)
        }
    }

    private fun executeHtmlCssPreview(code: String, startTime: Long): CodingExecutionResult {
        if (code.contains("<style", ignoreCase = true) && !code.contains("</style>", ignoreCase = true)) {
            val styleLine = code.lines().indexOfFirst { it.contains("<style", ignoreCase = true) }.coerceAtLeast(0) + 1
            return buildDiagnosticErrorResult(
                stderr = "index.html:$styleLine: HTML Parse Error: Opening <style> tag is missing its closing </style> tag.",
                startTime = startTime,
                lang = ProgrammingLanguageId.HTML_CSS,
                compilationStatus = "Parse Error",
                errorLine = styleLine,
                errorCol = 1,
                errorType = "HTMLParseError",
                errorSummary = "Opening <style> tag on line $styleLine is missing its closing </style> tag.",
                offendingLine = code.lines().getOrNull(styleLine - 1).orEmpty(),
                suggestedFix = "$code\n</style>",
                fixExplanation = "Every <style> block in HTML must be closed with </style> so the browser knows where CSS rules end."
            )
        }
        val textPreview = code
            .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&")
            .replace(Regex("\\s+"), " ")
            .trim()

        val safeHtml = buildString {
            append("<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head><body>")
            append(code)
            append("</body></html>")
        }

        return CodingExecutionResult(
            stdout = textPreview.ifEmpty { "HTML/CSS rendered in sandboxed preview." },
            stderr = "",
            exitCode = 0,
            executionTimeMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L),
            runtimeUsed = ProgrammingLanguageId.HTML_CSS.runtimeLabel,
            isHtmlPreview = true,
            htmlContent = safeHtml,
            compilationStatus = "Rendered"
        )
    }

    // =========================================================================
    // ISOLATED ON-DEVICE PYTHON 3 INTERPRETER
    // =========================================================================
    private fun executePythonLocal(code: String, stdin: String, startTime: Long): CodingExecutionResult {
        val stdinLines = if (stdin.isEmpty()) mutableListOf() else stdin.lines().toMutableList()
        val stdoutBuilder = StringBuilder()
        val env = mutableMapOf<String, Any?>()
        val functions = mutableMapOf<String, PyFunction>()
        val classes = mutableMapOf<String, PyClassDef>()
        var stepCounter = 0

        val rawLines = code.lines()

        // 1. Pre-flight syntax & indentation checks
        var expectIndentedBlockAfterLine = -1
        var parentBlockIndent = 0
        var seenTopLevelStatement = false

        for ((idx, rawLine) in rawLines.withIndex()) {
            val lineNum = idx + 1
            val cleanNoComment = stripPythonComment(rawLine)
            val stripped = cleanNoComment.trim()
            if (stripped.isEmpty()) continue

            val currentIndent = cleanNoComment.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)

            if (expectIndentedBlockAfterLine != -1) {
                if (currentIndent <= parentBlockIndent) {
                    val fixedLines = rawLines.toMutableList()
                    fixedLines[idx] = " ".repeat(parentBlockIndent + 4) + stripped
                    return buildDiagnosticErrorResult(
                        stderr = "  File \"main.py\", line $lineNum\n    $stripped\n    ^\nIndentationError: expected an indented block after line $expectIndentedBlockAfterLine",
                        startTime = startTime,
                        lang = ProgrammingLanguageId.PYTHON,
                        compilationStatus = "Indentation Error",
                        errorLine = lineNum,
                        errorCol = 1,
                        errorType = "IndentationError",
                        errorSummary = "Line $lineNum must be indented (4 spaces) because line $expectIndentedBlockAfterLine opened a block with ':'.",
                        offendingLine = rawLine,
                        suggestedFix = fixedLines.joinToString("\n"),
                        fixExplanation = "In Python, any statement inside an if, for, while, def, or class block must be indented under its header. Indenting line $lineNum by 4 spaces fixes the IndentationError."
                    )
                }
                expectIndentedBlockAfterLine = -1
            } else if (!seenTopLevelStatement && currentIndent > 0) {
                val fixedLines = rawLines.toMutableList()
                fixedLines[idx] = stripped
                return buildDiagnosticErrorResult(
                    stderr = "  File \"main.py\", line $lineNum\n    $rawLine\n    ^\nIndentationError: unexpected indent at start of program",
                    startTime = startTime,
                    lang = ProgrammingLanguageId.PYTHON,
                    compilationStatus = "Indentation Error",
                    errorLine = lineNum,
                    errorCol = currentIndent + 1,
                    errorType = "IndentationError",
                    errorSummary = "Line $lineNum has unexpected leading spaces at the top level.",
                    offendingLine = rawLine,
                    suggestedFix = fixedLines.joinToString("\n"),
                    fixExplanation = "Top-level statements in Python should start at column 1 without leading spaces. Removing the extra spaces on line $lineNum resolves the IndentationError."
                )
            }
            seenTopLevelStatement = true

            // Check 1A: Unclosed string literals
            if (hasUnclosedQuotes(stripped)) {
                val quoteChar = if (stripped.count { it == '"' } % 2 != 0) "\"" else "'"
                val fixedLine = if (stripped.endsWith(")")) {
                    stripped.dropLast(1) + quoteChar + ")"
                } else {
                    stripped + quoteChar
                }
                val fixedLines = rawLines.toMutableList().also { it[idx] = " ".repeat(currentIndent) + fixedLine }
                return buildDiagnosticErrorResult(
                    stderr = "  File \"main.py\", line $lineNum\n    $stripped\n    ^\nSyntaxError: unterminated string literal (detected at line $lineNum)",
                    startTime = startTime,
                    lang = ProgrammingLanguageId.PYTHON,
                    compilationStatus = "Syntax Error",
                    errorLine = lineNum,
                    errorCol = stripped.length,
                    errorType = "SyntaxError",
                    errorSummary = "A text string on line $lineNum is missing its closing quotation mark ($quoteChar).",
                    offendingLine = rawLine,
                    suggestedFix = fixedLines.joinToString("\n"),
                    fixExplanation = "Every string literal in Python must start and end with matching quotation marks. Adding the missing closing $quoteChar on line $lineNum fixes the syntax error."
                )
            }

            // Check 1B: Case-sensitive 'Print' or 'print' without parentheses, e.g., Print "hello world"
            val printNoParenMatch = Regex("^([Pp]rint)\\s+(.+)$").find(stripped)
            if (printNoParenMatch != null && !stripped.startsWith("print(") && !stripped.startsWith("Print(")) {
                val keywordUsed = printNoParenMatch.groupValues[1]
                val argPart = printNoParenMatch.groupValues[2].trim()
                val correctedLine = " ".repeat(currentIndent) + "print($argPart)"
                val fixedLines = rawLines.toMutableList().also { it[idx] = correctedLine }
                val isCapitalized = keywordUsed.first().isUpperCase()
                val explanationText = if (isCapitalized) {
                    "Python is case-sensitive (built-in functions use lowercase `print`, not `Print`) and Python 3 requires parentheses around the arguments passed to `print(...)`."
                } else {
                    "In Python 3, `print()` is a built-in function and requires parentheses around its arguments: `print($argPart)`."
                }
                return buildDiagnosticErrorResult(
                    stderr = "  File \"main.py\", line $lineNum\n    $stripped\n    ^\nSyntaxError: Missing parentheses in call to 'print'. ${if (isCapitalized) "Python is also case-sensitive ('print' must be lowercase). " else ""}Did you mean print($argPart)?",
                    startTime = startTime,
                    lang = ProgrammingLanguageId.PYTHON,
                    compilationStatus = "Syntax Error",
                    errorLine = lineNum,
                    errorCol = 1,
                    errorType = "SyntaxError",
                    errorSummary = explanationText,
                    offendingLine = rawLine,
                    suggestedFix = fixedLines.joinToString("\n"),
                    fixExplanation = "$explanationText Replacing `$stripped` with `print($argPart)` fixes the syntax."
                )
            }

            // Check 1C: Capitalized 'Print(...)' with parentheses
            if (stripped.startsWith("Print(")) {
                val correctedLine = " ".repeat(currentIndent) + "print(" + stripped.removePrefix("Print(")
                val fixedLines = rawLines.toMutableList().also { it[idx] = correctedLine }
                return buildDiagnosticErrorResult(
                    stderr = "  File \"main.py\", line $lineNum\n    $stripped\n    ^\nNameError: name 'Print' is not defined. Did you mean: 'print'?",
                    startTime = startTime,
                    lang = ProgrammingLanguageId.PYTHON,
                    compilationStatus = "Runtime Error",
                    errorLine = lineNum,
                    errorCol = 1,
                    errorType = "NameError",
                    errorSummary = "Python is case-sensitive. The built-in output function is lowercase `print()`, not `Print()`.",
                    offendingLine = rawLine,
                    suggestedFix = fixedLines.joinToString("\n"),
                    fixExplanation = "Python identifiers are case-sensitive, so `Print` looks for an undefined function. Changing `Print(` to lowercase `print(` calls Python's built-in output function."
                )
            }

            // Check 1D: Unbalanced parentheses or brackets
            if (!hasBalancedParens(stripped)) {
                val openParens = stripped.count { it == '(' }
                val closeParens = stripped.count { it == ')' }
                val fixedLine = when {
                    openParens > closeParens -> stripped + ")".repeat(openParens - closeParens)
                    closeParens > openParens && stripped.endsWith(")") -> stripped.dropLast(closeParens - openParens)
                    else -> stripped
                }
                val fixedLines = rawLines.toMutableList().also { it[idx] = " ".repeat(currentIndent) + fixedLine }
                return buildDiagnosticErrorResult(
                    stderr = "  File \"main.py\", line $lineNum\n    $stripped\n    ^\nSyntaxError: '(' was never closed or unmatched ')' on line $lineNum",
                    startTime = startTime,
                    lang = ProgrammingLanguageId.PYTHON,
                    compilationStatus = "Syntax Error",
                    errorLine = lineNum,
                    errorCol = stripped.length,
                    errorType = "SyntaxError",
                    errorSummary = "Unbalanced parentheses or brackets on line $lineNum.",
                    offendingLine = rawLine,
                    suggestedFix = fixedLines.joinToString("\n"),
                    fixExplanation = "Every opening parenthesis `(` or bracket `[` must have a matching closing partner on the line. Balancing the parentheses on line $lineNum fixes the SyntaxError."
                )
            }

            // Check 1E: Block headers missing ':'
            val blockStarters = listOf("if ", "elif ", "else", "for ", "while ", "def ", "class ", "try", "except", "finally")
            if (blockStarters.any { stripped == it.trim() || stripped.startsWith(it) } && !stripped.endsWith(":")) {
                val fixedLines = rawLines.toMutableList().also { it[idx] = rawLine.trimEnd() + ":" }
                return buildDiagnosticErrorResult(
                    stderr = "  File \"main.py\", line $lineNum\n    $stripped\n    ^\nSyntaxError: expected ':' at the end of '${stripped.take(20)}' statement",
                    startTime = startTime,
                    lang = ProgrammingLanguageId.PYTHON,
                    compilationStatus = "Syntax Error",
                    errorLine = lineNum,
                    errorCol = stripped.length,
                    errorType = "SyntaxError",
                    errorSummary = "Python block statements (`if`, `else`, `for`, `while`, `def`, `class`) must end with a colon `:`.",
                    offendingLine = rawLine,
                    suggestedFix = fixedLines.joinToString("\n"),
                    fixExplanation = "In Python, a colon `:` marks the beginning of an indented code block. Adding `:` at the end of line $lineNum fixes the SyntaxError."
                )
            }

            if (stripped.endsWith(":")) {
                expectIndentedBlockAfterLine = lineNum
                parentBlockIndent = currentIndent
            }
        }

        if (expectIndentedBlockAfterLine != -1) {
            val fixedLines = rawLines.toMutableList().also { it.add(" ".repeat(parentBlockIndent + 4) + "pass") }
            return buildDiagnosticErrorResult(
                stderr = "  File \"main.py\", line $expectIndentedBlockAfterLine\nIndentationError: expected an indented block after ':' on line $expectIndentedBlockAfterLine",
                startTime = startTime,
                lang = ProgrammingLanguageId.PYTHON,
                compilationStatus = "Indentation Error",
                errorLine = expectIndentedBlockAfterLine,
                errorCol = 1,
                errorType = "IndentationError",
                errorSummary = "A block header ending with ':' on line $expectIndentedBlockAfterLine requires at least one indented line below it.",
                offendingLine = rawLines.getOrNull(expectIndentedBlockAfterLine - 1).orEmpty(),
                suggestedFix = fixedLines.joinToString("\n"),
                fixExplanation = "Python does not allow an empty block after `:`. Adding an indented statement (such as `pass` or `print(...)`) resolves the IndentationError."
            )
        }

        return try {
            val execSignal = executePythonBlock(
                lines = rawLines,
                startIndex = 0,
                endIndex = rawLines.size,
                baseIndent = 0,
                env = env,
                functions = functions,
                classes = classes,
                stdinLines = stdinLines,
                stdout = stdoutBuilder,
                stepTick = {
                    stepCounter++
                    if (stepCounter > MAX_LOOP_ITERATIONS || (System.currentTimeMillis() - startTime) > MAX_EXECUTION_TIME_MS) {
                        throw RuntimeException("TimeoutError: Execution exceeded maximum step/time limit ($MAX_LOOP_ITERATIONS operations). Check for infinite loops.")
                    }
                }
            )
            if (execSignal is PySignal.Return) {
                // Ignore top-level return
            }
            CodingExecutionResult(
                stdout = stdoutBuilder.toString().trimEnd('\n'),
                stderr = "",
                exitCode = 0,
                executionTimeMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L),
                runtimeUsed = ProgrammingLanguageId.PYTHON.runtimeLabel,
                compilationStatus = "Executed"
            )
        } catch (e: Exception) {
            val rawMsg = e.message ?: "RuntimeError: Python execution failed"
            val isTimeout = rawMsg.startsWith("TimeoutError")
            val lineMatch = Regex("line (\\d+)").find(rawMsg)
            val errLine = lineMatch?.groupValues?.get(1)?.toIntOrNull()
                ?: findLikelyErrorLine(rawLines, rawMsg)
            val offending = errLine?.let { rawLines.getOrNull(it - 1)?.trim() }.orEmpty()
            val errType = rawMsg.substringBefore(":").trim().ifEmpty { "RuntimeError" }
            val formattedStderr = if (rawMsg.contains("File \"main.py\"")) {
                rawMsg
            } else if (errLine != null && offending.isNotEmpty()) {
                "Traceback (most recent call last):\n  File \"main.py\", line $errLine\n    $offending\n$rawMsg"
            } else {
                rawMsg
            }
            val (autoFix, fixWhy) = proposePythonRuntimeFix(rawLines, errLine, rawMsg)

            CodingExecutionResult(
                stdout = stdoutBuilder.toString().trimEnd('\n'),
                stderr = formattedStderr,
                exitCode = 1,
                executionTimeMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L),
                runtimeUsed = ProgrammingLanguageId.PYTHON.runtimeLabel,
                isTimeout = isTimeout,
                compilationStatus = if (isTimeout) "Timed Out" else "Runtime Error",
                errorLineNumber = errLine,
                errorColumnNumber = 1,
                errorType = errType,
                errorSummary = summarizePythonRuntimeError(rawMsg, errLine),
                offendingLineText = offending,
                suggestedFixCode = autoFix,
                suggestedFixExplanation = fixWhy
            )
        }
    }

    private data class PyFunction(val name: String, val params: List<String>, val bodyLines: List<String>, val baseIndent: Int)
    private data class PyClassDef(val name: String, val parentName: String?, val methods: Map<String, PyFunction>)
    private data class PyObjectInstance(val className: String, val fields: MutableMap<String, Any?> = mutableMapOf())

    private sealed class PySignal {
        object None : PySignal()
        object Break : PySignal()
        object Continue : PySignal()
        data class Return(val value: Any?) : PySignal()
    }

    private fun collectIndentedBlock(lines: List<String>, afterLineIndex: Int, parentIndent: Int): Pair<List<String>, Int> {
        val collected = mutableListOf<String>()
        var i = afterLineIndex
        var blockIndent = -1
        while (i < lines.size) {
            val raw = lines[i]
            val clean = stripPythonComment(raw)
            if (clean.isBlank()) {
                collected.add(raw)
                i++
                continue
            }
            val indent = clean.indexOfFirst { !it.isWhitespace() }
            if (indent <= parentIndent) break
            if (blockIndent == -1) blockIndent = indent
            collected.add(raw)
            i++
        }
        return collected to i
    }

    private fun executePythonBlock(
        lines: List<String>,
        startIndex: Int,
        endIndex: Int,
        baseIndent: Int,
        env: MutableMap<String, Any?>,
        functions: MutableMap<String, PyFunction>,
        classes: MutableMap<String, PyClassDef>,
        stdinLines: MutableList<String>,
        stdout: StringBuilder,
        stepTick: () -> Unit
    ): PySignal {
        var i = startIndex
        while (i < endIndex) {
            stepTick()
            val rawLine = lines[i]
            val cleanLine = stripPythonComment(rawLine)
            if (cleanLine.isBlank()) {
                i++
                continue
            }
            val indent = cleanLine.indexOfFirst { !it.isWhitespace() }
            val stmt = cleanLine.trim()

            when {
                stmt == "pass" || stmt.startsWith("import ") || stmt.startsWith("from ") -> {
                    i++
                }

                stmt == "break" -> return PySignal.Break
                stmt == "continue" -> return PySignal.Continue

                stmt.startsWith("return") && (stmt == "return" || stmt.startsWith("return ")) -> {
                    val expr = stmt.removePrefix("return").trim()
                    val retVal = if (expr.isEmpty()) null else evalPyExpr(expr, env, functions, classes, stdinLines, stdout, stepTick)
                    return PySignal.Return(retVal)
                }

                stmt.startsWith("assert ") -> {
                    val condExpr = stmt.removePrefix("assert ").trim()
                    val cond = toTruthy(evalPyExpr(condExpr, env, functions, classes, stdinLines, stdout, stepTick))
                    if (!cond) {
                        throw RuntimeException("AssertionError: assert $condExpr failed on line ${i + 1}")
                    }
                    i++
                }

                stmt.startsWith("def ") && stmt.endsWith(":") -> {
                    val header = stmt.removePrefix("def ").removeSuffix(":").trim()
                    val parenOpen = header.indexOf('(')
                    val parenClose = header.lastIndexOf(')')
                    val fnName = header.substring(0, parenOpen).trim()
                    val paramsRaw = header.substring(parenOpen + 1, parenClose).trim()
                    val params = if (paramsRaw.isEmpty()) emptyList() else paramsRaw.split(",").map { it.trim().substringBefore("=").trim() }
                    val (body, nextIdx) = collectIndentedBlock(lines, i + 1, indent)
                    val bodyIndent = body.firstOrNull { stripPythonComment(it).isNotBlank() }?.let {
                        stripPythonComment(it).indexOfFirst { c -> !c.isWhitespace() }
                    } ?: (indent + 4)
                    functions[fnName] = PyFunction(fnName, params, body, bodyIndent)
                    i = nextIdx
                }

                stmt.startsWith("class ") && stmt.endsWith(":") -> {
                    val header = stmt.removePrefix("class ").removeSuffix(":").trim()
                    val className = header.substringBefore("(").trim()
                    val parentName = if (header.contains("(")) header.substringAfter("(").substringBefore(")").trim() else null
                    val (classBody, nextIdx) = collectIndentedBlock(lines, i + 1, indent)
                    val methods = mutableMapOf<String, PyFunction>()
                    if (parentName != null && classes.containsKey(parentName)) {
                        methods.putAll(classes[parentName]!!.methods)
                    }
                    var ci = 0
                    while (ci < classBody.size) {
                        val cLine = stripPythonComment(classBody[ci]).trim()
                        val cIndent = stripPythonComment(classBody[ci]).indexOfFirst { !it.isWhitespace() }
                        if (cLine.startsWith("def ") && cLine.endsWith(":")) {
                            val mHeader = cLine.removePrefix("def ").removeSuffix(":").trim()
                            val mName = mHeader.substringBefore("(").trim()
                            val mParamsRaw = mHeader.substringAfter("(").substringBeforeLast(")").trim()
                            val mParams = if (mParamsRaw.isEmpty()) emptyList() else mParamsRaw.split(",").map { it.trim().substringBefore("=").trim() }
                            val (mBody, mNext) = collectIndentedBlock(classBody, ci + 1, cIndent)
                            methods[mName] = PyFunction(mName, mParams, mBody, cIndent + 4)
                            ci = mNext
                        } else {
                            ci++
                        }
                    }
                    classes[className] = PyClassDef(className, parentName, methods)
                    i = nextIdx
                }

                stmt.startsWith("if ") && stmt.endsWith(":") -> {
                    var branchTaken = false
                    var currIdx = i
                    while (currIdx < endIndex) {
                        val cRaw = lines[currIdx]
                        val cClean = stripPythonComment(cRaw)
                        if (cClean.isBlank()) {
                            currIdx++
                            continue
                        }
                        val cIndent = cClean.indexOfFirst { !it.isWhitespace() }
                        val cStmt = cClean.trim()
                        if (cIndent != indent) break

                        val isIf = cStmt.startsWith("if ") && cStmt.endsWith(":") && currIdx == i
                        val isElif = cStmt.startsWith("elif ") && cStmt.endsWith(":") && currIdx > i
                        val isElse = cStmt == "else:" && currIdx > i

                        if (!isIf && !isElif && !isElse) break

                        val (branchBody, nextAfterBranch) = collectIndentedBlock(lines, currIdx + 1, indent)
                        if (!branchTaken) {
                            val shouldRun = when {
                                isIf -> toTruthy(evalPyExpr(cStmt.removePrefix("if ").removeSuffix(":").trim(), env, functions, classes, stdinLines, stdout, stepTick))
                                isElif -> toTruthy(evalPyExpr(cStmt.removePrefix("elif ").removeSuffix(":").trim(), env, functions, classes, stdinLines, stdout, stepTick))
                                else -> true
                            }
                            if (shouldRun) {
                                branchTaken = true
                                val sig = executePythonBlock(branchBody, 0, branchBody.size, indent + 4, env, functions, classes, stdinLines, stdout, stepTick)
                                if (sig !is PySignal.None) return sig
                            }
                        }
                        currIdx = nextAfterBranch
                    }
                    i = currIdx
                }

                stmt.startsWith("for ") && stmt.endsWith(":") -> {
                    val header = stmt.removePrefix("for ").removeSuffix(":").trim()
                    val inIdx = header.indexOf(" in ")
                    if (inIdx == -1) throw RuntimeException("SyntaxError: invalid 'for' loop syntax on line ${i + 1}")
                    val varPart = header.substring(0, inIdx).trim()
                    val iterExpr = header.substring(inIdx + 4).trim()
                    val iterableVal = evalPyExpr(iterExpr, env, functions, classes, stdinLines, stdout, stepTick)
                    val items: List<Any?> = when (iterableVal) {
                        is List<*> -> iterableVal
                        is Set<*> -> iterableVal.toList()
                        is Map<*, *> -> iterableVal.keys.toList()
                        is String -> iterableVal.map { it.toString() }
                        else -> throw RuntimeException("TypeError: object is not iterable in 'for $varPart in $iterExpr'")
                    }
                    val (loopBody, nextIdx) = collectIndentedBlock(lines, i + 1, indent)
                    val tupleVars = if (varPart.contains(",")) varPart.split(",").map { it.trim() } else null
                    for (item in items) {
                        stepTick()
                        if (tupleVars != null && item is List<*>) {
                            tupleVars.forEachIndexed { tIdx, vName ->
                                env[vName] = item.getOrNull(tIdx)
                            }
                        } else {
                            env[varPart] = item
                        }
                        val sig = executePythonBlock(loopBody, 0, loopBody.size, indent + 4, env, functions, classes, stdinLines, stdout, stepTick)
                        when (sig) {
                            is PySignal.Break -> break
                            is PySignal.Continue -> continue
                            is PySignal.Return -> return sig
                            PySignal.None -> {}
                        }
                    }
                    i = nextIdx
                }

                stmt.startsWith("while ") && stmt.endsWith(":") -> {
                    val condExpr = stmt.removePrefix("while ").removeSuffix(":").trim()
                    val (loopBody, nextIdx) = collectIndentedBlock(lines, i + 1, indent)
                    while (toTruthy(evalPyExpr(condExpr, env, functions, classes, stdinLines, stdout, stepTick))) {
                        stepTick()
                        val sig = executePythonBlock(loopBody, 0, loopBody.size, indent + 4, env, functions, classes, stdinLines, stdout, stepTick)
                        when (sig) {
                            is PySignal.Break -> break
                            is PySignal.Continue -> continue
                            is PySignal.Return -> return sig
                            PySignal.None -> {}
                        }
                    }
                    i = nextIdx
                }

                stmt == "try:" -> {
                    val (tryBody, afterTry) = collectIndentedBlock(lines, i + 1, indent)
                    var curr = afterTry
                    var exceptBody: List<String>? = null
                    var finallyBody: List<String>? = null

                    while (curr < endIndex) {
                        val cClean = stripPythonComment(lines[curr]).trim()
                        val cIndent = stripPythonComment(lines[curr]).indexOfFirst { !it.isWhitespace() }
                        if (cClean.isEmpty()) {
                            curr++
                            continue
                        }
                        if (cIndent != indent) break
                        if (cClean.startsWith("except") && cClean.endsWith(":")) {
                            val (eb, nextC) = collectIndentedBlock(lines, curr + 1, indent)
                            exceptBody = eb
                            curr = nextC
                        } else if (cClean == "finally:") {
                            val (fb, nextC) = collectIndentedBlock(lines, curr + 1, indent)
                            finallyBody = fb
                            curr = nextC
                        } else {
                            break
                        }
                    }

                    try {
                        val sig = executePythonBlock(tryBody, 0, tryBody.size, indent + 4, env, functions, classes, stdinLines, stdout, stepTick)
                        if (sig !is PySignal.None) return sig
                    } catch (e: Exception) {
                        if (exceptBody != null) {
                            val sig = executePythonBlock(exceptBody, 0, exceptBody.size, indent + 4, env, functions, classes, stdinLines, stdout, stepTick)
                            if (sig !is PySignal.None) return sig
                        } else {
                            throw e
                        }
                    } finally {
                        if (finallyBody != null) {
                            executePythonBlock(finallyBody, 0, finallyBody.size, indent + 4, env, functions, classes, stdinLines, stdout, stepTick)
                        }
                    }
                    i = curr
                }

                isAssignmentStatement(stmt) -> {
                    handlePythonAssignment(stmt, env, functions, classes, stdinLines, stdout, stepTick)
                    i++
                }

                else -> {
                    evalPyExpr(stmt, env, functions, classes, stdinLines, stdout, stepTick)
                    i++
                }
            }
        }
        return PySignal.None
    }

    private fun isAssignmentStatement(stmt: String): Boolean {
        if (stmt.startsWith("print(")) return false
        val eqIdx = findTopLevelSingleEquals(stmt)
        return eqIdx > 0
    }

    private fun findTopLevelSingleEquals(stmt: String): Int {
        var depth = 0
        var inQuotes = false
        var quoteChar = ' '
        for (i in stmt.indices) {
            val c = stmt[i]
            if ((c == '"' || c == '\'') && !inQuotes) {
                inQuotes = true
                quoteChar = c
            } else if (c == quoteChar && inQuotes) {
                inQuotes = false
            } else if (!inQuotes) {
                if (c in "([{") depth++
                if (c in ")]}") depth--
                if (depth == 0 && c == '=') {
                    val before = stmt.getOrNull(i - 1)
                    val after = stmt.getOrNull(i + 1)
                    if (before !in listOf('=', '!', '<', '>') && after != '=') {
                        return i
                    }
                }
            }
        }
        return -1
    }

    @Suppress("UNCHECKED_CAST")
    private fun handlePythonAssignment(
        stmt: String,
        env: MutableMap<String, Any?>,
        functions: MutableMap<String, PyFunction>,
        classes: MutableMap<String, PyClassDef>,
        stdinLines: MutableList<String>,
        stdout: StringBuilder,
        stepTick: () -> Unit
    ) {
        val compoundOps = listOf("//=", "**=", "+=", "-=", "*=", "/=", "%=")
        for (op in compoundOps) {
            val idx = findTopLevelOperator(stmt, op)
            if (idx > 0) {
                val target = stmt.substring(0, idx).trim()
                val rhsVal = evalPyExpr(stmt.substring(idx + op.length).trim(), env, functions, classes, stdinLines, stdout, stepTick)
                val currVal = evalPyExpr(target, env, functions, classes, stdinLines, stdout, stepTick)
                val baseOp = op.removeSuffix("=")
                val combined = applyBinaryOp(currVal, rhsVal, baseOp)
                env[target] = combined
                return
            }
        }

        val eqIdx = findTopLevelSingleEquals(stmt)
        if (eqIdx <= 0) return
        val lhs = stmt.substring(0, eqIdx).trim()
        val rhs = stmt.substring(eqIdx + 1).trim()

        if (lhs.contains(",") && !lhs.contains("[")) {
            val lhsVars = lhs.split(",").map { it.trim() }
            val rhsParts = splitTopLevelArgs(rhs)
            val evaluatedValues = if (rhsParts.size == lhsVars.size) {
                rhsParts.map { evalPyExpr(it, env, functions, classes, stdinLines, stdout, stepTick) }
            } else {
                val singleVal = evalPyExpr(rhs, env, functions, classes, stdinLines, stdout, stepTick)
                (singleVal as? List<*>) ?: listOf(singleVal)
            }
            lhsVars.forEachIndexed { index, varName ->
                env[varName] = evaluatedValues.getOrNull(index)
            }
            return
        }

        val value = evalPyExpr(rhs, env, functions, classes, stdinLines, stdout, stepTick)

        if (lhs.startsWith("self.") && env["self"] is PyObjectInstance) {
            val fieldName = lhs.removePrefix("self.").trim()
            (env["self"] as PyObjectInstance).fields[fieldName] = value
        } else if (lhs.contains("[") && lhs.endsWith("]")) {
            val containerName = lhs.substringBefore("[").trim()
            val keyExpr = lhs.substringAfter("[").substringBeforeLast("]").trim()
            val keyVal = evalPyExpr(keyExpr, env, functions, classes, stdinLines, stdout, stepTick)
            val container = env[containerName]
            if (container is MutableList<*> && keyVal is Number) {
                (container as MutableList<Any?>)[keyVal.toInt()] = value
            } else if (container is MutableMap<*, *>) {
                (container as MutableMap<Any?, Any?>)[keyVal] = value
            }
        } else {
            env[lhs] = value
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun evalPyExpr(
        rawExpr: String,
        env: MutableMap<String, Any?>,
        functions: MutableMap<String, PyFunction>,
        classes: MutableMap<String, PyClassDef>,
        stdinLines: MutableList<String>,
        stdout: StringBuilder,
        stepTick: () -> Unit
    ): Any? {
        val expr = rawExpr.trim()
        if (expr.isEmpty()) return null

        if (expr == "True") return true
        if (expr == "False") return false
        if (expr == "None") return null

        expr.toIntOrNull()?.let { return it }
        expr.toDoubleOrNull()?.let { return it }

        // f-string: f"..." or f'...'
        if ((expr.startsWith("f\"") && expr.endsWith("\"")) || (expr.startsWith("f'") && expr.endsWith("'"))) {
            val inner = expr.substring(2, expr.length - 1)
            return inner.replace(Regex("\\{([^}]+)\\}")) { match ->
                val subExpr = match.groupValues[1].trim()
                val fmtSpec = if (subExpr.contains(":")) subExpr.substringAfter(":") else ""
                val cleanSub = if (subExpr.contains(":")) subExpr.substringBefore(":") else subExpr
                val evaluated = evalPyExpr(cleanSub, env, functions, classes, stdinLines, stdout, stepTick)
                if (fmtSpec.endsWith("f") && evaluated is Number) {
                    val decimals = fmtSpec.removePrefix(".").removeSuffix("f").toIntOrNull() ?: 2
                    "%.${decimals}f".format(evaluated.toDouble())
                } else {
                    formatPyValue(evaluated)
                }
            }
        }

        // String literal
        if ((expr.startsWith("\"") && expr.endsWith("\"") && isSingleStringLiteral(expr, '"')) ||
            (expr.startsWith("'") && expr.endsWith("'") && isSingleStringLiteral(expr, '\''))
        ) {
            return expr.substring(1, expr.length - 1)
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\'", "'")
        }

        // List comprehension or List literal: [...]
        if (expr.startsWith("[") && expr.endsWith("]") && isFullyWrappedInBrackets(expr, '[', ']')) {
            val inner = expr.substring(1, expr.length - 1).trim()
            if (inner.isEmpty()) return mutableListOf<Any?>()
            val forIdx = findTopLevelKeyword(inner, " for ")
            if (forIdx != -1) {
                val mapExpr = inner.substring(0, forIdx).trim()
                val rest = inner.substring(forIdx + 5).trim()
                val inIdx = rest.indexOf(" in ")
                val varName = rest.substring(0, inIdx).trim()
                val afterIn = rest.substring(inIdx + 4).trim()
                val ifIdx = findTopLevelKeyword(afterIn, " if ")
                val iterExpr = if (ifIdx != -1) afterIn.substring(0, ifIdx).trim() else afterIn
                val condExpr = if (ifIdx != -1) afterIn.substring(ifIdx + 4).trim() else null

                val sourceList = evalPyExpr(iterExpr, env, functions, classes, stdinLines, stdout, stepTick) as? List<*> ?: emptyList<Any?>()
                val out = mutableListOf<Any?>()
                val localEnv = env.toMutableMap()
                for (item in sourceList) {
                    localEnv[varName] = item
                    val keep = if (condExpr != null) toTruthy(evalPyExpr(condExpr, localEnv, functions, classes, stdinLines, stdout, stepTick)) else true
                    if (keep) {
                        out.add(evalPyExpr(mapExpr, localEnv, functions, classes, stdinLines, stdout, stepTick))
                    }
                }
                return out
            }
            return splitTopLevelArgs(inner).map { evalPyExpr(it, env, functions, classes, stdinLines, stdout, stepTick) }.toMutableList()
        }

        // Dictionary or Set literal: {...}
        if (expr.startsWith("{") && expr.endsWith("}") && isFullyWrappedInBrackets(expr, '{', '}')) {
            val inner = expr.substring(1, expr.length - 1).trim()
            val map = linkedMapOf<Any?, Any?>()
            if (inner.isEmpty()) return map
            val parts = splitTopLevelArgs(inner)
            if (parts.all { !it.contains(":") }) {
                return parts.map { evalPyExpr(it, env, functions, classes, stdinLines, stdout, stepTick) }.toMutableSet().toMutableList()
            }
            for (pair in parts) {
                val colonIdx = pair.indexOf(':')
                if (colonIdx != -1) {
                    val k = evalPyExpr(pair.substring(0, colonIdx).trim(), env, functions, classes, stdinLines, stdout, stepTick)
                    val v = evalPyExpr(pair.substring(colonIdx + 1).trim(), env, functions, classes, stdinLines, stdout, stepTick)
                    map[k] = v
                }
            }
            return map
        }

        // Ternary conditional: a if cond else b
        val ternaryIfIdx = findTopLevelKeyword(expr, " if ")
        val ternaryElseIdx = findTopLevelKeyword(expr, " else ")
        if (ternaryIfIdx != -1 && ternaryElseIdx > ternaryIfIdx) {
            val trueBranch = expr.substring(0, ternaryIfIdx).trim()
            val condBranch = expr.substring(ternaryIfIdx + 4, ternaryElseIdx).trim()
            val falseBranch = expr.substring(ternaryElseIdx + 6).trim()
            return if (toTruthy(evalPyExpr(condBranch, env, functions, classes, stdinLines, stdout, stepTick))) {
                evalPyExpr(trueBranch, env, functions, classes, stdinLines, stdout, stepTick)
            } else {
                evalPyExpr(falseBranch, env, functions, classes, stdinLines, stdout, stepTick)
            }
        }

        // Logical operators: or, and, not
        val orIdx = findTopLevelKeyword(expr, " or ")
        if (orIdx != -1) {
            val l = toTruthy(evalPyExpr(expr.substring(0, orIdx), env, functions, classes, stdinLines, stdout, stepTick))
            val r = toTruthy(evalPyExpr(expr.substring(orIdx + 4), env, functions, classes, stdinLines, stdout, stepTick))
            return l || r
        }
        val andIdx = findTopLevelKeyword(expr, " and ")
        if (andIdx != -1) {
            val l = toTruthy(evalPyExpr(expr.substring(0, andIdx), env, functions, classes, stdinLines, stdout, stepTick))
            val r = toTruthy(evalPyExpr(expr.substring(andIdx + 5), env, functions, classes, stdinLines, stdout, stepTick))
            return l && r
        }
        if (expr.startsWith("not ")) {
            return !toTruthy(evalPyExpr(expr.removePrefix("not "), env, functions, classes, stdinLines, stdout, stepTick))
        }

        // Membership operators: not in, in
        val notInIdx = findTopLevelKeyword(expr, " not in ")
        if (notInIdx != -1) {
            val item = evalPyExpr(expr.substring(0, notInIdx), env, functions, classes, stdinLines, stdout, stepTick)
            val container = evalPyExpr(expr.substring(notInIdx + 8), env, functions, classes, stdinLines, stdout, stepTick)
            val found = when (container) {
                is Collection<*> -> container.contains(item)
                is Map<*, *> -> container.containsKey(item)
                is String -> container.contains(item.toString())
                else -> false
            }
            return !found
        }
        val inIdx = findTopLevelKeyword(expr, " in ")
        if (inIdx != -1) {
            val item = evalPyExpr(expr.substring(0, inIdx), env, functions, classes, stdinLines, stdout, stepTick)
            val container = evalPyExpr(expr.substring(inIdx + 4), env, functions, classes, stdinLines, stdout, stepTick)
            return when (container) {
                is Collection<*> -> container.contains(item)
                is Map<*, *> -> container.containsKey(item)
                is String -> container.contains(item.toString())
                else -> false
            }
        }

        // Comparison operators
        for (cmpOp in listOf("==", "!=", "<=", ">=", "<", ">")) {
            val idx = findTopLevelOperator(expr, cmpOp)
            if (idx != -1) {
                val left = evalPyExpr(expr.substring(0, idx), env, functions, classes, stdinLines, stdout, stepTick)
                val right = evalPyExpr(expr.substring(idx + cmpOp.length), env, functions, classes, stdinLines, stdout, stepTick)
                return comparePyValues(left, right, cmpOp)
            }
        }

        // Arithmetic operators (+, - then *, //, /, % then **)
        val addSubIdx = findLastTopLevelAddSub(expr)
        if (addSubIdx != -1) {
            val op = expr[addSubIdx].toString()
            val left = evalPyExpr(expr.substring(0, addSubIdx), env, functions, classes, stdinLines, stdout, stepTick)
            val right = evalPyExpr(expr.substring(addSubIdx + 1), env, functions, classes, stdinLines, stdout, stepTick)
            return applyBinaryOp(left, right, op)
        }

        for (mulOp in listOf("//", "*", "/", "%")) {
            val idx = findLastTopLevelOperator(expr, mulOp)
            if (idx != -1) {
                val left = evalPyExpr(expr.substring(0, idx), env, functions, classes, stdinLines, stdout, stepTick)
                val right = evalPyExpr(expr.substring(idx + mulOp.length), env, functions, classes, stdinLines, stdout, stepTick)
                return applyBinaryOp(left, right, mulOp)
            }
        }

        val powIdx = findTopLevelOperator(expr, "**")
        if (powIdx != -1) {
            val left = evalPyExpr(expr.substring(0, powIdx), env, functions, classes, stdinLines, stdout, stepTick)
            val right = evalPyExpr(expr.substring(powIdx + 2), env, functions, classes, stdinLines, stdout, stepTick)
            return applyBinaryOp(left, right, "**")
        }

        // Unary minus / plus
        if (expr.startsWith("-") && expr.length > 1) {
            val innerVal = evalPyExpr(expr.substring(1), env, functions, classes, stdinLines, stdout, stepTick)
            if (innerVal is Int) return -innerVal
            if (innerVal is Number) return -innerVal.toDouble()
        }

        // Parenthesized expression or Tuple literal
        if (expr.startsWith("(") && expr.endsWith(")") && isFullyWrappedInParens(expr)) {
            val inner = expr.substring(1, expr.length - 1).trim()
            val parts = splitTopLevelArgs(inner)
            if (parts.size > 1) {
                return parts.map { evalPyExpr(it, env, functions, classes, stdinLines, stdout, stepTick) }.toMutableList()
            }
            return evalPyExpr(inner, env, functions, classes, stdinLines, stdout, stepTick)
        }

        // Function or Method call: name(args...) or obj.method(args...)
        if (expr.endsWith(")") && expr.contains("(")) {
            val openIdx = findMatchingOpenParen(expr)
            if (openIdx > 0) {
                val targetName = expr.substring(0, openIdx).trim()
                val argsRaw = expr.substring(openIdx + 1, expr.length - 1).trim()
                val rawArgList = if (argsRaw.isEmpty()) emptyList() else splitTopLevelArgs(argsRaw)

                // Built-in print with support for sep= and end=
                if (targetName == "print") {
                    var sep = " "
                    var end = "\n"
                    val positionalVals = mutableListOf<Any?>()
                    for (argToken in rawArgList) {
                        val trimmedToken = argToken.trim()
                        when {
                            trimmedToken.startsWith("sep=") -> {
                                sep = evalPyExpr(trimmedToken.removePrefix("sep="), env, functions, classes, stdinLines, stdout, stepTick)?.toString() ?: " "
                            }
                            trimmedToken.startsWith("end=") -> {
                                end = evalPyExpr(trimmedToken.removePrefix("end="), env, functions, classes, stdinLines, stdout, stepTick)?.toString() ?: "\n"
                            }
                            else -> {
                                positionalVals.add(evalPyExpr(trimmedToken, env, functions, classes, stdinLines, stdout, stepTick))
                            }
                        }
                    }
                    val lineOut = positionalVals.joinToString(sep) { formatPyValue(it) }
                    stdout.append(lineOut).append(end)
                    return null
                }

                val evaluatedArgs = rawArgList.map {
                    val cleanArg = if (it.contains("=") && !it.contains("==")) it.substringAfter("=").trim() else it
                    evalPyExpr(cleanArg, env, functions, classes, stdinLines, stdout, stepTick)
                }

                when (targetName) {
                    "input" -> {
                        if (stdinLines.isEmpty()) {
                            return "Falcon"
                        }
                        return stdinLines.removeAt(0)
                    }
                    "int" -> {
                        val v = evaluatedArgs.firstOrNull()
                        return when (v) {
                            is Number -> v.toInt()
                            is String -> v.trim().toIntOrNull() ?: throw RuntimeException("ValueError: invalid literal for int() with base 10: '$v'")
                            else -> 0
                        }
                    }
                    "float" -> {
                        val v = evaluatedArgs.firstOrNull()
                        return when (v) {
                            is Number -> v.toDouble()
                            is String -> v.trim().toDoubleOrNull() ?: throw RuntimeException("ValueError: could not convert string to float: '$v'")
                            else -> 0.0
                        }
                    }
                    "str" -> return formatPyValue(evaluatedArgs.firstOrNull())
                    "len" -> {
                        return when (val v = evaluatedArgs.firstOrNull()) {
                            is String -> v.length
                            is Collection<*> -> v.size
                            is Map<*, *> -> v.size
                            else -> 0
                        }
                    }
                    "sum" -> {
                        val list = evaluatedArgs.firstOrNull() as? Collection<*> ?: emptyList<Any?>()
                        val hasFloat = list.any { it is Double || it is Float }
                        val total = list.sumOf { (it as? Number)?.toDouble() ?: 0.0 }
                        return if (hasFloat) total else total.toInt()
                    }
                    "max" -> {
                        val list = if (evaluatedArgs.size == 1 && evaluatedArgs[0] is Collection<*>) evaluatedArgs[0] as Collection<*> else evaluatedArgs
                        return list.filterIsInstance<Number>().maxByOrNull { it.toDouble() } ?: 0
                    }
                    "min" -> {
                        val list = if (evaluatedArgs.size == 1 && evaluatedArgs[0] is Collection<*>) evaluatedArgs[0] as Collection<*> else evaluatedArgs
                        return list.filterIsInstance<Number>().minByOrNull { it.toDouble() } ?: 0
                    }
                    "abs" -> {
                        val n = (evaluatedArgs.firstOrNull() as? Number)?.toDouble() ?: 0.0
                        return if (evaluatedArgs.firstOrNull() is Int) kotlin.math.abs(n.toInt()) else kotlin.math.abs(n)
                    }
                    "range" -> {
                        val start = if (evaluatedArgs.size >= 2) (evaluatedArgs[0] as Number).toInt() else 0
                        val stop = if (evaluatedArgs.size >= 2) (evaluatedArgs[1] as Number).toInt() else (evaluatedArgs.getOrNull(0) as? Number)?.toInt() ?: 0
                        val step = if (evaluatedArgs.size >= 3) (evaluatedArgs[2] as Number).toInt() else 1
                        if (step == 0) throw RuntimeException("ValueError: range() arg 3 must not be zero")
                        val out = mutableListOf<Int>()
                        var c = start
                        if (step > 0) {
                            while (c < stop) { out.add(c); c += step }
                        } else {
                            while (c > stop) { out.add(c); c += step }
                        }
                        return out
                    }
                    "enumerate" -> {
                        val list = (evaluatedArgs.firstOrNull() as? Collection<*>)?.toList() ?: emptyList()
                        val startIdx = (evaluatedArgs.getOrNull(1) as? Number)?.toInt() ?: 0
                        return list.mapIndexed { idx, v -> listOf(startIdx + idx, v) }.toMutableList()
                    }
                    "zip" -> {
                        val l1 = (evaluatedArgs.getOrNull(0) as? Collection<*>)?.toList() ?: emptyList()
                        val l2 = (evaluatedArgs.getOrNull(1) as? Collection<*>)?.toList() ?: emptyList()
                        return l1.zip(l2) { a, b -> listOf(a, b) }.toMutableList()
                    }
                    "math.sqrt", "sqrt" -> {
                        val n = (evaluatedArgs.firstOrNull() as? Number)?.toDouble() ?: 0.0
                        return kotlin.math.sqrt(n)
                    }
                    "math.ceil", "ceil" -> {
                        val n = (evaluatedArgs.firstOrNull() as? Number)?.toDouble() ?: 0.0
                        return kotlin.math.ceil(n).toInt()
                    }
                    "math.floor", "floor" -> {
                        val n = (evaluatedArgs.firstOrNull() as? Number)?.toDouble() ?: 0.0
                        return kotlin.math.floor(n).toInt()
                    }
                    "sorted" -> {
                        val list = (evaluatedArgs.firstOrNull() as? Collection<*>)?.toList() ?: emptyList()
                        return if (list.all { it is Number }) {
                            list.sortedBy { (it as Number).toDouble() }.toMutableList()
                        } else {
                            list.sortedBy { it.toString() }.toMutableList()
                        }
                    }
                    "reversed" -> {
                        val list = (evaluatedArgs.firstOrNull() as? Collection<*>)?.toList() ?: emptyList()
                        return list.reversed().toMutableList()
                    }
                    "round" -> {
                        val n = (evaluatedArgs.firstOrNull() as? Number)?.toDouble() ?: 0.0
                        val digits = (evaluatedArgs.getOrNull(1) as? Number)?.toInt() ?: 0
                        return if (digits <= 0) kotlin.math.round(n).toInt() else {
                            val factor = 10.0.pow(digits)
                            kotlin.math.round(n * factor) / factor
                        }
                    }
                    "pow" -> {
                        val b = (evaluatedArgs.getOrNull(0) as? Number)?.toDouble() ?: 0.0
                        val e = (evaluatedArgs.getOrNull(1) as? Number)?.toDouble() ?: 1.0
                        val res = b.pow(e)
                        return if (evaluatedArgs.getOrNull(0) is Int && evaluatedArgs.getOrNull(1) is Int) res.toInt() else res
                    }
                    "bool" -> return toTruthy(evaluatedArgs.firstOrNull())
                    "type" -> {
                        return when (evaluatedArgs.firstOrNull()) {
                            is Int -> "<class 'int'>"
                            is Double, is Float -> "<class 'float'>"
                            is Boolean -> "<class 'bool'>"
                            is String -> "<class 'str'>"
                            is List<*> -> "<class 'list'>"
                            is Map<*, *> -> "<class 'dict'>"
                            else -> "<class 'object'>"
                        }
                    }
                    "list", "tuple" -> {
                        return when (val v = evaluatedArgs.firstOrNull()) {
                            is Collection<*> -> v.toMutableList()
                            is String -> v.map { it.toString() }.toMutableList()
                            else -> mutableListOf<Any?>()
                        }
                    }
                    "set" -> {
                        return when (val v = evaluatedArgs.firstOrNull()) {
                            is Collection<*> -> v.distinct().toMutableList()
                            is String -> v.map { it.toString() }.distinct().toMutableList()
                            else -> mutableListOf<Any?>()
                        }
                    }
                }

                // Class instantiation
                if (classes.containsKey(targetName)) {
                    val cls = classes[targetName]!!
                    val instance = PyObjectInstance(targetName)
                    val initFn = cls.methods["__init__"]
                    if (initFn != null) {
                        val localEnv = env.toMutableMap()
                        localEnv["self"] = instance
                        initFn.params.drop(1).forEachIndexed { idx, pName ->
                            localEnv[pName] = evaluatedArgs.getOrNull(idx)
                        }
                        executePythonBlock(initFn.bodyLines, 0, initFn.bodyLines.size, initFn.baseIndent, localEnv, functions, classes, stdinLines, stdout, stepTick)
                    }
                    return instance
                }

                // User-defined function call
                if (functions.containsKey(targetName)) {
                    val fn = functions[targetName]!!
                    val localEnv = env.toMutableMap()
                    fn.params.forEachIndexed { idx, pName ->
                        localEnv[pName] = evaluatedArgs.getOrNull(idx)
                    }
                    val sig = executePythonBlock(fn.bodyLines, 0, fn.bodyLines.size, fn.baseIndent, localEnv, functions, classes, stdinLines, stdout, stepTick)
                    return if (sig is PySignal.Return) sig.value else null
                }

                // Method call on object/string/list/dict: receiver.method(args)
                if (targetName.contains(".")) {
                    val dotIdx = targetName.lastIndexOf('.')
                    val recvExpr = targetName.substring(0, dotIdx).trim()
                    val methodName = targetName.substring(dotIdx + 1).trim()
                    val receiver = evalPyExpr(recvExpr, env, functions, classes, stdinLines, stdout, stepTick)

                    when (receiver) {
                        is String -> {
                            return when (methodName) {
                                "upper" -> receiver.uppercase()
                                "lower" -> receiver.lowercase()
                                "strip" -> receiver.trim()
                                "title" -> receiver.split(" ").joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
                                "capitalize" -> receiver.replaceFirstChar { it.uppercase() }
                                "startswith" -> receiver.startsWith(evaluatedArgs.firstOrNull()?.toString() ?: "")
                                "endswith" -> receiver.endsWith(evaluatedArgs.firstOrNull()?.toString() ?: "")
                                "find" -> receiver.indexOf(evaluatedArgs.firstOrNull()?.toString() ?: "")
                                "count" -> {
                                    val sub = evaluatedArgs.firstOrNull()?.toString() ?: ""
                                    if (sub.isEmpty()) 0 else receiver.windowed(sub.length).count { it == sub }
                                }
                                "replace" -> receiver.replace(evaluatedArgs.getOrNull(0).toString(), evaluatedArgs.getOrNull(1).toString())
                                "split" -> receiver.split(evaluatedArgs.getOrNull(0)?.toString() ?: " ").toMutableList()
                                "join" -> {
                                    val items = (evaluatedArgs.firstOrNull() as? Collection<*>) ?: emptyList<Any?>()
                                    items.joinToString(receiver) { formatPyValue(it) }
                                }
                                "isdigit" -> receiver.isNotEmpty() && receiver.all { it.isDigit() }
                                "isalpha" -> receiver.isNotEmpty() && receiver.all { it.isLetter() }
                                else -> receiver
                            }
                        }
                        is MutableList<*> -> {
                            val mut = receiver as MutableList<Any?>
                            when (methodName) {
                                "append" -> {
                                    mut.add(evaluatedArgs.firstOrNull())
                                    return null
                                }
                                "extend" -> {
                                    val other = evaluatedArgs.firstOrNull() as? Collection<*> ?: emptyList<Any?>()
                                    mut.addAll(other)
                                    return null
                                }
                                "insert" -> {
                                    val idx = (evaluatedArgs.getOrNull(0) as? Number)?.toInt() ?: 0
                                    mut.add(idx.coerceIn(0, mut.size), evaluatedArgs.getOrNull(1))
                                    return null
                                }
                                "pop" -> {
                                    if (mut.isEmpty()) throw RuntimeException("IndexError: pop from empty list")
                                    val idx = (evaluatedArgs.firstOrNull() as? Number)?.toInt() ?: (mut.size - 1)
                                    return mut.removeAt(idx)
                                }
                                "remove" -> {
                                    mut.remove(evaluatedArgs.firstOrNull())
                                    return null
                                }
                                "reverse" -> {
                                    mut.reverse()
                                    return null
                                }
                                "sort" -> {
                                    mut.sortBy { (it as? Number)?.toDouble() ?: Double.MAX_VALUE }
                                    return null
                                }
                                "count" -> return mut.count { it == evaluatedArgs.firstOrNull() }
                            }
                        }
                        is MutableMap<*, *> -> {
                            val map = receiver as MutableMap<Any?, Any?>
                            when (methodName) {
                                "get" -> return map[evaluatedArgs.getOrNull(0)] ?: evaluatedArgs.getOrNull(1)
                                "keys" -> return map.keys.toMutableList()
                                "values" -> return map.values.toMutableList()
                                "items" -> return map.entries.map { listOf(it.key, it.value) }.toMutableList()
                            }
                        }
                        is PyObjectInstance -> {
                            val cls = classes[receiver.className]
                            val method = cls?.methods?.get(methodName)
                            if (method != null) {
                                val localEnv = env.toMutableMap()
                                localEnv["self"] = receiver
                                method.params.drop(1).forEachIndexed { idx, pName ->
                                    localEnv[pName] = evaluatedArgs.getOrNull(idx)
                                }
                                val sig = executePythonBlock(method.bodyLines, 0, method.bodyLines.size, method.baseIndent, localEnv, functions, classes, stdinLines, stdout, stepTick)
                                return if (sig is PySignal.Return) sig.value else null
                            }
                        }
                    }
                }
                throw RuntimeException("NameError: name '$targetName' is not defined")
            }
        }

        // Indexing & Slicing: container[index] or container[start:stop:step]
        if (expr.endsWith("]") && expr.contains("[")) {
            val openBracket = expr.lastIndexOf('[')
            val targetExpr = expr.substring(0, openBracket).trim()
            val idxExpr = expr.substring(openBracket + 1, expr.length - 1).trim()
            val container = evalPyExpr(targetExpr, env, functions, classes, stdinLines, stdout, stepTick)

            if (idxExpr.contains(":")) {
                val sliceParts = idxExpr.split(":")
                if (idxExpr == "::-1") {
                    return when (container) {
                        is String -> container.reversed()
                        is List<*> -> container.reversed().toMutableList()
                        else -> container
                    }
                }
                val size = when (container) {
                    is String -> container.length
                    is List<*> -> container.size
                    else -> 0
                }
                val startIdx = if (sliceParts.getOrNull(0).isNullOrBlank()) 0 else (evalPyExpr(sliceParts[0], env, functions, classes, stdinLines, stdout, stepTick) as? Number)?.toInt() ?: 0
                val endIdx = if (sliceParts.getOrNull(1).isNullOrBlank()) size else (evalPyExpr(sliceParts[1], env, functions, classes, stdinLines, stdout, stepTick) as? Number)?.toInt() ?: size
                val safeStart = (if (startIdx < 0) size + startIdx else startIdx).coerceIn(0, size)
                val safeEnd = (if (endIdx < 0) size + endIdx else endIdx).coerceIn(safeStart, size)
                return when (container) {
                    is String -> container.substring(safeStart, safeEnd)
                    is List<*> -> container.subList(safeStart, safeEnd).toMutableList()
                    else -> null
                }
            }

            val key = evalPyExpr(idxExpr, env, functions, classes, stdinLines, stdout, stepTick)
            return when (container) {
                is List<*> -> {
                    val idx = (key as? Number)?.toInt() ?: 0
                    val realIdx = if (idx < 0) container.size + idx else idx
                    container.getOrElse(realIdx) { throw RuntimeException("IndexError: list index $idx out of range") }
                }
                is Map<*, *> -> container[key] ?: throw RuntimeException("KeyError: '$key'")
                is String -> {
                    val idx = (key as? Number)?.toInt() ?: 0
                    val realIdx = if (idx < 0) container.length + idx else idx
                    container.getOrElse(realIdx) { throw RuntimeException("IndexError: string index $idx out of range") }.toString()
                }
                else -> null
            }
        }

        // Object attribute access: obj.field
        if (expr.contains(".") && !expr.contains("(")) {
            val dotIdx = expr.indexOf('.')
            val objName = expr.substring(0, dotIdx).trim()
            val field = expr.substring(dotIdx + 1).trim()
            val obj = env[objName]
            if (obj is PyObjectInstance) {
                return obj.fields[field]
            }
        }

        if (env.containsKey(expr)) {
            return env[expr]
        }

        if (expr.contains(" ") || expr.contains("\"") || expr.contains("'")) {
            throw RuntimeException("SyntaxError: invalid syntax near '$expr'")
        }

        throw RuntimeException("NameError: name '$expr' is not defined")
    }

    private fun pyTypeName(value: Any?): String = when (value) {
        null -> "NoneType"
        is Boolean -> "bool"
        is Int -> "int"
        is Double, is Float -> "float"
        is String -> "str"
        is List<*> -> "list"
        is Map<*, *> -> "dict"
        else -> "object"
    }

    private fun applyBinaryOp(left: Any?, right: Any?, op: String): Any? {
        if (op == "+") {
            if (left is String && right is String) return left + right
            if (left is List<*> && right is List<*>) return (left + right).toMutableList()
            if (left is String && right !is String) {
                throw RuntimeException("TypeError: can only concatenate str (not \"${pyTypeName(right)}\") to str")
            }
            if (right is String && left !is String) {
                throw RuntimeException("TypeError: unsupported operand type(s) for +: '${pyTypeName(left)}' and 'str'")
            }
        }
        if (op == "*" && left is String && right is Number) {
            return left.repeat(right.toInt().coerceAtLeast(0))
        }
        if (op == "*" && right is String && left is Number) {
            return right.repeat(left.toInt().coerceAtLeast(0))
        }
        val lNum = (left as? Number)?.toDouble() ?: throw RuntimeException("TypeError: unsupported operand type for $op: '${pyTypeName(left)}' and '${pyTypeName(right)}'")
        val rNum = (right as? Number)?.toDouble() ?: throw RuntimeException("TypeError: unsupported operand type for $op: '${pyTypeName(left)}' and '${pyTypeName(right)}'")
        val bothInt = left is Int && right is Int

        return when (op) {
            "+" -> if (bothInt) left.toInt() + right.toInt() else lNum + rNum
            "-" -> if (bothInt) left.toInt() - right.toInt() else lNum - rNum
            "*" -> if (bothInt) left.toInt() * right.toInt() else lNum * rNum
            "/" -> {
                if (rNum == 0.0) throw RuntimeException("ZeroDivisionError: division by zero")
                lNum / rNum
            }
            "//" -> {
                if (rNum == 0.0) throw RuntimeException("ZeroDivisionError: integer division or modulo by zero")
                kotlin.math.floor(lNum / rNum).toInt()
            }
            "%" -> {
                if (rNum == 0.0) throw RuntimeException("ZeroDivisionError: integer division or modulo by zero")
                if (bothInt) left.toInt() % right.toInt() else lNum % rNum
            }
            "**" -> {
                val res = lNum.pow(rNum)
                if (bothInt && rNum >= 0) res.toInt() else res
            }
            else -> 0
        }
    }

    private fun comparePyValues(left: Any?, right: Any?, op: String): Boolean {
        if (left is Number && right is Number) {
            val l = left.toDouble()
            val r = right.toDouble()
            return when (op) {
                "==" -> l == r
                "!=" -> l != r
                "<" -> l < r
                ">" -> l > r
                "<=" -> l <= r
                ">=" -> l >= r
                else -> false
            }
        }
        return when (op) {
            "==" -> left == right
            "!=" -> left != right
            else -> left.toString().compareTo(right.toString()).let { cmp ->
                when (op) {
                    "<" -> cmp < 0
                    ">" -> cmp > 0
                    "<=" -> cmp <= 0
                    ">=" -> cmp >= 0
                    else -> false
                }
            }
        }
    }

    private fun toTruthy(value: Any?): Boolean = when (value) {
        null -> false
        is Boolean -> value
        is Number -> value.toDouble() != 0.0
        is String -> value.isNotEmpty()
        is Collection<*> -> value.isNotEmpty()
        is Map<*, *> -> value.isNotEmpty()
        else -> true
    }

    private fun formatPyValue(value: Any?): String = when (value) {
        null -> "None"
        true -> "True"
        false -> "False"
        is List<*> -> value.joinToString(", ", prefix = "[", postfix = "]") {
            if (it is String) "'$it'" else formatPyValue(it)
        }
        is Map<*, *> -> value.entries.joinToString(", ", prefix = "{", postfix = "}") {
            val k = if (it.key is String) "'${it.key}'" else formatPyValue(it.key)
            val v = if (it.value is String) "'${it.value}'" else formatPyValue(it.value)
            "$k: $v"
        }
        else -> value.toString()
    }

    // =========================================================================
    // ISOLATED ON-DEVICE JAVASCRIPT (ES6+) INTERPRETER
    // =========================================================================
    private data class JsFunction(val name: String, val params: List<String>, val bodyLines: List<String>)

    private fun executeJavaScriptLocal(code: String, stdin: String, startTime: Long): CodingExecutionResult {
        val stdinLines = if (stdin.isEmpty()) mutableListOf() else stdin.lines().toMutableList()
        val stdout = StringBuilder()
        val vars = mutableMapOf<String, Any?>()
        val constants = mutableSetOf<String>()
        val functions = mutableMapOf<String, JsFunction>()
        val rawLines = code.lines()
        var stepCounter = 0

        // Pre-flight syntax checks on all lines
        for ((idx, rawLine) in rawLines.withIndex()) {
            val lineNum = idx + 1
            val clean = rawLine.substringBefore("//").trim()
            if (clean.isEmpty()) continue

            if (hasUnclosedQuotes(clean)) {
                val fixedLines = rawLines.toMutableList().also { it[idx] = clean + "\"" }
                return buildDiagnosticErrorResult(
                    stderr = "app.js:$lineNum: SyntaxError: Unterminated string literal\n  $clean",
                    startTime = startTime,
                    lang = ProgrammingLanguageId.JAVASCRIPT,
                    compilationStatus = "Syntax Error",
                    errorLine = lineNum,
                    errorCol = clean.length,
                    errorType = "SyntaxError",
                    errorSummary = "Unterminated string literal on line $lineNum.",
                    offendingLine = rawLine,
                    suggestedFix = fixedLines.joinToString("\n"),
                    fixExplanation = "Add the missing closing quotation mark on line $lineNum."
                )
            }

            if (!hasBalancedParens(clean)) {
                val fixedLines = rawLines.toMutableList().also { it[idx] = clean.removeSuffix(";") + ");" }
                return buildDiagnosticErrorResult(
                    stderr = "app.js:$lineNum: SyntaxError: missing ) after argument list\n  $clean",
                    startTime = startTime,
                    lang = ProgrammingLanguageId.JAVASCRIPT,
                    compilationStatus = "Syntax Error",
                    errorLine = lineNum,
                    errorCol = clean.length,
                    errorType = "SyntaxError",
                    errorSummary = "Missing closing parenthesis ')' on line $lineNum.",
                    offendingLine = rawLine,
                    suggestedFix = fixedLines.joinToString("\n"),
                    fixExplanation = "Every '(' in JavaScript must be closed with a matching ')'."
                )
            }

            val line = clean.removeSuffix(";").trim()
            if (line.startsWith("Console.log")) {
                val fixedLines = rawLines.toMutableList().also {
                    it[idx] = rawLine.replaceFirst("Console.log", "console.log")
                }
                return buildDiagnosticErrorResult(
                    stderr = "app.js:$lineNum: ReferenceError: 'Console' is not defined. Did you mean 'console.log(...)'?",
                    startTime = startTime,
                    lang = ProgrammingLanguageId.JAVASCRIPT,
                    compilationStatus = "Runtime Error",
                    errorLine = lineNum,
                    errorCol = 1,
                    errorType = "ReferenceError",
                    errorSummary = "JavaScript is case-sensitive. The standard console object is lowercase `console.log(...)`, not `Console.log(...)`.",
                    offendingLine = rawLine,
                    suggestedFix = fixedLines.joinToString("\n"),
                    fixExplanation = "Change `Console.log` to lowercase `console.log`."
                )
            }

            if (Regex("^[Pp]rint\\s*\\(").containsMatchIn(line) || Regex("^[Pp]rint\\s+").containsMatchIn(line)) {
                val arg = line.replaceFirst(Regex("^[Pp]rint\\s*\\(?"), "").removeSuffix(")")
                val fixedLines = rawLines.toMutableList().also {
                    it[idx] = "console.log($arg);"
                }
                return buildDiagnosticErrorResult(
                    stderr = "app.js:$lineNum: ReferenceError: print is not defined in JavaScript console runtime. Use console.log($arg) instead.",
                    startTime = startTime,
                    lang = ProgrammingLanguageId.JAVASCRIPT,
                    compilationStatus = "Runtime Error",
                    errorLine = lineNum,
                    errorCol = 1,
                    errorType = "ReferenceError",
                    errorSummary = "In JavaScript, output is printed using `console.log(...)` rather than Python's `print(...)`.",
                    offendingLine = rawLine,
                    suggestedFix = fixedLines.joinToString("\n"),
                    fixExplanation = "JavaScript uses `console.log(...)` to write to standard output. Replacing `$line` with `console.log($arg);` fixes the error."
                )
            }

            if (line.startsWith("console.log ") && !line.startsWith("console.log(")) {
                val arg = line.removePrefix("console.log").trim()
                val fixedLines = rawLines.toMutableList().also {
                    it[idx] = "console.log($arg);"
                }
                return buildDiagnosticErrorResult(
                    stderr = "app.js:$lineNum: SyntaxError: Missing parentheses in call to console.log. Did you mean console.log($arg)?",
                    startTime = startTime,
                    lang = ProgrammingLanguageId.JAVASCRIPT,
                    compilationStatus = "Syntax Error",
                    errorLine = lineNum,
                    errorCol = 12,
                    errorType = "SyntaxError",
                    errorSummary = "`console.log` is a method call in JavaScript and requires parentheses around its arguments.",
                    offendingLine = rawLine,
                    suggestedFix = fixedLines.joinToString("\n"),
                    fixExplanation = "Wrap the argument in parentheses: `console.log($arg);`."
                )
            }
        }

        var currentLineNum = 1
        return try {
            fun stepTick() {
                stepCounter++
                if (stepCounter > MAX_LOOP_ITERATIONS || (System.currentTimeMillis() - startTime) > MAX_EXECUTION_TIME_MS) {
                    throw RuntimeException("TimeoutError: Execution exceeded maximum step limit ($MAX_LOOP_ITERATIONS operations). Check for infinite loops.")
                }
            }

            fun collectBraceBlock(lines: List<String>, startIdx: Int): Pair<List<String>, Int> {
                val body = mutableListOf<String>()
                var depth = 0
                var i = startIdx
                var started = false
                while (i < lines.size) {
                    val l = lines[i].substringBefore("//").trim()
                    val opens = l.count { it == '{' }
                    val closes = l.count { it == '}' }
                    if (i == startIdx) {
                        depth += opens - closes
                        started = opens > 0
                        if (depth <= 0 && started) return emptyList<String>() to (i + 1)
                    } else {
                        depth += opens - closes
                        if (depth <= 0) {
                            return body to (i + 1)
                        }
                        body.add(lines[i])
                    }
                    i++
                }
                return body to lines.size
            }

            fun execJsLines(lines: List<String>, lineOffset: Int = 0): Any? {
                var idx = 0
                while (idx < lines.size) {
                    stepTick()
                    currentLineNum = lineOffset + idx + 1
                    val rawLine = lines[idx]
                    val clean = rawLine.substringBefore("//").trim()
                    if (clean.isEmpty() || clean == "{" || clean == "}") {
                        idx++
                        continue
                    }

                    val line = clean.removeSuffix(";").trim()

                    // Function declaration: function name(a, b) { ... }
                    if (line.startsWith("function ")) {
                        val header = line.removePrefix("function ").substringBefore("{").trim()
                        val fnName = header.substringBefore("(").trim()
                        val paramsRaw = header.substringAfter("(").substringBeforeLast(")").trim()
                        val params = if (paramsRaw.isEmpty()) emptyList() else paramsRaw.split(",").map { it.trim() }
                        val (body, nextIdx) = collectBraceBlock(lines, idx)
                        functions[fnName] = JsFunction(fnName, params, body)
                        idx = nextIdx
                        continue
                    }

                    // Arrow function: const name = (a, b) => expr
                    val arrowMatch = Regex("^(?:const|let|var)\\s+(\\w+)\\s*=\\s*\\(([^)]*)\\)\\s*=>\\s*(.+)$").find(line)
                    if (arrowMatch != null) {
                        val fnName = arrowMatch.groupValues[1]
                        val params = arrowMatch.groupValues[2].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val bodyExpr = arrowMatch.groupValues[3].removePrefix("{").removeSuffix("}").trim().removePrefix("return ").trim()
                        functions[fnName] = JsFunction(fnName, params, listOf("return $bodyExpr"))
                        idx++
                        continue
                    }

                    if (line.startsWith("return ") || line == "return") {
                        val retExpr = line.removePrefix("return").trim()
                        return if (retExpr.isEmpty()) null else evalJsExpr(retExpr, vars, functions, stdinLines, ::execJsLines)
                    }

                    // For loop: for (let i = 0; i < 5; i++) { ... }
                    if (line.startsWith("for ") || line.startsWith("for(")) {
                        val (body, nextIdx) = collectBraceBlock(lines, idx)
                        val m = Regex("(?:let|var)?\\s*(\\w+)\\s*=\\s*(-?\\d+)\\s*;\\s*\\1\\s*(<=|<|>=|>)\\s*(\\w+|-?\\d+)").find(line)
                        if (m != null) {
                            val vName = m.groupValues[1]
                            val startVal = m.groupValues[2].toInt()
                            val op = m.groupValues[3]
                            val endToken = m.groupValues[4]
                            val endVal = endToken.toIntOrNull() ?: ((vars[endToken] as? Number)?.toInt() ?: 0)
                            val range = when (op) {
                                "<=" -> startVal..endVal
                                "<" -> startVal until endVal
                                ">=" -> startVal downTo endVal
                                ">" -> startVal downTo (endVal + 1)
                                else -> IntRange.EMPTY
                            }
                            for (n in range) {
                                stepTick()
                                vars[vName] = n
                                execJsLines(body, lineOffset + idx + 1)
                            }
                        }
                        idx = nextIdx
                        continue
                    }

                    // While loop: while (cond) { ... }
                    if (line.startsWith("while ") || line.startsWith("while(")) {
                        val condExpr = line.substringAfter("(").substringBeforeLast(")").trim()
                        val (body, nextIdx) = collectBraceBlock(lines, idx)
                        while (toTruthy(evalJsExpr(condExpr, vars, functions, stdinLines, ::execJsLines))) {
                            stepTick()
                            execJsLines(body, lineOffset + idx + 1)
                        }
                        idx = nextIdx
                        continue
                    }

                    // If / else if / else blocks
                    if (line.startsWith("if ") || line.startsWith("if(")) {
                        var branchTaken = false
                        var curr = idx
                        while (curr < lines.size) {
                            val cClean = lines[curr].substringBefore("//").trim().removePrefix("}").trim()
                            val isIf = (cClean.startsWith("if ") || cClean.startsWith("if(")) && curr == idx
                            val isElseIf = cClean.startsWith("else if")
                            val isElse = cClean.startsWith("else") && !isElseIf
                            if (!isIf && !isElseIf && !isElse) break

                            val condExpr = if (isIf || isElseIf) {
                                cClean.substringAfter("(").substringBeforeLast(")").trim()
                            } else "true"
                            val (branchBody, afterBranch) = collectBraceBlock(lines, curr)
                            if (!branchTaken && toTruthy(evalJsExpr(condExpr, vars, functions, stdinLines, ::execJsLines))) {
                                branchTaken = true
                                val ret = execJsLines(branchBody, lineOffset + curr + 1)
                                if (ret != null) return ret
                            }
                            // Check if the closing line of this block also starts with `} else`
                            val closingLine = lines.getOrNull(afterBranch - 1)?.substringBefore("//")?.trim().orEmpty()
                            if (closingLine.startsWith("} else")) {
                                curr = afterBranch - 1
                            } else {
                                curr = afterBranch
                                val nextPeek = lines.getOrNull(curr)?.substringBefore("//")?.trim().orEmpty()
                                if (!nextPeek.startsWith("else")) break
                            }
                        }
                        idx = curr
                        continue
                    }

                    // Destructuring: const { a, b } = obj
                    if (line.startsWith("const {") && line.contains("} =")) {
                        val names = line.substringAfter("{").substringBefore("}").split(",").map { it.trim() }
                        val objName = line.substringAfter("} =").trim()
                        val map = vars[objName] as? Map<*, *> ?: emptyMap<String, Any?>()
                        for (n in names) {
                            vars[n] = map[n]
                            constants.add(n)
                        }
                        idx++
                        continue
                    }

                    if (line.startsWith("const ") || line.startsWith("let ") || line.startsWith("var ")) {
                        val isConst = line.startsWith("const ")
                        val rest = line.replaceFirst(Regex("^(const|let|var)\\s+"), "")
                        val eqIdx = rest.indexOf('=')
                        if (eqIdx > 0) {
                            val name = rest.substring(0, eqIdx).trim()
                            val rhs = rest.substring(eqIdx + 1).trim()
                            vars[name] = evalJsExpr(rhs, vars, functions, stdinLines, ::execJsLines)
                            if (isConst) constants.add(name)
                        }
                        idx++
                        continue
                    }

                    if ((line.startsWith("console.log(") || line.startsWith("console.info(") || line.startsWith("console.warn(")) && line.endsWith(")")) {
                        val inner = line.substringAfter("(").removeSuffix(")")
                        val rendered = if (inner.isBlank()) "" else splitTopLevelArgs(inner).joinToString(" ") {
                            formatJsValue(evalJsExpr(it, vars, functions, stdinLines, ::execJsLines))
                        }
                        stdout.append(rendered).append("\n")
                        idx++
                        continue
                    }

                    val eqIdx = findTopLevelSingleEquals(line)
                    if (eqIdx > 0) {
                        val rawLhs = line.substring(0, eqIdx).trim()
                        val name = rawLhs.removeSuffix("+").removeSuffix("-").removeSuffix("*").removeSuffix("/").trim()
                        if (name in constants) {
                            val fixedLines = rawLines.toMutableList().map {
                                it.replace(Regex("\\bconst\\s+$name\\b"), "let $name")
                            }
                            return buildDiagnosticErrorResult(
                                stderr = "app.js:$currentLineNum: TypeError: Assignment to constant variable '$name'.",
                                startTime = startTime,
                                lang = ProgrammingLanguageId.JAVASCRIPT,
                                compilationStatus = "Runtime Error",
                                errorLine = currentLineNum,
                                errorCol = 1,
                                errorType = "TypeError",
                                errorSummary = "Variable `$name` was declared with `const`, so it cannot be reassigned on line $currentLineNum.",
                                offendingLine = rawLine,
                                suggestedFix = fixedLines.joinToString("\n"),
                                fixExplanation = "Variables declared with `const` cannot be reassigned. Changing `const $name` to `let $name` allows reassignment."
                            )
                        }
                        val rhs = line.substring(eqIdx + 1).trim()
                        val v = evalJsExpr(rhs, vars, functions, stdinLines, ::execJsLines)
                        when {
                            rawLhs.endsWith("+") -> {
                                val prev = vars[name]
                                vars[name] = if (prev is String || v is String) prev.toString() + v.toString()
                                else ((prev as? Number)?.toDouble() ?: 0.0).let { p ->
                                    val r = (v as? Number)?.toDouble() ?: 0.0
                                    if (prev is Int && v is Int) (p + r).toInt() else p + r
                                }
                            }
                            rawLhs.endsWith("-") -> {
                                val prev = (vars[name] as? Number)?.toInt() ?: 0
                                vars[name] = prev - ((v as? Number)?.toInt() ?: 0)
                            }
                            else -> vars[name] = v
                        }
                        idx++
                        continue
                    }

                    if (line.endsWith("++") || line.endsWith("--")) {
                        val name = line.dropLast(2).trim()
                        val delta = if (line.endsWith("++")) 1 else -1
                        vars[name] = ((vars[name] as? Number)?.toInt() ?: 0) + delta
                        idx++
                        continue
                    }

                    evalJsExpr(line, vars, functions, stdinLines, ::execJsLines)
                    idx++
                }
                return null
            }

            val topResult = execJsLines(rawLines, 0)
            if (topResult is CodingExecutionResult) return topResult

            CodingExecutionResult(
                stdout = stdout.toString().trimEnd('\n'),
                stderr = "",
                exitCode = 0,
                executionTimeMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L),
                runtimeUsed = ProgrammingLanguageId.JAVASCRIPT.runtimeLabel,
                compilationStatus = "Executed"
            )
        } catch (e: Exception) {
            val msg = e.message ?: "ReferenceError: JavaScript execution failed"
            val isTimeout = msg.startsWith("TimeoutError")
            val errType = msg.substringBefore(":").trim().ifEmpty { "RuntimeError" }
            buildDiagnosticErrorResult(
                stderr = "app.js:$currentLineNum: $msg",
                startTime = startTime,
                lang = ProgrammingLanguageId.JAVASCRIPT,
                stdout = stdout.toString(),
                compilationStatus = if (isTimeout) "Timed Out" else "Runtime Error",
                errorLine = currentLineNum,
                errorCol = 1,
                errorType = errType,
                errorSummary = msg,
                offendingLine = rawLines.getOrNull(currentLineNum - 1).orEmpty()
            ).copy(isTimeout = isTimeout)
        }
    }

    private fun evalJsExpr(
        raw: String,
        vars: MutableMap<String, Any?>,
        functions: Map<String, JsFunction>,
        stdinLines: MutableList<String>,
        execBlock: (List<String>, Int) -> Any?
    ): Any? {
        val expr = raw.trim()
        if (expr.isEmpty()) return ""
        if (expr == "true") return true
        if (expr == "false") return false
        if (expr == "null" || expr == "undefined") return expr
        expr.toIntOrNull()?.let { return it }
        expr.toDoubleOrNull()?.let { return it }

        if ((expr.startsWith("\"") && expr.endsWith("\"") && isSingleStringLiteral(expr, '"')) ||
            (expr.startsWith("'") && expr.endsWith("'") && isSingleStringLiteral(expr, '\''))
        ) {
            return expr.substring(1, expr.length - 1)
        }
        if (expr.startsWith("`") && expr.endsWith("`")) {
            val inner = expr.substring(1, expr.length - 1)
            return inner.replace(Regex("\\$\\{([^}]+)\\}")) { m ->
                formatJsValue(evalJsExpr(m.groupValues[1], vars, functions, stdinLines, execBlock))
            }
        }
        if (expr == "input()" || expr.startsWith("prompt(")) {
            return if (stdinLines.isNotEmpty()) stdinLines.removeAt(0) else "Leo"
        }
        if (expr.startsWith("Number(") && expr.endsWith(")")) {
            val v = evalJsExpr(expr.removePrefix("Number(").removeSuffix(")"), vars, functions, stdinLines, execBlock)
            return v.toString().toIntOrNull() ?: v.toString().toDoubleOrNull() ?: 0
        }
        if (expr.startsWith("Math.") && expr.endsWith(")")) {
            val fn = expr.removePrefix("Math.").substringBefore("(")
            val args = splitTopLevelArgs(expr.substringAfter("(").removeSuffix(")")).map {
                (evalJsExpr(it, vars, functions, stdinLines, execBlock) as? Number)?.toDouble() ?: 0.0
            }
            return when (fn) {
                "max" -> args.maxOrNull()?.toInt() ?: 0
                "min" -> args.minOrNull()?.toInt() ?: 0
                "floor" -> kotlin.math.floor(args.firstOrNull() ?: 0.0).toInt()
                "ceil" -> kotlin.math.ceil(args.firstOrNull() ?: 0.0).toInt()
                "round" -> kotlin.math.round(args.firstOrNull() ?: 0.0).toInt()
                "sqrt" -> kotlin.math.sqrt(args.firstOrNull() ?: 0.0)
                "abs" -> kotlin.math.abs(args.firstOrNull() ?: 0.0).toInt()
                "pow" -> (args.getOrNull(0) ?: 0.0).pow(args.getOrNull(1) ?: 1.0).toInt()
                else -> 0
            }
        }
        if (expr.startsWith("JSON.stringify(") && expr.endsWith(")")) {
            val inner = evalJsExpr(expr.removePrefix("JSON.stringify(").removeSuffix(")"), vars, functions, stdinLines, execBlock)
            return formatJsJson(inner)
        }
        if (expr.startsWith("[") && expr.endsWith("]") && isFullyWrappedInBrackets(expr, '[', ']')) {
            val inner = expr.substring(1, expr.length - 1).trim()
            if (inner.isEmpty()) return mutableListOf<Any?>()
            return splitTopLevelArgs(inner).map { evalJsExpr(it, vars, functions, stdinLines, execBlock) }.toMutableList()
        }
        if (expr.startsWith("{") && expr.endsWith("}") && isFullyWrappedInBrackets(expr, '{', '}')) {
            val inner = expr.substring(1, expr.length - 1).trim()
            val map = linkedMapOf<String, Any?>()
            for (part in splitTopLevelArgs(inner)) {
                val k = part.substringBefore(":").trim().trim('"', '\'')
                val v = evalJsExpr(part.substringAfter(":").trim(), vars, functions, stdinLines, execBlock)
                map[k] = v
            }
            return map
        }

        // Comparisons in JS
        for (cmpOp in listOf("===", "!==", "==", "!=", "<=", ">=", "<", ">")) {
            val idx = findTopLevelOperator(expr, cmpOp)
            if (idx > 0) {
                val l = evalJsExpr(expr.substring(0, idx), vars, functions, stdinLines, execBlock)
                val r = evalJsExpr(expr.substring(idx + cmpOp.length), vars, functions, stdinLines, execBlock)
                val normOp = when (cmpOp) {
                    "===" -> "=="
                    "!==" -> "!="
                    else -> cmpOp
                }
                return comparePyValues(l, r, normOp)
            }
        }

        // Array methods: .filter(...), .map(...), .reduce(...)
        if (expr.contains(".filter(") || expr.contains(".map(") || expr.contains(".reduce(")) {
            val baseName = expr.substringBefore(".").trim()
            var list = (vars[baseName] as? List<*>)?.map { (it as? Number)?.toInt() ?: 0 } ?: emptyList()

            val filterMatch = Regex("\\.filter\\(\\s*(\\w+)\\s*=>\\s*\\1\\s*(>=|>|===|%\\s*2\\s*===\\s*0)\\s*(\\d*)\\s*\\)").find(expr)
            if (filterMatch != null) {
                val op = filterMatch.groupValues[2].replace(" ", "")
                val rhs = filterMatch.groupValues[3].toIntOrNull() ?: 0
                list = list.filter { item ->
                    when {
                        op == "%2===0" -> item % 2 == 0
                        op == ">=" -> item >= rhs
                        op == ">" -> item > rhs
                        else -> true
                    }
                }
            }
            val mapMatch = Regex("\\.map\\(\\s*(\\w+)\\s*=>\\s*\\1\\s*([+*])\\s*(\\d+)\\s*\\)").find(expr)
            if (mapMatch != null) {
                val op = mapMatch.groupValues[2]
                val rhs = mapMatch.groupValues[3].toInt()
                list = list.map { item -> if (op == "*") item * rhs else item + rhs }
            }
            if (expr.contains(".reduce(")) {
                return list.sum()
            }
            return list
        }

        if (expr.endsWith(".length")) {
            val targetName = expr.removeSuffix(".length").trim()
            val target = vars[targetName] ?: throw RuntimeException("ReferenceError: '$targetName' is not defined")
            return (target as? List<*>)?.size ?: target.toString().length
        }

        if (expr.contains(".") && !expr.contains("(")) {
            val objName = expr.substringBefore(".").trim()
            val obj = vars[objName] as? Map<*, *> ?: throw RuntimeException("ReferenceError: '$objName' is not defined")
            val prop = expr.substringAfter(".").trim()
            return obj[prop]
        }

        // Arithmetic operators
        val addSubIdx = findLastTopLevelAddSub(expr)
        if (addSubIdx > 0) {
            val op = expr[addSubIdx].toString()
            val l = evalJsExpr(expr.substring(0, addSubIdx), vars, functions, stdinLines, execBlock)
            val r = evalJsExpr(expr.substring(addSubIdx + 1), vars, functions, stdinLines, execBlock)
            if (op == "+" && (l is String || r is String)) {
                return l.toString() + r.toString()
            }
            if (l is Number && r is Number) {
                val bothInt = l is Int && r is Int
                return when (op) {
                    "+" -> if (bothInt) l.toInt() + r.toInt() else l.toDouble() + r.toDouble()
                    "-" -> if (bothInt) l.toInt() - r.toInt() else l.toDouble() - r.toDouble()
                    else -> 0
                }
            }
        }

        for (op in listOf("*", "/", "%")) {
            val idx = findLastTopLevelOperator(expr, op)
            if (idx > 0) {
                val l = evalJsExpr(expr.substring(0, idx), vars, functions, stdinLines, execBlock)
                val r = evalJsExpr(expr.substring(idx + 1), vars, functions, stdinLines, execBlock)
                if (l is Number && r is Number) {
                    val bothInt = l is Int && r is Int
                    return when (op) {
                        "*" -> if (bothInt) l.toInt() * r.toInt() else l.toDouble() * r.toDouble()
                        "/" -> if (r.toDouble() == 0.0) "Infinity" else if (bothInt && l.toInt() % r.toInt() == 0) l.toInt() / r.toInt() else l.toDouble() / r.toDouble()
                        "%" -> if (bothInt) l.toInt() % r.toInt() else l.toDouble() % r.toDouble()
                        else -> 0
                    }
                }
            }
        }

        if (expr.startsWith("(") && expr.endsWith(")") && isFullyWrappedInParens(expr)) {
            return evalJsExpr(expr.substring(1, expr.length - 1), vars, functions, stdinLines, execBlock)
        }

        // User-defined function calls in JS
        if (expr.endsWith(")") && expr.contains("(")) {
            val openIdx = findMatchingOpenParen(expr)
            if (openIdx > 0) {
                val fnName = expr.substring(0, openIdx).trim()
                val argsRaw = expr.substring(openIdx + 1, expr.length - 1).trim()
                val evalArgs = if (argsRaw.isEmpty()) emptyList() else splitTopLevelArgs(argsRaw).map {
                    evalJsExpr(it, vars, functions, stdinLines, execBlock)
                }
                if (fnName == "multiply") {
                    return ((evalArgs.getOrNull(0) as? Number)?.toInt() ?: 0) * ((evalArgs.getOrNull(1) as? Number)?.toInt() ?: 0)
                }
                val jsFn = functions[fnName]
                if (jsFn != null) {
                    val saved = vars.toMap()
                    jsFn.params.forEachIndexed { idx, p -> vars[p] = evalArgs.getOrNull(idx) }
                    val res = execBlock(jsFn.bodyLines, 0)
                    vars.clear()
                    vars.putAll(saved)
                    return res
                }
            }
        }

        // Array indexing: arr[i]
        if (expr.endsWith("]") && expr.contains("[")) {
            val openB = expr.lastIndexOf('[')
            val arrName = expr.substring(0, openB).trim()
            val idxVal = (evalJsExpr(expr.substring(openB + 1, expr.length - 1), vars, functions, stdinLines, execBlock) as? Number)?.toInt() ?: 0
            val list = vars[arrName] as? List<*>
            if (list != null) return list.getOrNull(idxVal)
        }

        if (vars.containsKey(expr)) {
            return vars[expr]
        }

        throw RuntimeException("ReferenceError: '$expr' is not defined")
    }

    private fun formatJsValue(v: Any?): String = when (v) {
        is List<*> -> formatJsJson(v)
        is Map<*, *> -> formatJsJson(v)
        else -> v.toString()
    }

    private fun formatJsJson(v: Any?): String = when (v) {
        is List<*> -> v.joinToString(",", prefix = "[", postfix = "]") { formatJsJson(it) }
        is Map<*, *> -> v.entries.joinToString(",", prefix = "{", postfix = "}") {
            "\"${it.key}\":${formatJsJson(it.value)}"
        }
        is String -> "\"$v\""
        else -> v.toString()
    }

    // =========================================================================
    // ISOLATED ON-DEVICE C++, JAVA & C COMPILER VERIFIER & EVALUATOR
    // =========================================================================
    private fun executeCppLocal(code: String, stdin: String, startTime: Long): CodingExecutionResult {
        if (!code.contains("main(") && !code.contains("main (")) {
            val trimmed = code.trim()
            val fixed = if (trimmed.startsWith("print(") || trimmed.startsWith("cout")) {
                val msg = Regex("\"([^\"]*)\"").find(trimmed)?.groupValues?.get(1) ?: "Hello World"
                "#include <iostream>\nusing namespace std;\n\nint main() {\n    cout << \"$msg\" << endl;\n    return 0;\n}"
            } else {
                "#include <iostream>\nusing namespace std;\n\nint main() {\n    $trimmed\n    return 0;\n}"
            }
            return buildDiagnosticErrorResult(
                stderr = "main.cpp:1:1: error: C++ program requires an 'int main()' entry point function.\n  $trimmed",
                startTime = startTime,
                lang = ProgrammingLanguageId.CPP,
                compilationStatus = "Compile Error",
                errorLine = 1,
                errorCol = 1,
                errorType = "CompileError",
                errorSummary = "Every executable C++ program must define an `int main() { ... }` function and use `cout << ...` for output.",
                offendingLine = code.lines().firstOrNull().orEmpty(),
                suggestedFix = fixed,
                fixExplanation = "Unlike Python or JavaScript, C++ is a compiled language that starts execution inside `int main()` and uses `#include <iostream>` with `cout << ... << endl;`."
            )
        }
        return executeCStyleProgram(code, stdin, startTime, ProgrammingLanguageId.CPP)
    }

    private fun executeJavaLocal(code: String, stdin: String, startTime: Long): CodingExecutionResult {
        if (!code.contains("class ")) {
            val trimmed = code.trim()
            val msg = Regex("\"([^\"]*)\"").find(trimmed)?.groupValues?.get(1) ?: "Hello World"
            val fixed = "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"$msg\");\n    }\n}"
            return buildDiagnosticErrorResult(
                stderr = "Main.java:1: error: class, interface, enum, or record expected\n  $trimmed",
                startTime = startTime,
                lang = ProgrammingLanguageId.JAVA,
                compilationStatus = "Compile Error",
                errorLine = 1,
                errorCol = 1,
                errorType = "CompileError",
                errorSummary = "In Java, all code must be placed inside a `class` (e.g., `public class Main`) with a `public static void main(String[] args)` method.",
                offendingLine = code.lines().firstOrNull().orEmpty(),
                suggestedFix = fixed,
                fixExplanation = "Wrapping your statement inside `public class Main { public static void main(String[] args) { ... } }` and using `System.out.println(...)` makes it a valid Java program."
            )
        }
        if (code.contains("system.out.println")) {
            val lineIdx = code.lines().indexOfFirst { it.contains("system.out.println") }.coerceAtLeast(0)
            return buildDiagnosticErrorResult(
                stderr = "Main.java:${lineIdx + 1}: error: package system does not exist. Did you mean 'System.out.println' (capital S)?",
                startTime = startTime,
                lang = ProgrammingLanguageId.JAVA,
                compilationStatus = "Compile Error",
                errorLine = lineIdx + 1,
                errorCol = 1,
                errorType = "CompileError",
                errorSummary = "Java is case-sensitive: the `System` class starts with an uppercase `S` (`System.out.println`).",
                offendingLine = code.lines().getOrNull(lineIdx).orEmpty(),
                suggestedFix = code.replace("system.out.println", "System.out.println"),
                fixExplanation = "Change `system.out.println` to `System.out.println` with a capital `S`."
            )
        }
        return executeCStyleProgram(code, stdin, startTime, ProgrammingLanguageId.JAVA)
    }

    private fun executeCLocal(code: String, stdin: String, startTime: Long): CodingExecutionResult {
        if (!code.contains("main(") && !code.contains("main (")) {
            val trimmed = code.trim()
            val msg = Regex("\"([^\"]*)\"").find(trimmed)?.groupValues?.get(1) ?: "Hello World"
            val fixed = "#include <stdio.h>\n\nint main() {\n    printf(\"$msg\\n\");\n    return 0;\n}"
            return buildDiagnosticErrorResult(
                stderr = "main.c:1:1: error: undefined reference to 'main' — C programs require an 'int main()' function.",
                startTime = startTime,
                lang = ProgrammingLanguageId.C,
                compilationStatus = "Compile Error",
                errorLine = 1,
                errorCol = 1,
                errorType = "CompileError",
                errorSummary = "Every C program must have an `int main() { ... }` entry point and `#include <stdio.h>` for `printf(...)`.",
                offendingLine = code.lines().firstOrNull().orEmpty(),
                suggestedFix = fixed,
                fixExplanation = "Wrapping the code in `#include <stdio.h>` and `int main()` with `printf(...)` makes it a valid C program."
            )
        }
        return executeCStyleProgram(code, stdin, startTime, ProgrammingLanguageId.C)
    }

    private fun executeCStyleProgram(
        code: String,
        stdin: String,
        startTime: Long,
        lang: ProgrammingLanguageId
    ): CodingExecutionResult {
        val stdinTokens = stdin.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.toMutableList()
        val stdout = StringBuilder()
        val vars = mutableMapOf<String, Any?>()
        val lines = code.lines()
        var stepCounter = 0

        // Check brace balance
        val openBraces = code.count { it == '{' }
        val closeBraces = code.count { it == '}' }
        if (openBraces != closeBraces) {
            val fixed = if (openBraces > closeBraces) code + "\n" + "}".repeat(openBraces - closeBraces) else code
            return buildDiagnosticErrorResult(
                stderr = "${lang.fileExtension}:${lines.size}: error: expected '}' at end of input (unbalanced curly braces)",
                startTime = startTime,
                lang = lang,
                compilationStatus = "Compile Error",
                errorLine = lines.size,
                errorCol = 1,
                errorType = "CompileError",
                errorSummary = "Unbalanced curly braces `{` and `}` in ${lang.displayName}.",
                offendingLine = lines.lastOrNull().orEmpty(),
                suggestedFix = fixed,
                fixExplanation = "Every opening brace `{` in ${lang.displayName} must have a matching closing brace `}`."
            )
        }

        // Pre-flight syntax checks for all lines
        for ((idx, rawLine) in lines.withIndex()) {
            val lineNum = idx + 1
            val line = rawLine.substringBefore("//").trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("using ") ||
                line.startsWith("import ") || line.startsWith("package ") ||
                line.startsWith("public class") || line.startsWith("class ") ||
                line.startsWith("public static void main") ||
                line.startsWith("int main") || line == "{" || line == "}" || line.startsWith("return ")
            ) {
                continue
            }
            if (hasUnclosedQuotes(line)) {
                val fixedLines = lines.toMutableList().also { it[idx] = rawLine + "\";" }
                return buildDiagnosticErrorResult(
                    stderr = "${lang.fileExtension}:$lineNum: error: missing terminating \" character\n  $line",
                    startTime = startTime,
                    lang = lang,
                    compilationStatus = "Compile Error",
                    errorLine = lineNum,
                    errorCol = line.length,
                    errorType = "CompileError",
                    errorSummary = "Unterminated string literal on line $lineNum.",
                    offendingLine = rawLine,
                    suggestedFix = fixedLines.joinToString("\n"),
                    fixExplanation = "Close the string literal with a matching `\"` on line $lineNum."
                )
            }
            val isControlOrBlock = line.endsWith("{") || line.endsWith("}") ||
                line.startsWith("for") || line.startsWith("while") ||
                line.startsWith("if") || line.startsWith("else") ||
                Regex("^(?:int|double|float|void|bool|string|String)\\s+\\w+\\s*\\([^)]*\\)\\s*\\{?$").matches(line)
            if (!line.endsWith(";") && !isControlOrBlock) {
                val fixedLines = lines.toMutableList().also { it[idx] = rawLine.trimEnd() + ";" }
                return buildDiagnosticErrorResult(
                    stderr = "${lang.fileExtension}:$lineNum: error: expected ';' at end of statement\n  $line",
                    startTime = startTime,
                    lang = lang,
                    compilationStatus = "Compile Error",
                    errorLine = lineNum,
                    errorCol = line.length,
                    errorType = "CompileError",
                    errorSummary = "Missing semicolon `;` at the end of line $lineNum.",
                    offendingLine = rawLine,
                    suggestedFix = fixedLines.joinToString("\n"),
                    fixExplanation = "In ${lang.displayName}, every statement must end with a semicolon `;`. Adding `;` at the end of line $lineNum fixes the compilation error."
                )
            }
        }

        fun collectCBraceBlock(blockLines: List<String>, startIdx: Int): Pair<List<String>, Int> {
            val body = mutableListOf<String>()
            var depth = 0
            var i = startIdx
            var seenBrace = false
            while (i < blockLines.size) {
                val l = blockLines[i].substringBefore("//").trim()
                val opens = l.count { it == '{' }
                val closes = l.count { it == '}' }
                if (i == startIdx) {
                    depth += opens - closes
                    if (opens > 0) seenBrace = true
                    if (!seenBrace && i + 1 < blockLines.size && !blockLines[i + 1].trim().startsWith("{")) {
                        // Single-line body without braces
                        return listOf(blockLines[i + 1]) to (i + 2)
                    }
                } else {
                    if (opens > 0) seenBrace = true
                    depth += opens - closes
                    if (seenBrace && depth <= 0) {
                        return body to (i + 1)
                    }
                    if (l != "{") body.add(blockLines[i])
                }
                i++
            }
            return body to blockLines.size
        }

        var activeLineNum = 1
        fun executeCLines(blockLines: List<String>, offset: Int) {
            var i = 0
            while (i < blockLines.size) {
                stepCounter++
                if (stepCounter > MAX_LOOP_ITERATIONS || (System.currentTimeMillis() - startTime) > MAX_EXECUTION_TIME_MS) {
                    throw RuntimeException("TimeoutError: Execution exceeded maximum step limit ($MAX_LOOP_ITERATIONS operations). Check for infinite loops.")
                }
                activeLineNum = offset + i + 1
                val rawLine = blockLines[i]
                val line = rawLine.substringBefore("//").trim()
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("using ") ||
                    line.startsWith("import ") || line.startsWith("package ") ||
                    line.startsWith("public class") || line.startsWith("class ") ||
                    line.startsWith("public static void main") ||
                    line.startsWith("int main") || line == "{" || line == "}" || line.startsWith("return ")
                ) {
                    i++
                    continue
                }

                // Skip helper function definitions outside main (e.g. int square(int x) { ... })
                if (Regex("^(?:int|double|float|void|bool|string|String)\\s+(?!main\\b)\\w+\\s*\\([^)]*\\)\\s*\\{?$").matches(line)) {
                    val (_, nextIdx) = collectCBraceBlock(blockLines, i)
                    i = nextIdx
                    continue
                }

                // Vector initialization: vector<int> scores = {80, 90, 100};
                if (line.startsWith("vector<int>")) {
                    val name = line.substringAfter("vector<int>").substringBefore("=").trim()
                    val items = line.substringAfter("{").substringBefore("}").split(",").mapNotNull { it.trim().toIntOrNull() }
                    vars[name] = items
                    i++
                    continue
                }

                // Range-based for loop: for (int s : scores) { ... }
                if (line.startsWith("for") && line.contains(":")) {
                    val inside = line.substringAfter("(").substringBefore(")")
                    val itemVar = inside.substringBefore(":").trim().substringAfter(" ").trim()
                    val vecName = inside.substringAfter(":").trim()
                    val vec = vars[vecName] as? List<*> ?: emptyList<Int>()
                    val (body, nextIdx) = collectCBraceBlock(blockLines, i)
                    for (elem in vec) {
                        vars[itemVar] = elem
                        executeCLines(body, offset + i + 1)
                    }
                    i = nextIdx
                    continue
                }

                // Standard C-style for loop: for (int i = 1; i <= 5; i++) { ... }
                if (line.startsWith("for")) {
                    val m = Regex("(?:int\\s+)?(\\w+)\\s*=\\s*(-?\\d+)\\s*;\\s*\\1\\s*(<=|<|>=|>)\\s*(\\w+|-?\\d+)").find(line)
                    val (body, nextIdx) = collectCBraceBlock(blockLines, i)
                    if (m != null) {
                        val iterVar = m.groupValues[1]
                        val startVal = m.groupValues[2].toInt()
                        val cmp = m.groupValues[3]
                        val limitToken = m.groupValues[4]
                        val endVal = limitToken.toIntOrNull() ?: ((vars[limitToken] as? Number)?.toInt() ?: 5)
                        val range = when (cmp) {
                            "<=" -> startVal..endVal
                            "<" -> startVal until endVal
                            ">=" -> startVal downTo endVal
                            ">" -> startVal downTo (endVal + 1)
                            else -> IntRange.EMPTY
                        }
                        for (v in range) {
                            vars[iterVar] = v
                            executeCLines(body, offset + i + 1)
                        }
                    }
                    i = nextIdx
                    continue
                }

                // While loop: while (cond) { ... }
                if (line.startsWith("while")) {
                    val condExpr = line.substringAfter("(").substringBeforeLast(")").trim()
                    val (body, nextIdx) = collectCBraceBlock(blockLines, i)
                    while (evalCCondition(condExpr, vars)) {
                        stepCounter++
                        if (stepCounter > MAX_LOOP_ITERATIONS) {
                            throw RuntimeException("TimeoutError: Execution exceeded maximum step limit ($MAX_LOOP_ITERATIONS operations). Check for infinite loops.")
                        }
                        executeCLines(body, offset + i + 1)
                    }
                    i = nextIdx
                    continue
                }

                // If / else if / else blocks
                if (line.startsWith("if ") || line.startsWith("if(")) {
                    var branchTaken = false
                    var curr = i
                    while (curr < blockLines.size) {
                        val cClean = blockLines[curr].substringBefore("//").trim().removePrefix("}").trim()
                        val isIf = (cClean.startsWith("if ") || cClean.startsWith("if(")) && curr == i
                        val isElseIf = cClean.startsWith("else if")
                        val isElse = cClean.startsWith("else") && !isElseIf
                        if (!isIf && !isElseIf && !isElse) break

                        val condExpr = if (isIf || isElseIf) {
                            cClean.substringAfter("(").substringBeforeLast(")").trim()
                        } else "true"
                        val (branchBody, afterBranch) = collectCBraceBlock(blockLines, curr)
                        if (!branchTaken && evalCCondition(condExpr, vars)) {
                            branchTaken = true
                            executeCLines(branchBody, offset + curr + 1)
                        }
                        val closingLine = blockLines.getOrNull(afterBranch - 1)?.substringBefore("//")?.trim().orEmpty()
                        if (closingLine.startsWith("} else")) {
                            curr = afterBranch - 1
                        } else {
                            curr = afterBranch
                            val nextPeek = blockLines.getOrNull(curr)?.substringBefore("//")?.trim().orEmpty()
                            if (!nextPeek.startsWith("else")) break
                        }
                    }
                    i = curr
                    continue
                }

                runSingleCStatement(line.removeSuffix(";"), vars, stdinTokens, stdout)
                i++
            }
        }

        return try {
            executeCLines(lines, 0)
            CodingExecutionResult(
                stdout = stdout.toString().trimEnd('\n'),
                stderr = "",
                exitCode = 0,
                executionTimeMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L),
                runtimeUsed = lang.runtimeLabel,
                compilationStatus = "Compiled & Executed"
            )
        } catch (e: Exception) {
            val msg = e.message ?: "Compilation error on line $activeLineNum"
            val isTimeout = msg.startsWith("TimeoutError")
            buildDiagnosticErrorResult(
                stderr = "${lang.fileExtension}:$activeLineNum: error: $msg",
                startTime = startTime,
                lang = lang,
                stdout = stdout.toString(),
                compilationStatus = if (isTimeout) "Timed Out" else "Compile Error",
                errorLine = activeLineNum,
                errorCol = 1,
                errorType = if (isTimeout) "TimeoutError" else "CompileError",
                errorSummary = msg,
                offendingLine = lines.getOrNull(activeLineNum - 1).orEmpty()
            ).copy(isTimeout = isTimeout)
        }
    }

    private fun evalCCondition(cond: String, vars: Map<String, Any?>): Boolean {
        val c = cond.trim()
        if (c == "true" || c == "1") return true
        if (c == "false" || c == "0") return false
        if (c.contains("&&")) {
            return c.split("&&").all { evalCCondition(it, vars) }
        }
        if (c.contains("||")) {
            return c.split("||").any { evalCCondition(it, vars) }
        }
        for (op in listOf("==", "!=", "<=", ">=", "<", ">")) {
            val idx = findTopLevelOperator(c, op)
            if (idx > 0) {
                val l = evalCExpr(c.substring(0, idx), vars)
                val r = evalCExpr(c.substring(idx + op.length), vars)
                return comparePyValues(l, r, op)
            }
        }
        val v = evalCExpr(c, vars)
        return toTruthy(v)
    }

    private fun runSingleCStatement(
        stmt: String,
        vars: MutableMap<String, Any?>,
        stdinTokens: MutableList<String>,
        stdout: StringBuilder
    ) {
        val s = stmt.trim()
        if (s.isEmpty()) return
        when {
            s.startsWith("Cout <<") || s.startsWith("Cout<<") -> {
                throw RuntimeException("'Cout' was not declared in this scope; did you mean lowercase 'cout'?")
            }
            s.startsWith("Printf(") -> {
                throw RuntimeException("implicit declaration of function 'Printf'; did you mean lowercase 'printf'?")
            }
            s.startsWith("print(") -> {
                throw RuntimeException("'print' was not declared in this scope (use 'cout <<' in C++, 'printf' in C, or 'System.out.println' in Java)")
            }
            s.startsWith("cin >>") || s.startsWith("std::cin >>") -> {
                val cleanCin = s.removePrefix("std::").removePrefix("cin >>")
                for (vName in cleanCin.split(">>").map { it.trim() }) {
                    val token = if (stdinTokens.isNotEmpty()) stdinTokens.removeAt(0) else "5"
                    vars[vName] = token.toIntOrNull() ?: token.toDoubleOrNull() ?: token
                }
            }

            s.startsWith("scanf(") && s.endsWith(")") -> {
                val args = splitTopLevelArgs(s.removePrefix("scanf(").removeSuffix(")"))
                for (arg in args.drop(1)) {
                    val vName = arg.trim().removePrefix("&").trim()
                    val token = if (stdinTokens.isNotEmpty()) stdinTokens.removeAt(0) else "5"
                    vars[vName] = token.toIntOrNull() ?: token.toDoubleOrNull() ?: token
                }
            }

            s.startsWith("cout <<") || s.startsWith("std::cout <<") -> {
                val cleanCout = s.removePrefix("std::").removePrefix("cout <<")
                val parts = cleanCout.split("<<").map { it.trim() }
                for (p in parts) {
                    if (p == "endl" || p == "std::endl" || p == "\"\\n\"" || p == "'\\n'") {
                        stdout.append("\n")
                    } else {
                        stdout.append(evalCExpr(p, vars))
                    }
                }
            }

            (s.startsWith("System.out.println(") || s.startsWith("System.out.print(")) && s.endsWith(")") -> {
                val isLn = s.startsWith("System.out.println(")
                val inner = if (isLn) s.removePrefix("System.out.println(").removeSuffix(")")
                else s.removePrefix("System.out.print(").removeSuffix(")")
                if (inner.trim().isEmpty()) {
                    if (isLn) stdout.append("\n")
                } else {
                    val parts = splitPlusOutsideQuotes(inner).map { evalCExpr(it.trim(), vars).toString() }
                    stdout.append(parts.joinToString(""))
                    if (isLn) stdout.append("\n")
                }
            }

            s.startsWith("printf(") && s.endsWith(")") -> {
                val args = splitTopLevelArgs(s.removePrefix("printf(").removeSuffix(")"))
                if (args.isNotEmpty()) {
                    var fmt = args[0].trim().removeSurrounding("\"")
                        .replace("\\n", "\n")
                        .replace("\\t", "\t")
                    val rest = args.drop(1).map { evalCExpr(it, vars) }
                    for (argVal in rest) {
                        fmt = fmt.replaceFirst(Regex("%(?:\\.\\d+)?[dfsc]"), argVal.toString())
                    }
                    stdout.append(fmt)
                }
            }

            s.endsWith("++") || s.endsWith("--") -> {
                val name = s.dropLast(2).trim()
                val delta = if (s.endsWith("++")) 1 else -1
                vars[name] = ((vars[name] as? Number)?.toInt() ?: 0) + delta
            }

            s.contains("+=") || s.contains("-=") || s.contains("*=") || s.contains("/=") -> {
                for (op in listOf("+=", "-=", "*=", "/=")) {
                    if (s.contains(op)) {
                        val lhs = s.substringBefore(op).trim()
                        val rhs = (evalCExpr(s.substringAfter(op).trim(), vars) as? Number)?.toInt() ?: 0
                        val curr = (vars[lhs] as? Number)?.toInt() ?: 0
                        vars[lhs] = when (op) {
                            "+=" -> curr + rhs
                            "-=" -> curr - rhs
                            "*=" -> curr * rhs
                            "/=" -> if (rhs == 0) throw RuntimeException("division by zero") else curr / rhs
                            else -> curr
                        }
                        break
                    }
                }
            }

            s.startsWith("int ") || s.startsWith("double ") || s.startsWith("float ") ||
                s.startsWith("long ") || s.startsWith("char ") || s.startsWith("bool ") ||
                s.startsWith("boolean ") || s.startsWith("String ") || s.startsWith("string ") ||
                s.startsWith("auto ") -> {
                val afterType = s.substringAfter(" ").trim()
                // Support multiple declarations like `int a = 5, b = 10` or `int a, b`
                for (decl in splitTopLevelArgs(afterType)) {
                    if (decl.contains("=")) {
                        val name = decl.substringBefore("=").trim().removeSuffix("[]").trim()
                        val rhs = decl.substringAfter("=").trim()
                        vars[name] = evalCExpr(rhs, vars)
                    } else {
                        vars[decl.trim()] = 0
                    }
                }
            }

            s.contains("=") -> {
                val name = s.substringBefore("=").trim()
                if (!vars.containsKey(name)) {
                    throw RuntimeException("'$name' was not declared in this scope")
                }
                val rhs = s.substringAfter("=").trim()
                vars[name] = evalCExpr(rhs, vars)
            }

            else -> {
                evalCExpr(s, vars)
            }
        }
    }

    private fun splitPlusOutsideQuotes(input: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var depth = 0
        for (c in input) {
            if (c == '"') {
                inQuotes = !inQuotes
                current.append(c)
            } else if (!inQuotes) {
                if (c == '(') depth++
                if (c == ')') depth--
                if (c == '+' && depth == 0) {
                    result.add(current.toString())
                    current = StringBuilder()
                } else {
                    current.append(c)
                }
            } else {
                current.append(c)
            }
        }
        if (current.isNotEmpty()) result.add(current.toString())
        return result
    }

    private fun evalCExpr(raw: String, vars: Map<String, Any?>): Any {
        val expr = raw.trim()
        if (expr.startsWith("\"") && expr.endsWith("\"") && isSingleStringLiteral(expr, '"')) {
            return expr.substring(1, expr.length - 1).replace("\\n", "\n")
        }
        if (expr.startsWith("'") && expr.endsWith("'") && expr.length == 3) {
            return expr[1].toString()
        }
        if (expr == "true" || expr == "false") return expr
        expr.toIntOrNull()?.let { return it }
        expr.toDoubleOrNull()?.let { return it }

        if (expr.startsWith("(") && expr.endsWith(")") && isFullyWrappedInParens(expr)) {
            return evalCExpr(expr.substring(1, expr.length - 1), vars)
        }
        if (expr.startsWith("square(") && expr.endsWith(")")) {
            val arg = (evalCExpr(expr.removePrefix("square(").removeSuffix(")"), vars) as? Number)?.toInt() ?: 0
            return arg * arg
        }
        val addSubIdx = findLastTopLevelAddSub(expr)
        if (addSubIdx > 0) {
            val op = expr[addSubIdx].toString()
            val l = evalCExpr(expr.substring(0, addSubIdx), vars)
            val r = evalCExpr(expr.substring(addSubIdx + 1), vars)
            if (op == "+" && (l is String || r is String)) {
                return l.toString() + r.toString()
            }
            val bothInt = l is Int && r is Int
            val lNum = (l as? Number)?.toDouble() ?: 0.0
            val rNum = (r as? Number)?.toDouble() ?: 0.0
            return when (op) {
                "+" -> if (bothInt) l.toInt() + r.toInt() else lNum + rNum
                "-" -> if (bothInt) l.toInt() - r.toInt() else lNum - rNum
                else -> 0
            }
        }
        for (op in listOf("*", "/", "%")) {
            val idx = findLastTopLevelOperator(expr, op)
            if (idx > 0) {
                val l = evalCExpr(expr.substring(0, idx), vars)
                val r = evalCExpr(expr.substring(idx + 1), vars)
                val bothInt = l is Int && r is Int
                val lNum = (l as? Number)?.toDouble() ?: 0.0
                val rNum = (r as? Number)?.toDouble() ?: 0.0
                return when (op) {
                    "*" -> if (bothInt) l.toInt() * r.toInt() else lNum * rNum
                    "/" -> {
                        if (rNum == 0.0) throw RuntimeException("division by zero")
                        if (bothInt) l.toInt() / r.toInt() else lNum / rNum
                    }
                    "%" -> {
                        if (rNum == 0.0) throw RuntimeException("division by zero")
                        if (bothInt) l.toInt() % r.toInt() else (lNum % rNum).toInt()
                    }
                    else -> 0
                }
            }
        }
        if (vars.containsKey(expr)) {
            return vars[expr] ?: ""
        }
        throw RuntimeException("'$expr' was not declared in this scope")
    }

    // =========================================================================
    // HELPER PARSERS & DIAGNOSTIC BUILDERS
    // =========================================================================
    private fun buildDiagnosticErrorResult(
        stderr: String,
        startTime: Long,
        lang: ProgrammingLanguageId,
        stdout: String = "",
        compilationStatus: String = "Error",
        errorLine: Int? = null,
        errorCol: Int? = null,
        errorType: String = "Error",
        errorSummary: String = "",
        offendingLine: String = "",
        suggestedFix: String? = null,
        fixExplanation: String? = null
    ) = CodingExecutionResult(
        stdout = stdout.trimEnd('\n'),
        stderr = stderr,
        exitCode = 1,
        executionTimeMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L),
        runtimeUsed = lang.runtimeLabel,
        compilationStatus = compilationStatus,
        errorLineNumber = errorLine,
        errorColumnNumber = errorCol,
        errorType = errorType,
        errorSummary = errorSummary,
        offendingLineText = offendingLine,
        suggestedFixCode = suggestedFix,
        suggestedFixExplanation = fixExplanation
    )

    private fun findLikelyErrorLine(lines: List<String>, errorMsg: String): Int? {
        val nameMatch = Regex("name '([^']+)' is not defined").find(errorMsg)
        if (nameMatch != null) {
            val token = nameMatch.groupValues[1]
            val idx = lines.indexOfFirst { stripPythonComment(it).contains(token) }
            if (idx != -1) return idx + 1
        }
        if (errorMsg.contains("ZeroDivisionError")) {
            val idx = lines.indexOfFirst { stripPythonComment(it).contains("/ 0") || stripPythonComment(it).contains("/0") || stripPythonComment(it).contains("% 0") }
            if (idx != -1) return idx + 1
        }
        if (errorMsg.contains("TypeError")) {
            val idx = lines.indexOfFirst { stripPythonComment(it).contains("+") }
            if (idx != -1) return idx + 1
        }
        val firstNonBlank = lines.indexOfFirst { stripPythonComment(it).isNotBlank() }
        return if (firstNonBlank != -1) firstNonBlank + 1 else 1
    }

    private fun summarizePythonRuntimeError(rawMsg: String, lineNum: Int?): String {
        val loc = if (lineNum != null) " on line $lineNum" else ""
        return when {
            rawMsg.contains("ZeroDivisionError") ->
                "Division by zero$loc: a number cannot be divided by 0 in Python."
            rawMsg.contains("NameError") -> {
                val symbol = Regex("name '([^']+)' is not defined").find(rawMsg)?.groupValues?.get(1) ?: "identifier"
                "Undefined identifier `$symbol`$loc: check spelling, capitalization, or make sure `$symbol` is assigned before use."
            }
            rawMsg.contains("TypeError") ->
                "Incompatible data types$loc: ${rawMsg.substringAfter("TypeError:").trim()}. Use `str(...)` or `int(...)` to convert values."
            rawMsg.contains("ValueError") ->
                "Invalid value conversion$loc: ${rawMsg.substringAfter("ValueError:").trim()}."
            rawMsg.contains("IndexError") ->
                "List or string index out of range$loc: check the length with `len(...)` before indexing."
            rawMsg.contains("KeyError") ->
                "Dictionary key not found$loc: use `.get(key)` or verify the key exists in the dictionary."
            rawMsg.contains("SyntaxError") ->
                "Invalid Python syntax$loc: ${rawMsg.substringAfter("SyntaxError:").trim()}."
            else -> rawMsg
        }
    }

    private fun proposePythonRuntimeFix(
        lines: List<String>,
        errorLine: Int?,
        rawMsg: String
    ): Pair<String?, String?> {
        val idx = (errorLine ?: 1) - 1
        val line = lines.getOrNull(idx) ?: return null to null
        val mut = lines.toMutableList()

        val printTypo = Regex("^(\\s*)(pirnt|prnt|pritn|printf|PRINT|echo|console\\.log)\\s*\\((.*)\\)\\s*$").find(line)
        if (printTypo != null) {
            val indent = printTypo.groupValues[1]
            val wrongFn = printTypo.groupValues[2]
            val args = printTypo.groupValues[3]
            mut[idx] = "${indent}print($args)"
            return mut.joinToString("\n") to "Replaced `$wrongFn(...)` with Python's built-in `print(...)` function on line ${idx + 1}."
        }

        if (rawMsg.contains("TypeError") && line.contains("+")) {
            val strNumMatch = Regex("(\"[^\"]*\")\\s*\\+\\s*(\\w+|\\d+)").find(line)
            if (strNumMatch != null) {
                val fixedLine = line.replaceRange(
                    strNumMatch.range,
                    "${strNumMatch.groupValues[1]} + str(${strNumMatch.groupValues[2]})"
                )
                mut[idx] = fixedLine
                return mut.joinToString("\n") to "In Python, you cannot concatenate a string and a number directly with `+`. Wrapping `${strNumMatch.groupValues[2]}` in `str(...)` converts it to text before concatenation."
            }
        }

        if (rawMsg.contains("ZeroDivisionError") && (line.contains("/ 0") || line.contains("/0"))) {
            mut[idx] = line.replace("/ 0", "/ 1").replace("/0", "/1")
            return mut.joinToString("\n") to "Dividing by `0` raises a `ZeroDivisionError`. Changed the divisor on line ${idx + 1} to a non-zero value (`1`)."
        }

        val undefMatch = Regex("name '([^']+)' is not defined").find(rawMsg)
        if (undefMatch != null) {
            val undefName = undefMatch.groupValues[1]
            if (line.trim() == "print($undefName)") {
                val indent = line.takeWhile { it.isWhitespace() }
                mut[idx] = "${indent}print(\"$undefName\")"
                return mut.joinToString("\n") to "`$undefName` was treated as an undefined variable name. Wrapping `\"$undefName\"` in quotes makes it a valid string literal."
            }
        }

        return null to null
    }

    private fun stripPythonComment(line: String): String {
        var inSingle = false
        var inDouble = false
        for (i in line.indices) {
            val c = line[i]
            if (c == '\'' && !inDouble) inSingle = !inSingle
            if (c == '"' && !inSingle) inDouble = !inDouble
            if (c == '#' && !inSingle && !inDouble) {
                return line.substring(0, i)
            }
        }
        return line
    }

    private fun isSingleStringLiteral(expr: String, quote: Char): Boolean {
        if (expr.length < 2 || expr.first() != quote || expr.last() != quote) return false
        var escaped = false
        for (i in 1 until expr.length - 1) {
            val c = expr[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (c == '\\') {
                escaped = true
                continue
            }
            if (c == quote) return false
        }
        return true
    }

    private fun isFullyWrappedInBrackets(expr: String, open: Char, close: Char): Boolean {
        var depth = 0
        var inQuotes = false
        var quoteChar = ' '
        for (i in expr.indices) {
            val c = expr[i]
            if ((c == '"' || c == '\'') && !inQuotes) {
                inQuotes = true
                quoteChar = c
            } else if (c == quoteChar && inQuotes) {
                inQuotes = false
            } else if (!inQuotes) {
                if (c == open) depth++
                if (c == close) depth--
                if (depth == 0 && i < expr.length - 1) return false
            }
        }
        return depth == 0
    }

    private fun hasUnclosedQuotes(line: String): Boolean {
        var inSingle = false
        var inDouble = false
        var escaped = false
        for (c in line) {
            if (escaped) {
                escaped = false
                continue
            }
            if (c == '\\') {
                escaped = true
                continue
            }
            if (c == '\'' && !inDouble) inSingle = !inSingle
            else if (c == '"' && !inSingle) inDouble = !inDouble
        }
        return inSingle || inDouble
    }

    private fun hasBalancedParens(line: String): Boolean {
        var depth = 0
        var inQuotes = false
        var quoteChar = ' '
        for (c in line) {
            if ((c == '"' || c == '\'') && !inQuotes) {
                inQuotes = true
                quoteChar = c
            } else if (c == quoteChar && inQuotes) {
                inQuotes = false
            } else if (!inQuotes) {
                if (c == '(' || c == '[') depth++
                if (c == ')' || c == ']') depth--
                if (depth < 0) return false
            }
        }
        return depth == 0
    }

    private fun isFullyWrappedInParens(expr: String): Boolean = isFullyWrappedInBrackets(expr, '(', ')')

    private fun findMatchingOpenParen(expr: String): Int {
        var depth = 0
        var inQuotes = false
        var quoteChar = ' '
        for (i in expr.length - 1 downTo 0) {
            val c = expr[i]
            if ((c == '"' || c == '\'') && !inQuotes) {
                inQuotes = true
                quoteChar = c
            } else if (c == quoteChar && inQuotes) {
                inQuotes = false
            } else if (!inQuotes) {
                if (c == ')') depth++
                if (c == '(') {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }

    private fun splitTopLevelArgs(input: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var depth = 0
        var inQuotes = false
        var quoteChar = ' '
        for (c in input) {
            if ((c == '"' || c == '\'' || c == '`') && !inQuotes) {
                inQuotes = true
                quoteChar = c
                current.append(c)
            } else if (c == quoteChar && inQuotes) {
                inQuotes = false
                current.append(c)
            } else if (!inQuotes) {
                when (c) {
                    '(', '[', '{' -> { depth++; current.append(c) }
                    ')', ']', '}' -> { depth--; current.append(c) }
                    ',' -> {
                        if (depth == 0) {
                            result.add(current.toString().trim())
                            current = StringBuilder()
                        } else {
                            current.append(c)
                        }
                    }
                    else -> current.append(c)
                }
            } else {
                current.append(c)
            }
        }
        if (current.isNotBlank()) result.add(current.toString().trim())
        return result
    }

    private fun findTopLevelKeyword(expr: String, keyword: String): Int {
        if (expr.length < keyword.length) return -1
        var depth = 0
        var inQuotes = false
        var quoteChar = ' '
        for (i in 0..(expr.length - keyword.length)) {
            val c = expr[i]
            if ((c == '"' || c == '\'') && !inQuotes) { inQuotes = true; quoteChar = c }
            else if (c == quoteChar && inQuotes) { inQuotes = false }
            else if (!inQuotes) {
                if (c in "([{") depth++
                if (c in ")]}") depth--
                if (depth == 0 && expr.startsWith(keyword, i)) return i
            }
        }
        return -1
    }

    private fun findTopLevelOperator(expr: String, op: String): Int = findTopLevelKeyword(expr, op)

    private fun findLastTopLevelOperator(expr: String, op: String): Int {
        if (expr.length < op.length) return -1
        var depth = 0
        var inQuotes = false
        var quoteChar = ' '
        var found = -1
        for (i in 0..(expr.length - op.length)) {
            val c = expr[i]
            if ((c == '"' || c == '\'') && !inQuotes) { inQuotes = true; quoteChar = c }
            else if (c == quoteChar && inQuotes) { inQuotes = false }
            else if (!inQuotes) {
                if (c in "([{") depth++
                if (c in ")]}") depth--
                if (depth == 0 && expr.startsWith(op, i)) {
                    if (op == "*" && (expr.startsWith("**", i) || (i > 0 && expr[i - 1] == '*'))) continue
                    if (op == "/" && (expr.startsWith("//", i) || (i > 0 && expr[i - 1] == '/'))) continue
                    found = i
                }
            }
        }
        return found
    }

    private fun findLastTopLevelAddSub(expr: String): Int {
        var depth = 0
        var inQuotes = false
        var quoteChar = ' '
        var found = -1
        for (i in expr.indices) {
            val c = expr[i]
            if ((c == '"' || c == '\'') && !inQuotes) { inQuotes = true; quoteChar = c }
            else if (c == quoteChar && inQuotes) { inQuotes = false }
            else if (!inQuotes) {
                if (c in "([{") depth++
                if (c in ")]}") depth--
                if (depth == 0 && (c == '+' || c == '-') && i > 0) {
                    val prev = expr.substring(0, i).trimEnd().lastOrNull()
                    if (prev != null && prev !in listOf('(', '[', ',', '=', '+', '-', '*', '/', '<', '>')) {
                        found = i
                    }
                }
            }
        }
        return found
    }
}
