package com.example.data.coding

object CodingCurriculumCatalog {

    fun getLessonsForLanguage(lang: ProgrammingLanguageId): List<CodingLesson> = when (lang) {
        ProgrammingLanguageId.PYTHON -> PythonCurriculumData.lessons
        ProgrammingLanguageId.JAVASCRIPT -> javascriptLessons
        ProgrammingLanguageId.CPP -> cppLessons
        ProgrammingLanguageId.JAVA -> javaLessons
        ProgrammingLanguageId.C -> cLessons
        ProgrammingLanguageId.HTML_CSS -> htmlCssLessons
    }

    fun getStarterCode(lang: ProgrammingLanguageId): String = when (lang) {
        ProgrammingLanguageId.PYTHON -> """
            # Falcon AI Python Playground
            def greet_scholar(name, streak):
                print("Welcome,", name)
                print("Active Streak:", streak, "days")

            greet_scholar("Falcon Coder", 5)
            squares = [n * n for n in range(1, 5)]
            print("Squares:", squares)
        """.trimIndent()

        ProgrammingLanguageId.JAVASCRIPT -> """
            // Falcon AI JavaScript Playground (ES2024)
            const scholar = "Falcon Coder";
            const scores = [85, 92, 98];
            const total = scores.reduce((acc, val) => acc + val, 0);

            console.log("Scholar:", scholar);
            console.log("Average Score:", total / scores.length);
        """.trimIndent()

        ProgrammingLanguageId.CPP -> """
            // Falcon AI C++20 Playground
            #include <iostream>
            #include <vector>
            using namespace std;

            int main() {
                cout << "Falcon AI C++ Engine Ready!" << endl;
                int sum = 0;
                for (int i = 1; i <= 5; i++) {
                    sum += i;
                }
                cout << "Sum 1..5 = " << sum << endl;
                return 0;
            }
        """.trimIndent()

        ProgrammingLanguageId.JAVA -> """
            // Falcon AI Java Playground
            public class Main {
                public static void main(String[] args) {
                    String title = "Falcon AI Java Runtime";
                    int lessons = 10;
                    System.out.println(title);
                    System.out.println("Target Lessons: " + lessons);
                }
            }
        """.trimIndent()

        ProgrammingLanguageId.C -> """
            // Falcon AI C17 Playground
            #include <stdio.h>

            int main() {
                int coins = 150;
                int bonus = 50;
                printf("Falcon AI C Compiler\n");
                printf("Total Coins: %d\n", coins + bonus);
                return 0;
            }
        """.trimIndent()

        ProgrammingLanguageId.HTML_CSS -> """
            <!DOCTYPE html>
            <html>
            <head>
              <style>
                body {
                  font-family: sans-serif;
                  background: #0F172A;
                  color: #F8FAFC;
                  padding: 20px;
                }
                .card {
                  background: linear-gradient(135deg, #4F46E5, #06B6D4);
                  padding: 18px;
                  border-radius: 16px;
                  box-shadow: 0 8px 20px rgba(0,0,0,0.35);
                }
                h2 { margin-top: 0; }
                .badge {
                  display: inline-block;
                  background: rgba(255,255,255,0.2);
                  padding: 4px 10px;
                  border-radius: 99px;
                  font-size: 12px;
                }
              </style>
            </head>
            <body>
              <div class="card">
                <span class="badge">Live Web Sandbox</span>
                <h2>Falcon AI Web Studio</h2>
                <p>Edit this HTML &amp; CSS and tap Run to render your responsive card!</p>
              </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun getSnippetsForLanguage(lang: ProgrammingLanguageId): List<CodeSnippetTemplate> = when (lang) {
        ProgrammingLanguageId.PYTHON -> listOf(
            CodeSnippetTemplate(
                title = "Hello World & Variables",
                description = "Basic output and variable math",
                code = "print(\"Hello World\")\ncoins = 120\nbonus = 30\nprint(\"Total Coins:\", coins + bonus)"
            ),
            CodeSnippetTemplate(
                title = "Interactive input() Reader",
                description = "Reads name and number from Standard Input",
                code = "name = input()\nnum = int(input())\nprint(\"Hello\", name)\nprint(\"Squared:\", num * num)",
                sampleStdin = "Aria\n12"
            ),
            CodeSnippetTemplate(
                title = "Fibonacci Sequence Function",
                description = "Computes Fibonacci numbers using a loop",
                code = "def fib(n):\n    a = 0\n    b = 1\n    for i in range(n):\n        print(a)\n        nxt = a + b\n        a = b\n        b = nxt\n\nfib(6)"
            )
        )

        ProgrammingLanguageId.JAVASCRIPT -> listOf(
            CodeSnippetTemplate(
                title = "Console Output & Arrow Functions",
                description = "Modern ES6 const, template literals, and arrow functions",
                code = "const multiply = (a, b) => a * b;\nconsole.log(\"Hello World\");\nconsole.log(\"6 x 7 =\", multiply(6, 7));"
            ),
            CodeSnippetTemplate(
                title = "Array filter() and map()",
                description = "Functional array transformations in JavaScript",
                code = "const numbers = [1, 2, 3, 4, 5, 6];\nconst evensDoubled = numbers.filter(n => n % 2 === 0).map(n => n * 10);\nconsole.log(\"Result:\", JSON.stringify(evensDoubled));"
            ),
            CodeSnippetTemplate(
                title = "Reading Standard Input in JS",
                description = "Uses input() helper to read lines from stdin",
                code = "const user = input();\nconst score = Number(input());\nconsole.log(`Scholar: ${'$'}{user}, Bonus: ${'$'}{score + 50}`);",
                sampleStdin = "Leo\n200"
            )
        )

        ProgrammingLanguageId.CPP -> listOf(
            CodeSnippetTemplate(
                title = "Hello World & iostream",
                description = "Standard C++20 console output",
                code = "#include <iostream>\nusing namespace std;\n\nint main() {\n    cout << \"Hello World\" << endl;\n    return 0;\n}"
            ),
            CodeSnippetTemplate(
                title = "Reading cin Standard Input",
                description = "Reads integer from cin and computes factorial",
                code = "#include <iostream>\nusing namespace std;\n\nint main() {\n    int n = 5;\n    cin >> n;\n    int fact = 1;\n    for (int i = 1; i <= n; i++) {\n        fact = fact * i;\n    }\n    cout << \"Factorial: \" << fact << endl;\n    return 0;\n}",
                sampleStdin = "5"
            )
        )

        ProgrammingLanguageId.JAVA -> listOf(
            CodeSnippetTemplate(
                title = "Java Main Class & Loops",
                description = "Standard Java entry point and loop calculation",
                code = "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello World\");\n        int total = 0;\n        for (int i = 1; i <= 4; i++) {\n            total += i * 10;\n        }\n        System.out.println(\"Total: \" + total);\n    }\n}"
            )
        )

        ProgrammingLanguageId.C -> listOf(
            CodeSnippetTemplate(
                title = "C printf & Pointers Basics",
                description = "Formatted output in C17",
                code = "#include <stdio.h>\n\nint main() {\n    printf(\"Hello World\\n\");\n    int x = 42;\n    printf(\"Value: %d\\n\", x);\n    return 0;\n}"
            )
        )

        ProgrammingLanguageId.HTML_CSS -> listOf(
            CodeSnippetTemplate(
                title = "Modern Flexbox Dashboard Card",
                description = "Responsive card with gradient badge",
                code = getStarterCode(ProgrammingLanguageId.HTML_CSS)
            )
        )
    }

    // JAVASCRIPT CURRICULUM (Language-Specific)
    private val javascriptLessons: List<CodingLesson> = listOf(
        CodingLesson(
            id = "js_01",
            languageId = ProgrammingLanguageId.JAVASCRIPT,
            moduleNumber = 1,
            moduleTitle = "Module 1: Modern JavaScript Foundations (ES6+)",
            lessonNumber = 1,
            title = "1. How JavaScript Powers the Web & console.log()",
            difficulty = "Beginner",
            topicTag = "JS Basics",
            conceptExplanation = "JavaScript is the programming language built into every web browser and modern server runtime (Node.js). It makes web pages interactive and processes data dynamically. Use console.log() to print values to the developer console.",
            whyItExists = "HTML builds a webpage's skeleton and CSS paints it, but JavaScript gives it a brain—handling clicks, animations, live updates, and logic.",
            simplerExplanation = "If a webpage is a car, HTML is the metal frame, CSS is the paint job, and JavaScript is the engine under the hood.",
            syntaxGuide = "// Single-line comment\nconsole.log(\"Hello World\");\nconsole.log(25 + 75);",
            workingExampleCode = "// First JavaScript program\nconsole.log(\"Welcome to Falcon AI JavaScript!\");\nconsole.log(\"Engine Status:\", 200);",
            lineByLineWalkthrough = listOf(
                "Line 1: A comment starting with //.",
                "Line 2: Calls console.log() to output text.",
                "Line 3: Logs a label and the number 200."
            ),
            expectedOutput = "Welcome to Falcon AI JavaScript!\nEngine Status: 200",
            commonMistakes = listOf(
                "Writing Console.log() with a capital C — JavaScript is case-sensitive and requires lowercase 'console'."
            ),
            realWorldExample = "Logging API responses and user events in browser DevTools.",
            exercisePrompt = "Use console.log() to display exactly: Hello World",
            exerciseStarterCode = "// Log Hello World to the console\n",
            exerciseSolutionCode = "console.log(\"Hello World\");",
            exerciseExpectedOutputContains = listOf("Hello World"),
            exerciseHint = "Write console.log(\"Hello World\");",
            quizQuestion = "Which function outputs a message to the JavaScript console?",
            quizOptions = listOf("console.log()", "print.out()", "System.log()", "echo()"),
            quizCorrectIndex = 0,
            quizWrongExplanation = "JavaScript uses the global 'console' object and its .log() method.",
            quizHint = "It starts with 'console.'."
        ),
        CodingLesson(
            id = "js_02",
            languageId = ProgrammingLanguageId.JAVASCRIPT,
            moduleNumber = 1,
            moduleTitle = "Module 1: Modern JavaScript Foundations (ES6+)",
            lessonNumber = 2,
            title = "2. Variables with let and const & Template Literals",
            difficulty = "Beginner",
            topicTag = "Variables",
            conceptExplanation = "Modern JavaScript uses 'const' for values that won't be reassigned and 'let' for variables that change (avoid legacy 'var'). Backtick template literals (`Hello ${'$'}{name}`) embed variables directly inside strings.",
            whyItExists = "Using 'const' by default prevents accidental variable overwrites, making code safer and easier to reason about.",
            simplerExplanation = "'const' is a permanent marker label; 'let' is a whiteboard label you can erase and update.",
            syntaxGuide = "const appName = \"Falcon AI\";\nlet coins = 100;\ncoins += 50;\nconsole.log(`${'$'}{appName} Coins: ${'$'}{coins}`);",
            workingExampleCode = "const language = \"JavaScript\";\nlet level = 1;\nlevel = level + 1;\nconsole.log(`Learning ${'$'}{language} at Level ${'$'}{level}`);",
            lineByLineWalkthrough = listOf(
                "Line 1: Declares a constant 'language'.",
                "Lines 2-3: Declares mutable 'level' with let and increments it to 2.",
                "Line 4: Uses backticks (`) and ${'$'}{...} interpolation."
            ),
            expectedOutput = "Learning JavaScript at Level 2",
            commonMistakes = listOf(
                "Trying to reassign a 'const' variable (throws TypeError: Assignment to constant variable).",
                "Using normal single quotes '...' instead of backticks `...` for ${'$'}{variable} interpolation."
            ),
            realWorldExample = "Rendering dynamic user greetings and live score counters in web apps.",
            exercisePrompt = "Declare const score = 95 and log it using console.log(score).",
            exerciseStarterCode = "const score = 95;\nconsole.log(score);",
            exerciseSolutionCode = "const score = 95;\nconsole.log(score);",
            exerciseExpectedOutputContains = listOf("95"),
            exerciseHint = "Run the code to output 95.",
            quizQuestion = "When should you use 'const' vs 'let' in modern JavaScript?",
            quizOptions = listOf(
                "Use 'const' by default when a variable won't be reassigned, and 'let' when it needs to change",
                "Use 'let' for constants and 'const' for loops",
                "They are completely identical",
                "'const' only works for numbers"
            ),
            quizCorrectIndex = 0,
            quizWrongExplanation = "'const' prevents reassignment and signals intent clearly; 'let' is for variables that get updated.",
            quizHint = "const stands for constant reference."
        ),
        CodingLesson(
            id = "js_03",
            languageId = ProgrammingLanguageId.JAVASCRIPT,
            moduleNumber = 1,
            moduleTitle = "Module 1: Modern JavaScript Foundations (ES6+)",
            lessonNumber = 3,
            title = "3. Strict Equality (===), Conditionals & Loops",
            difficulty = "Beginner",
            topicTag = "Control Flow",
            conceptExplanation = "Always use strict equality (=== and !==) in JavaScript rather than loose equality (==), because == performs Surprising type coercion (like '5' == 5 being true). Combine === with if / else if / else and for loops.",
            whyItExists = "Strict equality checks BOTH value and data type, preventing subtle bugs when comparing strings and numbers.",
            simplerExplanation = "Loose equality (==) says a picture of an apple is the same as a real apple; strict equality (===) checks that both are real apples.",
            syntaxGuide = "if (score === 100) {\n    console.log(\"Perfect!\");\n}",
            workingExampleCode = "const points = 100;\nif (points === 100) {\n    console.log(\"Strict Match: 100\");\n}\nfor (let i = 1; i <= 3; i++) {\n    console.log(\"Lap\", i);\n}",
            lineByLineWalkthrough = listOf(
                "Lines 2-4: Checks strict equality (value 100 and type number).",
                "Lines 5-7: Loops i from 1 through 3."
            ),
            expectedOutput = "Strict Match: 100\nLap 1\nLap 2\nLap 3",
            commonMistakes = listOf(
                "Using '==' which evaluates 0 == false and \"5\" == 5 as true."
            ),
            realWorldExample = "Validating form values and iterating over UI items.",
            exercisePrompt = "Log the numbers 1, 2, 3 using a for loop in JavaScript.",
            exerciseStarterCode = "for (let i = 1; i <= 3; i++) {\n    console.log(i);\n}",
            exerciseSolutionCode = "for (let i = 1; i <= 3; i++) {\n    console.log(i);\n}",
            exerciseExpectedOutputContains = listOf("1", "2", "3"),
            exerciseHint = "Click Run Code to execute the loop.",
            quizQuestion = "What does '5' === 5 evaluate to in JavaScript?",
            quizOptions = listOf("false (because string and number are different types)", "true", "55", "SyntaxError"),
            quizCorrectIndex = 0,
            quizWrongExplanation = "=== checks both type and value. '5' is a string and 5 is a number, so '5' === 5 is false.",
            quizHint = "Are their data types identical?"
        ),
        CodingLesson(
            id = "js_04",
            languageId = ProgrammingLanguageId.JAVASCRIPT,
            moduleNumber = 2,
            moduleTitle = "Module 2: Functions, Arrays, Objects & Async JS",
            lessonNumber = 4,
            title = "4. Functions, Arrow Functions & Array Higher-Order Methods",
            difficulty = "Intermediate",
            topicTag = "Functions & Arrays",
            conceptExplanation = "JavaScript supports traditional function declarations and concise ES6 arrow functions: const add = (a, b) => a + b. Arrays provide functional methods like .map() (transform), .filter() (select), and .reduce() (aggregate).",
            whyItExists = "Arrow functions and array methods let you process data pipelines declaratively without mutating state.",
            simplerExplanation = ".map() transforms every item in an array, .filter() keeps only items that pass a test, and .reduce() combines them into one total.",
            syntaxGuide = "const nums = [1, 2, 3, 4];\nconst doubled = nums.map(n => n * 2);",
            workingExampleCode = "const scores = [72, 88, 95, 64, 91];\nconst honors = scores.filter(s => s >= 85);\nconst bonusScores = honors.map(s => s + 5);\nconsole.log(\"Honors:\", JSON.stringify(honors));\nconsole.log(\"With Bonus:\", JSON.stringify(bonusScores));",
            lineByLineWalkthrough = listOf(
                "Line 2: .filter() keeps only scores >= 85 ([88, 95, 91]).",
                "Line 3: .map() adds 5 to each honors score ([93, 100, 96])."
            ),
            expectedOutput = "Honors: [88,95,91]\nWith Bonus: [93,100,96]",
            commonMistakes = listOf(
                "Forgetting the 'return' keyword when using curly braces { } in an arrow function."
            ),
            realWorldExample = "Filtering a product catalog by price and mapping items into UI cards.",
            exercisePrompt = "Run the arrow function and array filter/map pipeline.",
            exerciseStarterCode = "const nums = [2, 4, 6];\nconst res = nums.map(n => n * 10);\nconsole.log(JSON.stringify(res));",
            exerciseSolutionCode = "const nums = [2, 4, 6];\nconst res = nums.map(n => n * 10);\nconsole.log(JSON.stringify(res));",
            exerciseExpectedOutputContains = listOf("[20,40,60]"),
            exerciseHint = "Run the code to output [20,40,60].",
            quizQuestion = "Which JavaScript array method creates a new array containing only elements that pass a condition?",
            quizOptions = listOf(".filter()", ".map()", ".forEach()", ".push()"),
            quizCorrectIndex = 0,
            quizWrongExplanation = ".filter(predicate) returns a new array with only elements where the callback returned true.",
            quizHint = "Think of a coffee filter."
        ),
        CodingLesson(
            id = "js_05",
            languageId = ProgrammingLanguageId.JAVASCRIPT,
            moduleNumber = 2,
            moduleTitle = "Module 2: Functions, Arrays, Objects & Async JS",
            lessonNumber = 5,
            title = "5. Objects, Destructuring, JSON & Async/Await",
            difficulty = "Advanced",
            topicTag = "Objects & Async",
            conceptExplanation = "JavaScript objects store key-value properties and methods. Object destructuring (const { name, grade } = user) extracts properties cleanly, while Promises and async/await handle asynchronous network operations without freezing the UI thread.",
            whyItExists = "Every web API speaks JSON (JavaScript Object Notation), and async/await keeps web apps responsive while waiting for servers.",
            simplerExplanation = "Asynchronous code is like ordering food at a cafe with a buzzer: you don't stand frozen at the counter—you sit down and continue chatting until the buzzer rings (Promise resolves).",
            syntaxGuide = "const scholar = { name: \"Aria\",xp: 350 };\nconst { name, xp } = scholar;\nconsole.log(name, xp);",
            workingExampleCode = "const scholar = { name: \"Aria\", rank: \"Diamond\", coins: 320 };\nconst { name, rank, coins } = scholar;\nconsole.log(`Scholar ${'$'}{name} [${'$'}{rank}] has ${'$'}{coins} coins`);\nconsole.log(\"JSON:\", JSON.stringify(scholar));",
            lineByLineWalkthrough = listOf(
                "Line 1: Creates an object literal with 3 properties.",
                "Line 2: Destructures name, rank, and coins into constants.",
                "Lines 3-4: Logs formatted details and serialized JSON."
            ),
            expectedOutput = "Scholar Aria [Diamond] has 320 coins\nJSON: {\"name\":\"Aria\",\"rank\":\"Diamond\",\"coins\":320}",
            commonMistakes = listOf(
                "Using 'await' outside an 'async' function."
            ),
            realWorldExample = "Fetching user profile JSON from a REST API and destructuring the response fields.",
            exercisePrompt = "Run the object destructuring and JSON serialization example.",
            exerciseStarterCode = "const user = { id: 1, role: \"Coder\" };\nconsole.log(user.role);\nconsole.log(JSON.stringify(user));",
            exerciseSolutionCode = "const user = { id: 1, role: \"Coder\" };\nconsole.log(user.role);\nconsole.log(JSON.stringify(user));",
            exerciseExpectedOutputContains = listOf("Coder"),
            exerciseHint = "Run the code.",
            quizQuestion = "What does JSON.stringify(obj) do in JavaScript?",
            quizOptions = listOf(
                "Converts a JavaScript object into a JSON-formatted string",
                "Parses a JSON string into an object",
                "Deletes all keys from the object",
                "Encrypts the object"
            ),
            quizCorrectIndex = 0,
            quizWrongExplanation = "JSON.stringify() serializes an object into a string, whereas JSON.parse() converts a string back into an object.",
            quizHint = "Look at the word 'stringify'."
        )
    )

    // C++ CURRICULUM (Language-Specific)
    private val cppLessons: List<CodingLesson> = listOf(
        CodingLesson(
            id = "cpp_01",
            languageId = ProgrammingLanguageId.CPP,
            moduleNumber = 1,
            moduleTitle = "Module 1: C++ Compilation, Types & I/O Streams",
            lessonNumber = 1,
            title = "1. Structure of a C++ Program: #include, main(), and std::cout",
            difficulty = "Beginner",
            topicTag = "C++ Basics",
            conceptExplanation = "C++ is a statically typed, compiled systems language. Every C++ program starts execution inside the int main() function. We include <iostream> to use std::cout with the stream insertion operator (<<) to print output, and end statements with a semicolon (;).",
            whyItExists = "Because C++ compiles directly to native machine instructions, it powers Unreal Engine games, operating systems, browsers, and high-frequency systems.",
            simplerExplanation = "If Python is an automatic car, C++ is a Formula 1 race car where you control every gear directly for maximum speed.",
            syntaxGuide = "#include <iostream>\nusing namespace std;\n\nint main() {\n    cout << \"Hello World\" << endl;\n    return 0;\n}",
            workingExampleCode = "#include <iostream>\nusing namespace std;\n\nint main() {\n    cout << \"Falcon AI C++ Compiler Online\" << endl;\n    cout << \"Speed: \" << 100 << \"%\" << endl;\n    return 0;\n}",
            lineByLineWalkthrough = listOf(
                "Line 1: Includes the standard Input/Output stream library.",
                "Line 4: Entry point function where execution begins.",
                "Lines 5-6: Streams text and numbers to standard output.",
                "Line 7: Returns exit code 0 to signal successful execution to the OS."
            ),
            expectedOutput = "Falcon AI C++ Compiler Online\nSpeed: 100%",
            commonMistakes = listOf(
                "Forgetting the semicolon (;) at the end of a C++ statement.",
                "Using >> instead of << with cout (<< points data INTO cout)."
            ),
            realWorldExample = "High-performance game loop initialization and telemetry logging.",
            exercisePrompt = "Write a complete C++ program that prints Hello World using cout.",
            exerciseStarterCode = "#include <iostream>\nusing namespace std;\n\nint main() {\n    cout << \"Hello World\" << endl;\n    return 0;\n}",
            exerciseSolutionCode = "#include <iostream>\nusing namespace std;\n\nint main() {\n    cout << \"Hello World\" << endl;\n    return 0;\n}",
            exerciseExpectedOutputContains = listOf("Hello World"),
            exerciseHint = "Ensure cout << \"Hello World\" << endl; ends with a semicolon.",
            quizQuestion = "What is the required entry-point function in every standard C++ program?",
            quizOptions = listOf("int main()", "void start()", "def main():", "function init()"),
            quizCorrectIndex = 0,
            quizWrongExplanation = "The C++ linker looks specifically for int main() as the program's starting point.",
            quizHint = "It returns an int and is named 'main'."
        ),
        CodingLesson(
            id = "cpp_02",
            languageId = ProgrammingLanguageId.CPP,
            moduleNumber = 1,
            moduleTitle = "Module 1: C++ Compilation, Types & I/O Streams",
            lessonNumber = 2,
            title = "2. Static Data Types, Variables & std::cin Input",
            difficulty = "Beginner",
            topicTag = "Types & cin",
            conceptExplanation = "In C++, every variable must declare its type upfront: int (whole numbers), double (64-bit decimals), char (single character 'A'), bool (true/false), and string. Use std::cin >> variable to read values from Standard Input.",
            whyItExists = "Declaring exact types allows the C++ compiler to allocate exact bytes in CPU registers and catch type errors before the program ever runs.",
            simplerExplanation = "Static typing is like custom-fitted trays: an 'int' tray holds a 32-bit integer, and the compiler won't let you accidentally jam a text string into it.",
            syntaxGuide = "int age = 16;\ndouble gpa = 3.95;\ncin >> age;\ncout << \"Age: \" << age << endl;",
            workingExampleCode = "#include <iostream>\nusing namespace std;\n\nint main() {\n    int coins = 120;\n    int bonus = 80;\n    int total = coins + bonus;\n    cout << \"Total Coins: \" << total << endl;\n    return 0;\n}",
            lineByLineWalkthrough = listOf(
                "Lines 5-7: Declares strongly-typed int variables and computes their sum.",
                "Line 8: Outputs Total Coins: 200."
            ),
            expectedOutput = "Total Coins: 200",
            commonMistakes = listOf(
                "Integer division truncation: in C++, 7 / 2 evaluates to 3 (not 3.5) because both operands are ints!"
            ),
            realWorldExample = "Physics simulations calculating exact positions and velocities.",
            exercisePrompt = "Compute and output the product of int a = 12 and int b = 5.",
            exerciseStarterCode = "#include <iostream>\nusing namespace std;\n\nint main() {\n    int a = 12;\n    int b = 5;\n    cout << a * b << endl;\n    return 0;\n}",
            exerciseSolutionCode = "#include <iostream>\nusing namespace std;\n\nint main() {\n    int a = 12;\n    int b = 5;\n    cout << a * b << endl;\n    return 0;\n}",
            exerciseExpectedOutputContains = listOf("60"),
            exerciseHint = "Run the code to print 60.",
            quizQuestion = "What is the result of 9 / 2 in C++ when both 9 and 2 are int values?",
            quizOptions = listOf("4 (fractional part is truncated)", "4.5", "5", "Compiler Error"),
            quizCorrectIndex = 0,
            quizWrongExplanation = "Dividing two integers in C++ performs integer division, discarding the remainder.",
            quizHint = "Integer division drops the decimal."
        ),
        CodingLesson(
            id = "cpp_03",
            languageId = ProgrammingLanguageId.CPP,
            moduleNumber = 2,
            moduleTitle = "Module 2: Control Flow, Vectors, Pointers & OOP",
            lessonNumber = 3,
            title = "3. Control Flow, Loops, Functions & Pass-by-Reference",
            difficulty = "Intermediate",
            topicTag = "Functions & Refs",
            conceptExplanation = "C++ functions specify their return type and parameter types (e.g., int add(int a, int b)). Passing a parameter by reference (int &x) lets a function modify the caller's variable directly without copying memory.",
            whyItExists = "Pass-by-reference avoids expensive copying of large data structures while remaining clean and safe.",
            simplerExplanation = "Pass-by-value is handing someone a photocopy of your document; pass-by-reference (&) is sharing the live Google Doc so their edits appear on your original.",
            syntaxGuide = "int multiply(int x, int y) {\n    return x * y;\n}",
            workingExampleCode = "#include <iostream>\nusing namespace std;\n\nint square(int n) {\n    return n * n;\n}\n\nint main() {\n    for (int i = 1; i <= 4; i++) {\n        cout << i << \"^2 = \" << square(i) << endl;\n    }\n    return 0;\n}",
            lineByLineWalkthrough = listOf(
                "Lines 4-6: Defines typed function square(int n).",
                "Lines 9-11: Loops i from 1 to 4 and prints each square."
            ),
            expectedOutput = "1^2 = 1\n2^2 = 4\n3^2 = 9\n4^2 = 16",
            commonMistakes = listOf(
                "Declaring a non-void function without returning a value."
            ),
            realWorldExample = "Game engine math utilities computing vector magnitudes.",
            exercisePrompt = "Run the C++ loop calling square(i) for 1 through 4.",
            exerciseStarterCode = "#include <iostream>\nusing namespace std;\n\nint square(int n) {\n    return n * n;\n}\n\nint main() {\n    for (int i = 1; i <= 4; i++) {\n        cout << square(i) << endl;\n    }\n    return 0;\n}",
            exerciseSolutionCode = "#include <iostream>\nusing namespace std;\n\nint square(int n) {\n    return n * n;\n}\n\nint main() {\n    for (int i = 1; i <= 4; i++) {\n        cout << square(i) << endl;\n    }\n    return 0;\n}",
            exerciseExpectedOutputContains = listOf("1", "4", "9", "16"),
            exerciseHint = "Run the code.",
            quizQuestion = "What symbol in a C++ parameter list indicates pass-by-reference (e.g., void update(int &val))?",
            quizOptions = listOf("& (Ampersand)", "@ (At sign)", "$ (Dollar sign)", "# (Hash)"),
            quizCorrectIndex = 0,
            quizWrongExplanation = "In C++, '&' in a parameter declaration binds a reference to the caller's variable.",
            quizHint = "It's the ampersand symbol."
        ),
        CodingLesson(
            id = "cpp_04",
            languageId = ProgrammingLanguageId.CPP,
            moduleNumber = 2,
            moduleTitle = "Module 2: Control Flow, Vectors, Pointers & OOP",
            lessonNumber = 4,
            title = "4. STL std::vector, Pointers, and Classes",
            difficulty = "Advanced",
            topicTag = "STL & Classes",
            conceptExplanation = "std::vector<T> is C++'s dynamic resizable array from the Standard Template Library (STL). Pointers (int* ptr = &x) store memory addresses, and C++ Classes encapsulate private state with public constructors and methods.",
            whyItExists = "Vectors manage heap memory automatically (RAII), giving you raw array speed with dynamic resizing.",
            simplerExplanation = "A pointer is a house's street address written on an index card, while dereferencing (*ptr) is walking to that address and opening the front door.",
            syntaxGuide = "#include <vector>\nvector<int> nums = {10, 20, 30};\nnums.push_back(40);",
            workingExampleCode = "#include <iostream>\n#include <vector>\nusing namespace std;\n\nint main() {\n    vector<int> scores = {80, 90, 100};\n    int sum = 0;\n    for (int s : scores) {\n        sum += s;\n    }\n    cout << \"Vector Sum: \" << sum << endl;\n    return 0;\n}",
            lineByLineWalkthrough = listOf(
                "Line 6: Initializes a dynamic std::vector<int> with 3 elements.",
                "Lines 8-10: Uses a range-based for loop to sum all elements."
            ),
            expectedOutput = "Vector Sum: 270",
            commonMistakes = listOf(
                "Dereferencing an uninitialized or nullptr pointer."
            ),
            realWorldExample = "Storing active 3D scene entities in a game engine using std::vector.",
            exercisePrompt = "Run the std::vector sum calculation in C++.",
            exerciseStarterCode = "#include <iostream>\n#include <vector>\nusing namespace std;\n\nint main() {\n    vector<int> scores = {80, 90, 100};\n    int sum = 0;\n    for (int s : scores) {\n        sum += s;\n    }\n    cout << \"Vector Sum: \" << sum << endl;\n    return 0;\n}",
            exerciseSolutionCode = "#include <iostream>\n#include <vector>\nusing namespace std;\n\nint main() {\n    vector<int> scores = {80, 90, 100};\n    int sum = 0;\n    for (int s : scores) {\n        sum += s;\n    }\n    cout << \"Vector Sum: \" << sum << endl;\n    return 0;\n}",
            exerciseExpectedOutputContains = listOf("Vector Sum: 270"),
            exerciseHint = "Click Run Code.",
            quizQuestion = "Which C++ STL container provides a dynamically resizable array?",
            quizOptions = listOf("std::vector", "std::map", "std::set", "std::pair"),
            quizCorrectIndex = 0,
            quizWrongExplanation = "std::vector<T> stores contiguous elements in memory and grows automatically with push_back().",
            quizHint = "It starts with 'std::vec'."
        )
    )

    // JAVA CURRICULUM (Language-Specific)
    private val javaLessons: List<CodingLesson> = listOf(
        CodingLesson(
            id = "java_01",
            languageId = ProgrammingLanguageId.JAVA,
            moduleNumber = 1,
            moduleTitle = "Module 1: Java JVM, Classes & Core Syntax",
            lessonNumber = 1,
            title = "1. How the JVM Works, Class Structure & System.out.println",
            difficulty = "Beginner",
            topicTag = "Java Basics",
            conceptExplanation = "Java compiles source code (.java) into platform-independent bytecode (.class) that runs on any device with a Java Virtual Machine (JVM). Every Java program lives inside a class and starts at public static void main(String[] args).",
            whyItExists = "Write Once, Run Anywhere: the same compiled Java program runs on Linux servers, Windows desktops, and cloud clusters.",
            simplerExplanation = "Java requires every piece of code to live inside a Class building, and 'main' is the front lobby entrance.",
            syntaxGuide = "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello World\");\n    }\n}",
            workingExampleCode = "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Falcon AI Java Runtime Ready\");\n        System.out.println(\"JVM Status: Active\");\n    }\n}",
            lineByLineWalkthrough = listOf(
                "Line 1: Declares public class Main.",
                "Line 2: Standard main entry point method.",
                "Lines 3-4: Prints lines to standard output."
            ),
            expectedOutput = "Falcon AI Java Runtime Ready\nJVM Status: Active",
            commonMistakes = listOf(
                "Writing system.out.println with a lowercase 's' — System is a class name and must start with capital 'S'."
            ),
            realWorldExample = "Enterprise banking backends, microservices, and Android system services.",
            exercisePrompt = "Print Hello World using System.out.println in Java.",
            exerciseStarterCode = "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello World\");\n    }\n}",
            exerciseSolutionCode = "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello World\");\n    }\n}",
            exerciseExpectedOutputContains = listOf("Hello World"),
            exerciseHint = "Run System.out.println(\"Hello World\");",
            quizQuestion = "What executes compiled Java bytecode across different operating systems?",
            quizOptions = listOf("JVM (Java Virtual Machine)", "Browser DOM", "Python Interpreter", "CSS Engine"),
            quizCorrectIndex = 0,
            quizWrongExplanation = "The Java Virtual Machine (JVM) translates Java bytecode into native machine instructions on any OS.",
            quizHint = "Stands for Java Virtual Machine."
        ),
        CodingLesson(
            id = "java_02",
            languageId = ProgrammingLanguageId.JAVA,
            moduleNumber = 1,
            moduleTitle = "Module 1: Java JVM, Classes & Core Syntax",
            lessonNumber = 2,
            title = "2. Primitive Types, Strings, Loops & OOP Methods",
            difficulty = "Intermediate",
            topicTag = "Java OOP",
            conceptExplanation = "Java distinguishes primitive types (int, double, boolean, char) from reference object types (String, ArrayList). Static methods can be called directly from main() to organize reusable logic.",
            whyItExists = "Strong static typing and object-oriented structure make large multi-developer codebases reliable and self-documenting.",
            simplerExplanation = "Primitives are lightweight coins kept right in your pocket (stack); Objects are vaults stored in the bank (heap) that you access with a keycard reference.",
            syntaxGuide = "int score = 95;\nboolean passed = score >= 60;\nSystem.out.println(\"Passed: \" + passed);",
            workingExampleCode = "public class Main {\n    public static void main(String[] args) {\n        int sum = 0;\n        for (int i = 1; i <= 5; i++) {\n            sum += i * 10;\n        }\n        System.out.println(\"Calculated Sum: \" + sum);\n    }\n}",
            lineByLineWalkthrough = listOf(
                "Line 3: Initializes int sum = 0.",
                "Lines 4-6: Loops i from 1 to 5, adding i * 10 each iteration (10+20+30+40+50 = 150).",
                "Line 7: Prints Calculated Sum: 150."
            ),
            expectedOutput = "Calculated Sum: 150",
            commonMistakes = listOf(
                "Comparing two Strings with == instead of .equals() in Java."
            ),
            realWorldExample = "Calculating invoice totals and validating user permissions in backend services.",
            exercisePrompt = "Run the Java loop to compute and output Calculated Sum: 150.",
            exerciseStarterCode = "public class Main {\n    public static void main(String[] args) {\n        int sum = 0;\n        for (int i = 1; i <= 5; i++) {\n            sum += i * 10;\n        }\n        System.out.println(\"Calculated Sum: \" + sum);\n    }\n}",
            exerciseSolutionCode = "public class Main {\n    public static void main(String[] args) {\n        int sum = 0;\n        for (int i = 1; i <= 5; i++) {\n            sum += i * 10;\n        }\n        System.out.println(\"Calculated Sum: \" + sum);\n    }\n}",
            exerciseExpectedOutputContains = listOf("Calculated Sum: 150"),
            exerciseHint = "Click Run Code.",
            quizQuestion = "How should you compare two String values for content equality in Java?",
            quizOptions = listOf("str1.equals(str2)", "str1 == str2", "str1 === str2", "str1.same(str2)"),
            quizCorrectIndex = 0,
            quizWrongExplanation = "In Java, '==' checks if two references point to the exact same memory object; .equals() compares the actual characters.",
            quizHint = "Use the .equals() method."
        )
    )

    // C CURRICULUM (Language-Specific)
    private val cLessons: List<CodingLesson> = listOf(
        CodingLesson(
            id = "c_01",
            languageId = ProgrammingLanguageId.C,
            moduleNumber = 1,
            moduleTitle = "Module 1: C Systems Programming & Memory",
            lessonNumber = 1,
            title = "1. C Compilation, stdio.h & printf() Format Specifiers",
            difficulty = "Beginner",
            topicTag = "C Basics",
            conceptExplanation = "C is the foundational systems programming language used to build Linux, Git, Python's interpreter (CPython), and embedded microcontrollers. Use #include <stdio.h> and printf() with format specifiers (%d for int, %f for float, %s for string).",
            whyItExists = "C gives direct, zero-overhead control over hardware memory and CPU instructions.",
            simplerExplanation = "Format specifiers like %d are placeholders in a template sentence that printf fills in with your numbers.",
            syntaxGuide = "#include <stdio.h>\nint main() {\n    int score = 100;\n    printf(\"Score: %d\\n\", score);\n    return 0;\n}",
            workingExampleCode = "#include <stdio.h>\n\nint main() {\n    int level = 3;\n    int xp = 450;\n    printf(\"Falcon C Runtime\\n\");\n    printf(\"Level %d - XP: %d\\n\", level, xp);\n    return 0;\n}",
            lineByLineWalkthrough = listOf(
                "Line 1: Includes Standard Input/Output header.",
                "Line 7: Replaces the two %d placeholders with level (3) and xp (450)."
            ),
            expectedOutput = "Falcon C Runtime\nLevel 3 - XP: 450",
            commonMistakes = listOf(
                "Forgetting \\n at the end of printf(), causing lines to run together.",
                "Mismatching format specifiers (like using %s for an int)."
            ),
            realWorldExample = "Embedded firmware, operating system kernels, and robotics controllers.",
            exercisePrompt = "Use printf to output Hello World in C.",
            exerciseStarterCode = "#include <stdio.h>\n\nint main() {\n    printf(\"Hello World\\n\");\n    return 0;\n}",
            exerciseSolutionCode = "#include <stdio.h>\n\nint main() {\n    printf(\"Hello World\\n\");\n    return 0;\n}",
            exerciseExpectedOutputContains = listOf("Hello World"),
            exerciseHint = "Run printf(\"Hello World\\n\");",
            quizQuestion = "Which format specifier is used in printf() to print an integer (int) in C?",
            quizOptions = listOf("%d", "%s", "%f", "%c"),
            quizCorrectIndex = 0,
            quizWrongExplanation = "%d (decimal integer) prints an int value in C.",
            quizHint = "d stands for decimal integer."
        ),
        CodingLesson(
            id = "c_02",
            languageId = ProgrammingLanguageId.C,
            moduleNumber = 1,
            moduleTitle = "Module 1: C Systems Programming & Memory",
            lessonNumber = 2,
            title = "2. Loops, Arrays, Pointers & Memory Addresses",
            difficulty = "Intermediate",
            topicTag = "Pointers & Arrays",
            conceptExplanation = "In C, arrays are contiguous blocks of memory, and pointers store the exact memory address (&variable) of a value. Understanding pointers reveals how computers actually organize data in RAM.",
            whyItExists = "Pointers allow C functions to modify variables in place and traverse hardware buffers at maximum speed.",
            simplerExplanation = "Every byte in RAM has a street number (address). '&x' tells you x's street number, and '*ptr' visits that street number.",
            syntaxGuide = "int x = 42;\nint *ptr = &x;\nprintf(\"%d\\n\", *ptr);",
            workingExampleCode = "#include <stdio.h>\n\nint main() {\n    int sum = 0;\n    for (int i = 1; i <= 5; i++) {\n        sum += i;\n    }\n    printf(\"Sum 1..5: %d\\n\", sum);\n    return 0;\n}",
            lineByLineWalkthrough = listOf(
                "Lines 5-7: Accumulates 1 + 2 + 3 + 4 + 5 into sum.",
                "Line 8: Prints Sum 1..5: 15."
            ),
            expectedOutput = "Sum 1..5: 15",
            commonMistakes = listOf(
                "Reading past the end of a fixed-size C array (buffer overflow)."
            ),
            realWorldExample = "Audio/video codecs and network packet processing.",
            exercisePrompt = "Run the C loop to calculate and print Sum 1..5: 15.",
            exerciseStarterCode = "#include <stdio.h>\n\nint main() {\n    int sum = 0;\n    for (int i = 1; i <= 5; i++) {\n        sum += i;\n    }\n    printf(\"Sum 1..5: %d\\n\", sum);\n    return 0;\n}",
            exerciseSolutionCode = "#include <stdio.h>\n\nint main() {\n    int sum = 0;\n    for (int i = 1; i <= 5; i++) {\n        sum += i;\n    }\n    printf(\"Sum 1..5: %d\\n\", sum);\n    return 0;\n}",
            exerciseExpectedOutputContains = listOf("Sum 1..5: 15"),
            exerciseHint = "Click Run Code.",
            quizQuestion = "What does the '&' operator (e.g., &x) return in C?",
            quizOptions = listOf("The memory address of variable x", "The square of x", "A copy of x", "The data type of x"),
            quizCorrectIndex = 0,
            quizWrongExplanation = "The address-of operator (&) returns the memory address where variable x is stored.",
            quizHint = "Think of 'address-of'."
        )
    )

    // HTML & CSS CURRICULUM (Language-Specific)
    private val htmlCssLessons: List<CodingLesson> = listOf(
        CodingLesson(
            id = "web_01",
            languageId = ProgrammingLanguageId.HTML_CSS,
            moduleNumber = 1,
            moduleTitle = "Module 1: HTML5 Structure & CSS3 Modern Styling",
            lessonNumber = 1,
            title = "1. HTML5 Semantic Structure, Headings, Paragraphs & Cards",
            difficulty = "Beginner",
            topicTag = "HTML5 Structure",
            conceptExplanation = "HTML (HyperText Markup Language) structures web content using semantic elements like <h1> (main heading), <p> (paragraph), <button>, and <div> / <section> containers. Most elements have an opening tag <tag> and closing tag </tag>.",
            whyItExists = "Browsers, screen readers, and search engines rely on semantic HTML tags to understand and render every website on the internet.",
            simplerExplanation = "HTML tags are like architectural labels on a blueprint—marking which part is the title banner, which is a paragraph, and which is a button.",
            syntaxGuide = "<h1>Main Heading</h1>\n<p>This is a paragraph of text.</p>\n<button>Click Me</button>",
            workingExampleCode = "<!DOCTYPE html>\n<html>\n<body>\n  <h1>Hello World</h1>\n  <p>Welcome to Falcon AI Web Development!</p>\n</body>\n</html>",
            lineByLineWalkthrough = listOf(
                "Line 1: Declares modern HTML5 document mode.",
                "Line 4: Renders a top-level heading 'Hello World'.",
                "Line 5: Renders a descriptive paragraph."
            ),
            expectedOutput = "Hello World\nWelcome to Falcon AI Web Development!",
            commonMistakes = listOf(
                "Forgetting the forward slash in closing tags like </h1> or </p>."
            ),
            realWorldExample = "Every landing page, blog article, and web dashboard starts with semantic HTML5 markup.",
            exercisePrompt = "Include an <h1>Hello World</h1> heading inside your HTML document and run the preview.",
            exerciseStarterCode = "<!DOCTYPE html>\n<html>\n<body>\n  <h1>Hello World</h1>\n  <p>Built in Falcon AI</p>\n</body>\n</html>",
            exerciseSolutionCode = "<!DOCTYPE html>\n<html>\n<body>\n  <h1>Hello World</h1>\n  <p>Built in Falcon AI</p>\n</body>\n</html>",
            exerciseExpectedOutputContains = listOf("Hello World"),
            exerciseHint = "Keep <h1>Hello World</h1> inside the <body> tag and tap Run.",
            quizQuestion = "Which HTML tag represents the most important top-level heading on a page?",
            quizOptions = listOf("<h1>", "<h6>", "<head>", "<title>"),
            quizCorrectIndex = 0,
            quizWrongExplanation = "<h1> is the primary top-level heading, while <h6> is the smallest subheading.",
            quizHint = "Number 1 is the top level."
        ),
        CodingLesson(
            id = "web_02",
            languageId = ProgrammingLanguageId.HTML_CSS,
            moduleNumber = 1,
            moduleTitle = "Module 1: HTML5 Structure & CSS3 Modern Styling",
            lessonNumber = 2,
            title = "2. CSS Selectors, Box Model & Responsive Flexbox Layouts",
            difficulty = "Intermediate",
            topicTag = "CSS Flexbox",
            conceptExplanation = "CSS (Cascading Style Sheets) controls colors, typography, spacing (padding inside, margin outside, border around), and layout. Setting display: flex on a container aligns child cards smoothly across mobile and desktop screens.",
            whyItExists = "Without CSS, every website would look like a plain black-and-white text document from 1991.",
            simplerExplanation = "In the CSS Box Model, 'padding' is the bubble wrap inside a shipping box around the gift, 'border' is the cardboard wall, and 'margin' is the space between boxes.",
            syntaxGuide = ".card {\n  background: #4F46E5;\n  color: white;\n  padding: 16px;\n  border-radius: 12px;\n}",
            workingExampleCode = "<!DOCTYPE html>\n<html>\n<head>\n<style>\n  .box { background: #4F46E5; color: white; padding: 16px; border-radius: 12px; }\n</style>\n</head>\n<body>\n  <div class=\"box\">Styled Flexbox Card</div>\n</body>\n</html>",
            lineByLineWalkthrough = listOf(
                "Line 5: Defines the .box class selector with background, padding, and rounded corners.",
                "Line 9: Applies class=\"box\" to the div container."
            ),
            expectedOutput = "Styled Flexbox Card",
            commonMistakes = listOf(
                "Forgetting the dot (.) before a class selector name in CSS (e.g., writing card {} instead of .card {})."
            ),
            realWorldExample = "Responsive navigation bars and feature grids on modern web applications.",
            exercisePrompt = "Run the styled CSS card preview.",
            exerciseStarterCode = "<!DOCTYPE html>\n<html>\n<head>\n<style>\n  .box { background: #4F46E5; color: white; padding: 16px; border-radius: 12px; }\n</style>\n</head>\n<body>\n  <div class=\"box\">Styled Flexbox Card</div>\n</body>\n</html>",
            exerciseSolutionCode = "<!DOCTYPE html>\n<html>\n<head>\n<style>\n  .box { background: #4F46E5; color: white; padding: 16px; border-radius: 12px; }\n</style>\n</head>\n<body>\n  <div class=\"box\">Styled Flexbox Card</div>\n</body>\n</html>",
            exerciseExpectedOutputContains = listOf("Styled Flexbox Card"),
            exerciseHint = "Tap Run to render the HTML/CSS preview.",
            quizQuestion = "In the CSS Box Model, which property controls the inner space between an element's text content and its border?",
            quizOptions = listOf("padding", "margin", "outline", "gap"),
            quizCorrectIndex = 0,
            quizWrongExplanation = "Padding is the space inside the border; margin is the space outside the border.",
            quizHint = "Think of inner protective padding inside a box."
        )
    )

    // INTERACTIVE PRACTICE CHALLENGES (Multi-Test-Case, Bug Fixing, Predict Output, Write from Scratch)
    fun getPracticeChallenges(lang: ProgrammingLanguageId): List<PracticeChallenge> = when (lang) {
        ProgrammingLanguageId.PYTHON -> listOf(
            PracticeChallenge(
                id = "py_prac_1",
                languageId = ProgrammingLanguageId.PYTHON,
                type = ChallengeType.PREDICT_OUTPUT,
                title = "Predict Output: Modulo & Floor Division",
                difficulty = "Beginner",
                topicTag = "Operators",
                prompt = "What will this Python code output when executed?",
                starterCode = "x = 19\ny = 5\nprint(x // y, x % y)",
                options = listOf("3 4", "3.8 4", "4 3", "3 5"),
                correctOptionIndex = 0,
                hint = "19 // 5 is how many full 5s fit in 19 (3), and 19 % 5 is the remainder (4).",
                explanation = "19 divided by 5 is 3 with a remainder of 4. print(x // y, x % y) outputs '3 4'.",
                solutionCode = "x = 19\ny = 5\nprint(x // y, x % y)"
            ),
            PracticeChallenge(
                id = "py_prac_2",
                languageId = ProgrammingLanguageId.PYTHON,
                type = ChallengeType.FIND_THE_BUG,
                title = "Find the Bug: Even Sum Calculator",
                difficulty = "Beginner",
                topicTag = "Loops & Conditionals",
                prompt = "This program should calculate the sum of even numbers in [2, 5, 8, 11, 10] (which is 20), but it has a bug in the condition. Fix the bug so it prints 20!",
                starterCode = "nums = [2, 5, 8, 11, 10]\ntotal = 0\nfor n in nums:\n    if n % 2 == 1:\n        total = total + n\nprint(total)",
                testCases = listOf(
                    CodingTestCase(input = "", expectedOutput = "20", description = "Sum of evens in [2, 5, 8, 11, 10] == 20"),
                    CodingTestCase(input = "", expectedOutput = "20", description = "Hidden edge verification", isHidden = true)
                ),
                hint = "n % 2 == 1 checks for ODD numbers! Change it to n % 2 == 0 to check for EVEN numbers.",
                explanation = "Even numbers have a remainder of 0 when divided by 2 (n % 2 == 0).",
                solutionCode = "nums = [2, 5, 8, 11, 10]\ntotal = 0\nfor n in nums:\n    if n % 2 == 0:\n        total = total + n\nprint(total)"
            ),
            PracticeChallenge(
                id = "py_prac_3",
                languageId = ProgrammingLanguageId.PYTHON,
                type = ChallengeType.WRITE_FROM_SCRATCH,
                title = "Write from Scratch: Double the Input Number",
                difficulty = "Intermediate",
                topicTag = "Input/Output",
                prompt = "Read an integer from input() and print that integer multiplied by 2. Your solution will be tested against multiple test cases (including hidden edge cases like 0 and negative numbers).",
                starterCode = "# Read an integer from input() and print it multiplied by 2\nn = int(input())\nprint(n * 2)",
                testCases = listOf(
                    CodingTestCase(input = "21", expectedOutput = "42", description = "Positive integer: 21 -> 42"),
                    CodingTestCase(input = "0", expectedOutput = "0", description = "Zero edge case: 0 -> 0"),
                    CodingTestCase(input = "-15", expectedOutput = "-30", description = "Negative integer (Hidden test case)", isHidden = true)
                ),
                hint = "Convert input() with int(input()), then print(n * 2).",
                explanation = "Using n = int(input()) followed by print(n * 2) works for positive, zero, and negative integers.",
                solutionCode = "n = int(input())\nprint(n * 2)"
            ),
            PracticeChallenge(
                id = "py_prac_4",
                languageId = ProgrammingLanguageId.PYTHON,
                type = ChallengeType.COMPLETE_THE_CODE,
                title = "Complete the Function: Maximum of Two",
                difficulty = "Intermediate",
                topicTag = "Functions",
                prompt = "Complete the max_of_two(a, b) function so it returns the larger number and prints the result for (14, 29) -> 29.",
                starterCode = "def max_of_two(a, b):\n    if a > b:\n        return a\n    return b\n\nprint(max_of_two(14, 29))",
                testCases = listOf(
                    CodingTestCase(input = "", expectedOutput = "29", description = "max_of_two(14, 29) outputs 29")
                ),
                hint = "Return a if a > b else return b.",
                explanation = "Comparing a > b and returning the larger value handles any pair of numbers.",
                solutionCode = "def max_of_two(a, b):\n    if a > b:\n        return a\n    return b\n\nprint(max_of_two(14, 29))"
            )
        )

        ProgrammingLanguageId.JAVASCRIPT -> listOf(
            PracticeChallenge(
                id = "js_prac_1",
                languageId = ProgrammingLanguageId.JAVASCRIPT,
                type = ChallengeType.PREDICT_OUTPUT,
                title = "Predict Output: Array .map() & .filter()",
                difficulty = "Beginner",
                topicTag = "Arrays",
                prompt = "What does this JavaScript snippet log to the console?",
                starterCode = "const arr = [1, 2, 3, 4];\nconst res = arr.filter(x => x > 2).map(x => x * 10);\nconsole.log(JSON.stringify(res));",
                options = listOf("[30,40]", "[10,20,30,40]", "[3,4]", "[20,30,40]"),
                correctOptionIndex = 0,
                hint = "First filter(x => x > 2) keeps [3, 4], then map multiplies each by 10.",
                explanation = "[3, 4] mapped with x * 10 becomes [30, 40].",
                solutionCode = "const arr = [1, 2, 3, 4];\nconst res = arr.filter(x => x > 2).map(x => x * 10);\nconsole.log(JSON.stringify(res));"
            ),
            PracticeChallenge(
                id = "js_prac_2",
                languageId = ProgrammingLanguageId.JAVASCRIPT,
                type = ChallengeType.FIND_THE_BUG,
                title = "Find the Bug: Constant Reassignment",
                difficulty = "Beginner",
                topicTag = "Variables",
                prompt = "This code tries to increment counter, but counter was declared with 'const'. Change 'const' to 'let' so it logs 15!",
                starterCode = "let counter = 10;\ncounter = counter + 5;\nconsole.log(counter);",
                testCases = listOf(
                    CodingTestCase(input = "", expectedOutput = "15", description = "Outputs 15 without TypeError")
                ),
                hint = "Use 'let counter = 10;' so counter can be updated.",
                explanation = "Variables that get reassigned must be declared with 'let' instead of 'const'.",
                solutionCode = "let counter = 10;\ncounter = counter + 5;\nconsole.log(counter);"
            )
        )

        ProgrammingLanguageId.CPP -> listOf(
            PracticeChallenge(
                id = "cpp_prac_1",
                languageId = ProgrammingLanguageId.CPP,
                type = ChallengeType.FIND_THE_BUG,
                title = "Find the Bug: Sum Loop in C++",
                difficulty = "Beginner",
                topicTag = "C++ Loops",
                prompt = "Compute and print 50 using cout.",
                starterCode = "#include <iostream>\nusing namespace std;\n\nint main() {\n    int x = 25;\n    cout << x * 2 << endl;\n    return 0;\n}",
                testCases = listOf(
                    CodingTestCase(input = "", expectedOutput = "50", description = "Outputs 50")
                ),
                hint = "Multiply x by 2 and stream to cout.",
                explanation = "cout << x * 2 << endl; prints 50.",
                solutionCode = "#include <iostream>\nusing namespace std;\n\nint main() {\n    int x = 25;\n    cout << x * 2 << endl;\n    return 0;\n}"
            )
        )

        else -> listOf(
            PracticeChallenge(
                id = "${lang.key}_prac_1",
                languageId = lang,
                type = ChallengeType.MULTIPLE_CHOICE,
                title = "${lang.displayName} Core Concept Check",
                difficulty = "Beginner",
                topicTag = "Foundations",
                prompt = "Which best describes clean engineering practice in ${lang.displayName}?",
                starterCode = getStarterCode(lang),
                options = listOf(
                    "Writing readable, modular code and testing edge cases",
                    "Ignoring compiler warnings and errors",
                    "Putting all code on a single line",
                    "Skipping input validation"
                ),
                correctOptionIndex = 0,
                hint = "Think about maintainability and correctness.",
                explanation = "Readable, modular code and systematic edge-case testing are universal software engineering foundations.",
                solutionCode = getStarterCode(lang)
            )
        )
    }

    // GUIDED PROJECTS PER LANGUAGE
    fun getProjectsForLanguage(lang: ProgrammingLanguageId): List<CodingProject> = when (lang) {
        ProgrammingLanguageId.PYTHON -> listOf(
            CodingProject(
                id = "py_proj_1",
                languageId = ProgrammingLanguageId.PYTHON,
                title = "Project 1: Smart Study Timer & XP Calculator",
                stage = "Month 1 Mini-Project",
                difficulty = "Beginner",
                description = "Build a Python utility that calculates total study XP and bonus Star Coins from completed sessions.",
                objectives = listOf(
                    "Define variables for sessions_completed and minutes_per_session",
                    "Compute total_minutes and xp_earned (minutes * 2)",
                    "Print the formatted summary report"
                ),
                starterCode = "sessions = 4\nminutes_each = 25\ntotal_minutes = sessions * minutes_each\nxp_earned = total_minutes * 2\nprint(\"Total Study Minutes:\", total_minutes)\nprint(\"XP Earned:\", xp_earned)",
                expectedOutputKeywords = listOf("100", "200"),
                referenceSolution = "sessions = 4\nminutes_each = 25\ntotal_minutes = sessions * minutes_each\nxp_earned = total_minutes * 2\nprint(\"Total Study Minutes:\", total_minutes)\nprint(\"XP Earned:\", xp_earned)"
            ),
            CodingProject(
                id = "py_proj_2",
                languageId = ProgrammingLanguageId.PYTHON,
                title = "Project 2: Automated Quiz Grader & Analytics Engine",
                stage = "Month 2 Practical Project",
                difficulty = "Intermediate",
                description = "Write a modular Python program using functions, lists, and comprehensions to grade student scores and filter honors scholars.",
                objectives = listOf(
                    "Process a list of student quiz scores",
                    "Calculate average score using sum() and len()",
                    "Filter honors scores (>= 85) and print the report"
                ),
                starterCode = "scores = [75, 88, 94, 82, 91]\navg = sum(scores) / len(scores)\nhonors = [s for s in scores if s >= 85]\nprint(\"Average:\", avg)\nprint(\"Honors Scores:\", honors)",
                expectedOutputKeywords = listOf("86.0", "88", "94", "91"),
                referenceSolution = "scores = [75, 88, 94, 82, 91]\navg = sum(scores) / len(scores)\nhonors = [s for s in scores if s >= 85]\nprint(\"Average:\", avg)\nprint(\"Honors Scores:\", honors)"
            ),
            CodingProject(
                id = "py_proj_3",
                languageId = ProgrammingLanguageId.PYTHON,
                title = "Project 3: Object-Oriented Inventory & Order System",
                stage = "Month 3 Capstone Project",
                difficulty = "Advanced",
                description = "Design an OOP class representing an inventory item with methods to compute total order value and verify stock.",
                objectives = listOf(
                    "Define a class with __init__ attributes",
                    "Implement a method returning price * quantity",
                    "Instantiate and print the verified order total"
                ),
                starterCode = "class Order:\n    def __init__(self, item, price, qty):\n        self.item = item\n        self.price = price\n        self.qty = qty\n    def summary(self):\n        print(\"Order:\", self.item, \"Total:\", self.price * self.qty)\n\nord = Order(\"Coding Book\", 35, 3)\nord.summary()",
                expectedOutputKeywords = listOf("Coding Book", "105"),
                referenceSolution = "class Order:\n    def __init__(self, item, price, qty):\n        self.item = item\n        self.price = price\n        self.qty = qty\n    def summary(self):\n        print(\"Order:\", self.item, \"Total:\", self.price * self.qty)\n\nord = Order(\"Coding Book\", 35, 3)\nord.summary()"
            )
        )

        else -> listOf(
            CodingProject(
                id = "${lang.key}_proj_1",
                languageId = lang,
                title = "${lang.displayName} Practical Milestone Project",
                stage = "Core Practical Project",
                difficulty = "Intermediate",
                description = "Build and run a complete working ${lang.displayName} application demonstrating variables, control flow, and clean output.",
                objectives = listOf(
                    "Write valid ${lang.displayName} syntax",
                    "Execute in the sandboxed runtime without errors",
                    "Verify expected output in the console"
                ),
                starterCode = getStarterCode(lang),
                expectedOutputKeywords = listOf("Falcon"),
                referenceSolution = getStarterCode(lang)
            )
        )
    }

    // 90-DAY (THREE-MONTH) LEARNING ROADMAP
    fun getThreeMonthRoadmap(lang: ProgrammingLanguageId): List<RoadmapStage> = listOf(
        RoadmapStage(
            monthNumber = 1,
            title = "Month 1 — Foundations",
            subtitle = "Days 1–30 • Core Syntax, Logic & Simple Programs",
            daysRange = "Days 1–30",
            coreTopics = listOf(
                "How ${lang.displayName} works & executing your first programs",
                "Variables, naming conventions, data types & type conversion",
                "Standard input/output and operators",
                "Conditional statements, loops, and daily beginner coding exercises"
            ),
            milestones = listOf(
                "Write and debug 15+ foundational ${lang.displayName} scripts without syntax errors",
                "Complete Month 1 Foundation Mini-Project"
            ),
            targetLessonCount = 10
        ),
        RoadmapStage(
            monthNumber = 2,
            title = "Month 2 — Practical Programming",
            subtitle = "Days 31–60 • Functions, Data Structures & Debugging",
            daysRange = "Days 31–60",
            coreTopics = listOf(
                "Reusable functions, parameters, return values, and variable scope",
                "Collections & data structures (lists/arrays, maps/dictionaries, strings)",
                "Error handling, defensive programming, and systematic debugging",
                "Modular code organization and intermediate problem-solving"
            ),
            milestones = listOf(
                "Solve multi-test-case coding challenges including edge cases",
                "Build Month 2 Practical Data Processing Project"
            ),
            targetLessonCount = 18
        ),
        RoadmapStage(
            monthNumber = 3,
            title = "Month 3 — Building Real Projects",
            subtitle = "Days 61–90 • Advanced Concepts, Algorithms & Capstone",
            daysRange = "Days 61–90",
            coreTopics = listOf(
                "Object-oriented programming, classes, inheritance & language-specific idioms",
                "Algorithms, time complexity, automated testing & clean code standards",
                "APIs, JSON serialization, and real-world software architecture",
                "Independent capstone project and final practical assessment"
            ),
            milestones = listOf(
                "Pass all module assessments and edge-case test suites",
                "Complete the Month 3 Final Capstone Project"
            ),
            targetLessonCount = 26
        )
    )
}
