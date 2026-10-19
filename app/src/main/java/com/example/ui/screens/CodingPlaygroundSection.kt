package com.example.ui.screens

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.coding.AiCodeAction
import com.example.data.coding.AiCodeAssistantResponse
import com.example.data.coding.CodeSnippetTemplate
import com.example.data.coding.CodingCurriculumCatalog
import com.example.data.coding.CodingExecutionResult
import com.example.data.coding.ProgrammingLanguageId
import com.example.ui.theme.CosmicIndigo
import com.example.ui.theme.CosmicViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SolarFlareAmber
import com.example.ui.theme.StarlightGold
import com.example.ui.theme.SupernovaCoral
import com.example.ui.theme.TurquoiseNebula

class CodeSyntaxHighlighter : VisualTransformation {
    private val keywordRegex = Regex(
        "\\b(def|return|if|elif|else|for|while|in|break|continue|pass|import|from|as|class|try|except|finally|assert|yield|and|or|not|True|False|None|const|let|var|function|async|await|int|double|float|char|bool|void|string|String|public|private|static|using|namespace|std|include|vector|printf|scanf|cout|cin|endl|main|new)\\b"
    )
    private val builtinRegex = Regex(
        "\\b(print|input|range|len|sum|min|max|abs|int|float|str|sorted|reversed|round|pow|type|bool|list|console|log|JSON|stringify|System|out|println)\\b"
    )
    private val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
    private val stringRegex = Regex("(\"[^\"\\n]*\"|'[^'\\n]*'|`[^`\\n]*`)")
    private val commentRegex = Regex("(#.*|//.*|<!--[\\s\\S]*?-->)")

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val annotated = buildAnnotatedString {
            append(raw)

            for (m in numberRegex.findAll(raw)) {
                addStyle(SpanStyle(color = Color(0xFFF59E0B)), m.range.first, m.range.last + 1)
            }
            for (m in builtinRegex.findAll(raw)) {
                addStyle(SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold), m.range.first, m.range.last + 1)
            }
            for (m in keywordRegex.findAll(raw)) {
                addStyle(SpanStyle(color = Color(0xFFC084FC), fontWeight = FontWeight.Bold), m.range.first, m.range.last + 1)
            }
            for (m in stringRegex.findAll(raw)) {
                addStyle(SpanStyle(color = Color(0xFF34D399)), m.range.first, m.range.last + 1)
            }
            for (m in commentRegex.findAll(raw)) {
                addStyle(SpanStyle(color = Color(0xFF94A3B8)), m.range.first, m.range.last + 1)
            }
        }
        return TransformedText(annotated, OffsetMapping.Identity)
    }
}

private fun getQuickKeywordsForLanguage(language: ProgrammingLanguageId): List<Pair<String, String>> {
    return when (language) {
        ProgrammingLanguageId.PYTHON -> listOf(
            "print()" to "print(\"\")",
            "input()" to "input(\"Enter value: \")",
            "def" to "def my_func(x):\n    return x\n",
            "for range" to "for i in range(5):\n    print(i)\n",
            "if / else" to "if x > 0:\n    print(\"Positive\")\nelse:\n    print(\"Non-positive\")\n",
            "while" to "while count < 5:\n    count += 1\n",
            "list []" to "items = [1, 2, 3, 4]\n",
            "dict {}" to "data = {\"name\": \"Falcon\", \"score\": 100}\n",
            "f-string" to "f\"Result: {x}\""
        )
        ProgrammingLanguageId.JAVASCRIPT -> listOf(
            "console.log" to "console.log(\"\");",
            "const" to "const value = 10;",
            "let" to "let count = 0;",
            "function" to "function solve(n) {\n    return n * 2;\n}",
            "arrow =>" to "const calc = (x) => x * 2;",
            "for loop" to "for (let i = 0; i < 5; i++) {\n    console.log(i);\n}",
            "if / else" to "if (score >= 50) {\n    console.log(\"Pass\");\n}"
        )
        ProgrammingLanguageId.CPP -> listOf(
            "cout <<" to "cout << \"Hello\" << endl;",
            "cin >>" to "cin >> x;",
            "for loop" to "for (int i = 0; i < 5; i++) {\n    cout << i << endl;\n}",
            "vector<int>" to "vector<int> nums = {1, 2, 3};",
            "if / else" to "if (x > 0) {\n    cout << \"Positive\" << endl;\n}",
            "#include" to "#include <iostream>\nusing namespace std;\n"
        )
        ProgrammingLanguageId.JAVA -> listOf(
            "println()" to "System.out.println(\"\");",
            "for loop" to "for (int i = 0; i < 5; i++) {\n    System.out.println(i);\n}",
            "String" to "String message = \"Falcon\";",
            "int[]" to "int[] nums = {10, 20, 30};",
            "if / else" to "if (x > 0) {\n    System.out.println(\"Positive\");\n}"
        )
        ProgrammingLanguageId.C -> listOf(
            "printf()" to "printf(\"%d\\n\", x);",
            "scanf()" to "scanf(\"%d\", &x);",
            "for loop" to "for (int i = 0; i < 5; i++) {\n    printf(\"%d\\n\", i);\n}",
            "if / else" to "if (x > 0) {\n    printf(\"Positive\\n\");\n}"
        )
        ProgrammingLanguageId.HTML_CSS -> listOf(
            "<h1>" to "<h1>Falcon Web Heading</h1>\n",
            "<p>" to "<p>Clean responsive paragraph text.</p>\n",
            "<button>" to "<button style=\"padding:10px 16px;background:#00F5D4;border:none;border-radius:8px;\">Click Me</button>\n",
            "<div> card" to "<div class=\"card\">\n  <h2>Card Title</h2>\n</div>\n",
            "<style>" to "<style>\n  body { font-family: sans-serif; background: #0F172A; color: #F8FAFC; padding: 16px; }\n</style>\n"
        )
    }
}

private fun codeNeedsInteractiveStdin(code: String): Boolean {
    return code.contains("input(") ||
        code.contains("cin >>") ||
        code.contains("scanf(") ||
        code.contains("Scanner(") ||
        code.contains("readLine(")
}

@Composable
fun ProfessionalCodePlayground(
    selectedLanguage: ProgrammingLanguageId,
    code: String,
    stdin: String,
    isExecuting: Boolean,
    executionResult: CodingExecutionResult?,
    isAiLoading: Boolean,
    aiResponse: AiCodeAssistantResponse?,
    onCodeChanged: (String) -> Unit,
    onStdinChanged: (String) -> Unit,
    onRunCode: () -> Unit,
    onResetStarterCode: () -> Unit,
    onRequestAiAction: (AiCodeAction, ProgrammingLanguageId, String?) -> Unit,
    onApplySuggestedCode: (String, ProgrammingLanguageId?) -> Unit,
    onShowMessage: (String) -> Unit,
    onSelectLanguage: ((ProgrammingLanguageId) -> Unit)? = null,
    onClearConsole: (() -> Unit)? = null,
    onDismissAiResponse: (() -> Unit)? = null
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val syntaxHighlighter = remember { CodeSyntaxHighlighter() }
    val snippets: List<CodeSnippetTemplate> = remember(selectedLanguage) {
        CodingCurriculumCatalog.getSnippetsForLanguage(selectedLanguage)
    }
    val quickKeywords = remember(selectedLanguage) {
        getQuickKeywordsForLanguage(selectedLanguage)
    }

    var showStdinPanel by remember {
        mutableStateOf(codeNeedsInteractiveStdin(code))
    }
    var showSnippetsMenu by remember { mutableStateOf(false) }
    var pendingExampleCode by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selfFixMode by remember(executionResult) { mutableStateOf(false) }
    var targetConvertLang by remember(selectedLanguage) {
        mutableStateOf(
            if (selectedLanguage == ProgrammingLanguageId.PYTHON) ProgrammingLanguageId.JAVASCRIPT
            else ProgrammingLanguageId.PYTHON
        )
    }
    var customAiQuestion by remember { mutableStateOf("") }

    // Confirmation dialog before overwriting existing user code when loading an example
    if (pendingExampleCode != null) {
        AlertDialog(
            onDismissRequest = { pendingExampleCode = null },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFE2E8F0),
            title = {
                Text(
                    text = "Replace Existing Code?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Your editor already contains ${selectedLanguage.displayName} code. Loading an example will replace your current code. Do you want to continue?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val (newCode, newStdin) = pendingExampleCode!!
                        onCodeChanged(newCode)
                        if (newStdin.isNotEmpty()) {
                            onStdinChanged(newStdin)
                            showStdinPanel = true
                        }
                        pendingExampleCode = null
                        showSnippetsMenu = false
                        onShowMessage("Loaded ${selectedLanguage.displayName} example")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldSuccess,
                        contentColor = Color(0xFF052E16)
                    ),
                    modifier = Modifier.testTag("confirm_load_example_button")
                ) {
                    Text("Load Example", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingExampleCode = null },
                    modifier = Modifier.testTag("cancel_load_example_button")
                ) {
                    Text("Keep My Code", color = TurquoiseNebula, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    val requestLoadExample: (String, String) -> Unit = { sampleCode, sampleStdin ->
        if (code.isNotBlank()) {
            pendingExampleCode = sampleCode to sampleStdin
        } else {
            onCodeChanged(sampleCode)
            if (sampleStdin.isNotEmpty()) {
                onStdinChanged(sampleStdin)
                showStdinPanel = true
            }
            showSnippetsMenu = false
            onShowMessage("Loaded ${selectedLanguage.displayName} example")
        }
    }

    val triggerExecute: () -> Unit = {
        if (!isExecuting) {
            keyboardController?.hide()
            selfFixMode = false
            if (codeNeedsInteractiveStdin(code)) {
                showStdinPanel = true
            }
            onRunCode()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("coding_playground_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 0. FALCON CODE — TOP INSTANT EXECUTE COMMAND HEADER
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(colors = listOf(EmeraldSuccess, TurquoiseNebula, CosmicViolet))
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF0B132B), Color(0xFF161F42), Color(0xFF0F172A))
                        )
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldSuccess.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, EmeraldSuccess),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Falcon Code",
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Falcon Code",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = TurquoiseNebula.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "INSTANT IDE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TurquoiseNebula,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Write & execute ${selectedLanguage.displayName} directly — no tutorial required",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Prominent Top EXECUTE Button
                    Button(
                        onClick = triggerExecute,
                        enabled = !isExecuting,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            contentColor = Color(0xFF052E16)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("top_execute_code_button")
                    ) {
                        if (isExecuting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF052E16)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Running...", fontWeight = FontWeight.ExtraBold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Execute Code",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Execute Code", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                // Instant Language Switcher Strip inside Falcon Code
                if (onSelectLanguage != null) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(ProgrammingLanguageId.entries) { lang ->
                            val isSelected = lang == selectedLanguage
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(lang.accentHex).copy(alpha = 0.3f) else Color(0xFF1E293B),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) TurquoiseNebula else Color(0xFF334155)
                                ),
                                modifier = Modifier
                                    .clickable { onSelectLanguage(lang) }
                                    .testTag("falcon_code_lang_${lang.key}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = lang.shortBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) TurquoiseNebula else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = lang.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. IDE Editor Card (File tab, Load Example, Quick Keywords, Symbol Bar, Line Numbers & Editor)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(
                1.dp,
                if (executionResult != null && !executionResult.isSuccess && executionResult.errorLineNumber != null) {
                    SupernovaCoral.copy(alpha = 0.85f)
                } else {
                    Color(0xFF334155)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(selectedLanguage.accentHex).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(selectedLanguage.accentHex))
                        ) {
                            Text(
                                text = selectedLanguage.fileExtension,
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedLanguage.runtimeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Optional "Load Example" button (never overwrites without confirmation)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TurquoiseNebula.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, TurquoiseNebula.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clickable {
                                    val starter = CodingCurriculumCatalog.getStarterCode(selectedLanguage)
                                    requestLoadExample(starter, "")
                                }
                                .testTag("load_example_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "Load Example",
                                    tint = TurquoiseNebula,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Load Example",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TurquoiseNebula,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(code))
                                onShowMessage("Code copied to clipboard")
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("editor_copy_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Code",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                onCodeChanged("")
                                onClearConsole?.invoke()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("editor_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear Editor",
                                tint = SupernovaCoral,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Row A: Quick Symbol & Indentation Bar for Mobile Coding
                val quickSymbols = listOf("    ", "(", ")", "\"", "'", ":", ";", "=", "+", "-", "*", "/", "[", "]", "{", "}", "<", ">", "#")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF162032))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CosmicViolet.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, CosmicViolet.copy(alpha = 0.6f)),
                        modifier = Modifier.clickable { showSnippetsMenu = !showSnippetsMenu }
                    ) {
                        Text(
                            text = if (showSnippetsMenu) "Hide Examples" else "Examples ▾",
                            style = MaterialTheme.typography.labelSmall,
                            color = TurquoiseNebula,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    quickSymbols.forEach { sym ->
                        val label = if (sym == "    ") "Tab ⇥" else sym
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.clickable {
                                onCodeChanged(code + sym)
                            }
                        ) {
                            Text(
                                text = label,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Row B: 1-Tap Language Keyword Inserts (print(), input(), def, for, etc.)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111827))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Insert:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                    quickKeywords.forEach { (chipLabel, snippetText) ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, TurquoiseNebula.copy(alpha = 0.35f)),
                            modifier = Modifier.clickable {
                                val separator = if (code.isEmpty() || code.endsWith("\n") || code.endsWith(" ")) "" else "\n"
                                onCodeChanged(code + separator + snippetText)
                            }
                        ) {
                            Text(
                                text = chipLabel,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TurquoiseNebula,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Example Templates Picker (respects confirmation dialog if user already has code)
                AnimatedVisibility(visible = showSnippetsMenu) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Select a ${selectedLanguage.displayName} Example Program:",
                            style = MaterialTheme.typography.labelSmall,
                            color = StarlightGold
                        )
                        snippets.forEach { snippet ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        requestLoadExample(snippet.code, snippet.sampleStdin)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = snippet.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = snippet.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }

                // Line Numbers + Syntax-Highlighted Editable Code Canvas (with Error Line Highlight)
                val lineCount = remember(code) { code.lines().size.coerceAtLeast(1) }
                val errorLineNum = if (executionResult != null && !executionResult.isSuccess) {
                    executionResult.errorLineNumber
                } else {
                    null
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 165.dp, max = 320.dp)
                        .padding(vertical = 10.dp)
                ) {
                    // Line Numbers Gutter with Error Line Highlight
                    Column(
                        modifier = Modifier
                            .width(42.dp)
                            .padding(start = 6.dp, end = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        for (i in 1..lineCount) {
                            val isErrorLine = errorLineNum == i
                            Text(
                                text = if (isErrorLine) "!$i" else "$i",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = if (isErrorLine) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isErrorLine) SupernovaCoral else Color(0xFF64748B)
                                )
                            )
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height((lineCount * 20).coerceAtLeast(145).dp)
                            .background(if (errorLineNum != null) SupernovaCoral.copy(alpha = 0.5f) else Color(0xFF334155))
                    )

                    // Editable Code Input
                    BasicTextField(
                        value = code,
                        onValueChange = onCodeChanged,
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFFF8FAFC)
                        ),
                        cursorBrush = SolidColor(TurquoiseNebula),
                        visualTransformation = syntaxHighlighter,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                            .testTag("code_editor_input"),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (code.isEmpty()) {
                                    Text(
                                        text = "Write your ${selectedLanguage.displayName} code here, then tap Execute Code...\n(Or tap 'Load Example' above to insert a sample program)",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                // Intelligent Error Detection Banner directly below editor when a line/syntax error is detected
                if (executionResult != null && !executionResult.isSuccess &&
                    (executionResult.errorLineNumber != null || executionResult.errorExplanation.isNotBlank())
                ) {
                    Surface(
                        color = SupernovaCoral.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, SupernovaCoral.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("intelligent_error_banner"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = SupernovaCoral,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val lineBadge = executionResult.errorLineNumber?.let { "Line $it • " } ?: ""
                                    Text(
                                        text = "${lineBadge}${executionResult.errorType.ifBlank { "Error Detected" }}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SupernovaCoral,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            if (executionResult.errorLineContent.isNotBlank()) {
                                Text(
                                    text = "> ${executionResult.errorLineContent}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFECACA)
                                )
                            }
                            if (executionResult.errorExplanation.isNotBlank()) {
                                Text(
                                    text = executionResult.errorExplanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFF8FAFC)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF334155))

                // Bottom Action Bar: Stdin Toggle + Execute Code Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showStdinPanel = !showStdinPanel },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (showStdinPanel) TurquoiseNebula else Color(0xFF475569)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("toggle_stdin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Program Input",
                            tint = TurquoiseNebula,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (stdin.isNotBlank()) "Console Input (Set)" else "Console Input (stdin)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = triggerExecute,
                        enabled = !isExecuting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            contentColor = Color(0xFF052E16)
                        ),
                        modifier = Modifier.testTag("run_code_button")
                    ) {
                        if (isExecuting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF052E16)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Executing...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Execute Code",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Execute Code", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Standard Input (stdin) Drawer
                AnimatedVisibility(visible = showStdinPanel || codeNeedsInteractiveStdin(code)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Interactive Console Input (stdin) — Enter one input value per line for input() / cin / scanf:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TurquoiseNebula
                        )
                        OutlinedTextField(
                            value = stdin,
                            onValueChange = onStdinChanged,
                            placeholder = {
                                Text(
                                    "Example:\nFalcon\n42",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stdin_input_field")
                        )
                    }
                }
            }
        }

        // 2. REAL OUTPUT CONSOLE (stdout, stderr, compilation status, execution status & duration)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
            border = BorderStroke(
                1.5.dp,
                when {
                    isExecuting -> SolarFlareAmber
                    executionResult == null -> Color(0xFF334155)
                    executionResult.isSuccess -> EmeraldSuccess.copy(alpha = 0.8f)
                    else -> SupernovaCoral.copy(alpha = 0.85f)
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("code_output_console")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = TurquoiseNebula,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Output Console (${selectedLanguage.fileExtension})",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            if (executionResult != null && executionResult.compilationStatus.isNotBlank()) {
                                Text(
                                    text = "Status: ${executionResult.compilationStatus}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Execution Status Badge
                        val statusText = when {
                            isExecuting -> "RUNNING..."
                            executionResult == null -> "READY"
                            executionResult.isTimeout -> "TIMEOUT"
                            executionResult.isSuccess -> "SUCCESS • ${executionResult.executionTimeMs}ms"
                            else -> "ERROR (Exit ${executionResult.exitCode})"
                        }
                        val statusColor = when {
                            isExecuting -> SolarFlareAmber
                            executionResult == null -> Color(0xFF94A3B8)
                            executionResult.isSuccess -> EmeraldSuccess
                            else -> SupernovaCoral
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = statusColor.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, statusColor)
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }

                        if (executionResult != null && onClearConsole != null) {
                            IconButton(
                                onClick = onClearConsole,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("clear_console_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Clear Console",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (isExecuting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = EmeraldSuccess
                        )
                        Text(
                            text = "Executing ${selectedLanguage.displayName} code...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                } else if (executionResult == null) {
                    Text(
                        text = "$ falcon-run ${selectedLanguage.fileExtension}\nWrite your ${selectedLanguage.displayName} code in the editor above and tap 'Execute Code' to view real program output here.",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )
                } else {
                    // Sandboxed HTML/CSS Preview when language is HTML_CSS
                    if (executionResult.isHtmlPreview && executionResult.htmlContent.isNotBlank()) {
                        Text(
                            text = "Sandboxed HTML/CSS Live Preview:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TurquoiseNebula
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = false
                                        settings.allowFileAccess = false
                                        settings.allowContentAccess = false
                                    }
                                },
                                update = { webView ->
                                    webView.loadDataWithBaseURL(
                                        null,
                                        executionResult.htmlContent,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            )
                        }
                    }

                    // Standard Output (stdout)
                    if (executionResult.stdout.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF111827),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Standard Output (stdout):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = EmeraldSuccess
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = executionResult.stdout,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = Color(0xFFF8FAFC),
                                    modifier = Modifier.testTag("stdout_text")
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "[Program finished in ${executionResult.executionTimeMs}ms]",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    } else if (executionResult.isSuccess) {
                        Text(
                            text = "[Program finished with no console output in ${executionResult.executionTimeMs}ms]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.testTag("stdout_text")
                        )
                    }

                    // Standard Error (stderr)
                    if (executionResult.stderr.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SupernovaCoral.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, SupernovaCoral),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = SupernovaCoral,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (executionResult.compilationStatus == "Empty Editor") {
                                                "Notice:"
                                            } else {
                                                "Compiler / Runtime Error (stderr):"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SupernovaCoral,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = executionResult.stderr,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFECACA),
                                    modifier = Modifier.testTag("stderr_text")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2B. DEDICATED ERROR-RECOVERY PANEL: "Fix it with Falcon AI" or "I'll fix it myself"
        if (executionResult != null && !executionResult.isSuccess && !executionResult.isEmptyCodeWarning && code.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161F38)),
                border = BorderStroke(1.5.dp, if (selfFixMode) TurquoiseNebula else CosmicViolet),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("error_recovery_panel")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selfFixMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Self-Fix Mode Active",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TurquoiseNebula,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                val lineHint = executionResult.errorLineNumber?.let { "Check line $it in the editor above" }
                                    ?: "Edit your code in the editor above"
                                Text(
                                    text = "$lineHint, then tap 'Execute Code' to test your fix.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                            TextButton(
                                onClick = {
                                    selfFixMode = false
                                    onRequestAiAction(AiCodeAction.FIX_CODE, targetConvertLang, null)
                                },
                                modifier = Modifier.testTag("fix_with_falcon_ai_button")
                            ) {
                                Text("Fix it with Falcon AI", color = CosmicViolet, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Need Help Fixing This Error?",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Choose how you'd like to proceed. Falcon AI can explain the error and suggest a correction without modifying your code automatically, or you can fix it yourself.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    onRequestAiAction(AiCodeAction.FIX_CODE, targetConvertLang, null)
                                },
                                enabled = !isAiLoading,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CosmicViolet,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("fix_with_falcon_ai_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Fix it with Falcon AI",
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    selfFixMode = true
                                    onDismissAiResponse?.invoke()
                                    onShowMessage("Got it! Edit the highlighted line and tap Execute Code when ready.")
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, TurquoiseNebula),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("fix_it_myself_button")
                            ) {
                                Text(
                                    text = "I'll fix it myself",
                                    color = TurquoiseNebula,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2C. BEFORE-AND-AFTER CODE COMPARISON CARD (Shown when Falcon AI proposes a fix)
        if (aiResponse != null && !aiResponse.suggestedCode.isNullOrBlank() && aiResponse.originalCode.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.5.dp, EmeraldSuccess.copy(alpha = 0.85f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_fix_comparison_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = aiResponse.actionTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // 1. Original Code (Before)
                    Text(
                        text = "Original Code (Before):",
                        style = MaterialTheme.typography.labelMedium,
                        color = SupernovaCoral,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E1B2E),
                        border = BorderStroke(1.dp, SupernovaCoral.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            aiResponse.originalCode.lines().forEachIndexed { idx, line ->
                                val lineNum = idx + 1
                                val isChanged = aiResponse.changedLineNumbers.contains(lineNum)
                                Text(
                                    text = "$lineNum | $line",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = if (isChanged) Color(0xFFFECACA) else Color(0xFF94A3B8),
                                    fontWeight = if (isChanged) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // 2. Corrected Code (After)
                    Text(
                        text = "Corrected Code (After):",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0A221E),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.65f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            aiResponse.suggestedCode.lines().forEachIndexed { idx, line ->
                                val lineNum = idx + 1
                                val isChanged = aiResponse.changedLineNumbers.contains(lineNum)
                                Text(
                                    text = "$lineNum | $line",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = if (isChanged) EmeraldSuccess else Color(0xFFE2E8F0),
                                    fontWeight = if (isChanged) FontWeight.Bold else FontWeight.Normal,
                                    modifier = if (idx == 0) Modifier.testTag("corrected_code_preview") else Modifier
                                )
                            }
                        }
                    }

                    // 3. What Changed
                    if (aiResponse.whatChangedBullets.isNotEmpty()) {
                        Text(
                            text = "What Changed:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TurquoiseNebula,
                            fontWeight = FontWeight.Bold
                        )
                        aiResponse.whatChangedBullets.forEach { bullet ->
                            Text(
                                text = "• $bullet",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }

                    // 4. Why It Failed
                    if (aiResponse.whyItFailed.isNotBlank()) {
                        Text(
                            text = "Why It Failed:",
                            style = MaterialTheme.typography.labelMedium,
                            color = SolarFlareAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = aiResponse.whyItFailed,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                    }

                    // 5. Tip to Remember Next Time
                    if (aiResponse.tipToRemember.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CosmicViolet.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, CosmicViolet.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = StarlightGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tip to Remember Next Time: ${aiResponse.tipToRemember}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Two mandatory user decision actions: "Apply Fix to Editor" and "Keep My Code"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onApplySuggestedCode(aiResponse.suggestedCode, aiResponse.targetLanguage)
                                onDismissAiResponse?.invoke()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldSuccess,
                                contentColor = Color(0xFF052E16)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("apply_fix_to_editor_button")
                        ) {
                            Text("Apply Fix to Editor", fontWeight = FontWeight.ExtraBold)
                        }

                        OutlinedButton(
                            onClick = {
                                onDismissAiResponse?.invoke()
                                onShowMessage("Kept your current code in the editor")
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF64748B)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("keep_my_code_button")
                        ) {
                            Text("Keep My Code", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. AI-Powered Code Assistant Panel
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CosmicViolet.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_code_assistant_panel")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = CosmicViolet.copy(alpha = 0.18f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CosmicViolet,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Falcon AI Code Assistant",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Contextual help for your ${selectedLanguage.displayName} code & output",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 10 Contextual AI Actions Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(AiCodeAction.entries) { action ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                onRequestAiAction(action, targetConvertLang, null)
                            },
                            label = { Text(action.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CosmicViolet.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.testTag("ai_action_${action.name.lowercase()}")
                        )
                    }
                }

                // Target Language Selector for Code Conversion
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Convert target:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ProgrammingLanguageId.entries.filter { it != selectedLanguage }.forEach { lang ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (targetConvertLang == lang) CosmicViolet else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                targetConvertLang = lang
                                onRequestAiAction(AiCodeAction.CONVERT_LANGUAGE, lang, null)
                            }
                        ) {
                            Text(
                                text = "→ ${lang.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (targetConvertLang == lang) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Ask Falcon AI Custom Coding Question or "I Don't Understand"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customAiQuestion,
                        onValueChange = { customAiQuestion = it },
                        placeholder = { Text("Ask Falcon AI or say 'I don't understand'...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("coding_ai_question_input")
                    )
                    IconButton(
                        onClick = {
                            if (customAiQuestion.isNotBlank()) {
                                onRequestAiAction(AiCodeAction.EXPLAIN_CONCEPT, targetConvertLang, customAiQuestion)
                                customAiQuestion = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(CosmicViolet, CircleShape)
                            .testTag("coding_ai_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Ask Falcon AI",
                            tint = Color.White
                        )
                    }
                }

                if (isAiLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Falcon AI is analyzing your ${selectedLanguage.displayName} code...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (aiResponse != null && aiResponse.originalCode.isBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CosmicIndigo.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, CosmicViolet.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_code_response_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = aiResponse.actionTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = CosmicViolet,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = aiResponse.explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp
                            )

                            if (!aiResponse.suggestedCode.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = aiResponse.suggestedCode,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = Color(0xFF34D399),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                                Button(
                                    onClick = {
                                        onApplySuggestedCode(aiResponse.suggestedCode, aiResponse.targetLanguage)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CosmicViolet),
                                    modifier = Modifier.testTag("apply_suggested_code_button")
                                ) {
                                    Text(
                                        text = if (aiResponse.targetLanguage != null) {
                                            "Switch to ${aiResponse.targetLanguage.displayName} & Load Code"
                                        } else {
                                            "Apply Suggested Code to Editor"
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
