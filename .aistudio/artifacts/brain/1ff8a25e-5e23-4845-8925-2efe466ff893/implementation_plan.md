# LuminoAI — Multilingual Cosmic AI Tutor & Learning Companion

LuminoAI is a vibrant, multilingual Android learning application for students in Grades 1 through 12 that turns homework help and daily study into an engaging cosmic adventure. Students can snap or upload homework photos, ask questions in 14+ global languages, receive grade-tailored step-by-step AI explanations, take adaptive daily quizzes, track study and quiz streaks, and use earned coins to recover missed streaks.

## User Review & Critical Decisions

> [!IMPORTANT]
> Review the confirmed product decisions below before we begin building and provisioning the backend services.

- **Confirmed Decision 1 — Grade-by-Grade Personalization (Grades 1–12)**: Students can select their exact grade level (Grade 1 through Grade 12) alongside curriculum tags (e.g., *Number Sense*, *Fractions*, *Algebra*, *Geometry*, *Life Science*, *Physics*, *World History*, *Reading & Grammar*, *Coding*). All AI homework explanations and daily quizzes dynamically adapt vocabulary, metaphors, and rigor to the selected grade.
- **Confirmed Decision 2 — Global Multilingual Support (14 Languages)**: Prominent support for **Simple English**, **European** (*Spanish, French, German, Portuguese*), **South Asian** (*Hindi, Bengali, Tamil, Telugu, Marathi*), and **East Asian & Middle Eastern** (*Mandarin, Japanese, Korean, Arabic* with RTL layout support) across explanations, quizzes, and key interface controls.
- **Confirmed Decision 3 — Vibrant Cosmic Explorer Aesthetic**: Deep cosmic indigo headers, warm starlight surface cards, glowing amber/coral streak indicators, collectible cosmic achievement badges, and friendly geometric typography.
- **Decision Gate — Cloud Persistence & AI Provisioning**: Upon plan approval, we will provision **Firebase Authentication (Google Sign-In)** and **Cloud Firestore** to securely sync student progress, streaks, coins, homework history, and achievements across devices, paired with **Gemini API (`gemini-3.5-flash` & `gemini-3.1-pro-preview`)** for multimodal homework analysis and dynamic quiz generation.

---

## 1. Overview & Core Concept

- **What It Does**: Provides an all-in-one AI study companion where students upload homework images or ask questions, get structured step-by-step guidance in their preferred language, complete daily adaptive quizzes, manage study reminders, and stay motivated through streaks, coins, and a coin-based streak recovery shop.
- **Target Audience / Persona**: Children and students across Grades 1–12 (ages 6–18) and multilingual families seeking accessible, jargon-free homework breakdowns in their native language or Simple English.
- **Key Value**: Removes language barriers and grade-level mismatch in education while replacing streak anxiety with an encouraging coin-based streak recovery system.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Onboarding & Cosmic Profile Setup**:
   - Student signs in with Google Sign-In (`CredentialManager`), selects their **Grade (1–12)**, preferred **Explanation Language** (from 14 supported languages), and favorite **Curriculum Tags**.
2. **Engaging Cosmic Dashboard**:
   - Displays the student's Cosmic Explorer rank, active Grade & Language pills (one-tap switchable), **Study Streak** & **Quiz Streak** counters, **Star Coin** balance, and a **Streak Recovery / Shield Card** when a streak needs protection or recovery.
   - Features a Daily Learning Quest ring, quick-launch cards for *Snap Homework*, *Ask AI Tutor*, and *Daily Quiz*, plus a live **Study Reminder** toggle card.
3. **Homework Upload & Step-by-Step AI Tutor**:
   - Students pick a homework photo using the zero-permission Android Photo Picker (`PickVisualMedia`), draw/select a subject tag, or type a question.
   - Gemini analyzes the problem and returns a structured **Step-by-Step Breakdown** in the chosen language and grade level:
     - *Problem Summary*
     - *Interactive Step-by-Step Cards* (with "Reveal Next Step" or "Show All Steps" modes so students learn actively rather than just copying)
     - *Key Concept & Vocabulary Spotlight* (bilingual terms in the selected language + Simple English)
     - *Quick Check Mini-Question* (awards +15 Star Coins upon completion and increments the Daily Study Streak).
4. **Adaptive Daily Quiz & Streak Engine**:
   - Generates a 5-question interactive quiz tailored to the student's Grade (1–12), selected curriculum tag, and language.
   - Provides immediate encouraging feedback, step-by-step explanations for every answer, and a celebratory completion summary awarding Star Coins, XP, and incrementing the **Quiz Streak**.
5. **Coin-Based Streak Recovery & Cosmic Vault**:
   - If a student misses a day of study or quizzes, the **Cosmic Time-Warp Recovery** banner lets them spend earned Star Coins (e.g., 100 Coins) to restore their broken Study or Quiz Streak, or purchase a **Streak Freeze Shield** (75 Coins) in advance.
6. **Progress Analytics, Achievements & Study Reminders**:
   - Visualizes weekly study minutes, quiz accuracy by curriculum tag, unlocked Cosmic Badges (*First Launch*, *7-Day Supernova*, *Polyglot Scholar*, *Homework Hero*, *Quiz Master*), and configurable daily study reminder schedules (time picker + day-of-week selector).

### Visual Identity & Theme
- **Aesthetic Direction**: *Vibrant Cosmic Explorer* — imaginative, warm, and structured with high-contrast readability for young learners and teens alike.
- **Color Palette & Mood**:
  - `--background`: Deep Starlight Cream (`#FAF8FF`) in Light Mode / Deep Space Indigo (`#0F1026`) in Dark Mode
  - `--surface-card`: Warm Cosmic White (`#FFFFFF`) / Nebula Slate (`#1B1E3C`) with subtle tonal elevation
  - `--primary`: Electric Cosmic Violet (`#5B4DFF`) for primary actions and active states
  - `--secondary`: Turquoise Nebula (`#00C4B4`) for multilingual & concept highlights
  - `--accent-streak`: Solar Flare Amber (`#FF8A00`) & Supernova Coral (`#FF5252`) for streaks and badges
  - `--accent-coin`: Starlight Gold (`#FFC82C`) for coin balances and streak recovery actions
- **Typography & Hierarchy**:
  - **Display / Headings**: `Fredoka` (bundled locally via `font-util`) for friendly, rounded, approachable headers, badge titles, and streak counters.
  - **Body / Explanations**: `Plus Jakarta Sans` (bundled locally via `font-util`) for crisp readability across multilingual scripts and mathematical steps.
- **Interactive Feedback & Motion**:
  - Spring-animated step reveal cards, animated progress rings on the dashboard, celebratory coin burst banners when completing homework or quizzes, and instant ripple feedback on all interactive chips.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Active Step-by-Step Scaffolding vs. Single Wall of Text**
  - *Chosen Approach*: Structured JSON output from Gemini that separates responses into *Summary*, *Ordered Steps*, *Key Concept*, and a *Practice Check Question*.
  - *Why*: Children learn better when complex homework problems are broken into digestible cards they can step through at their own pace, with a mini comprehension check at the end to earn coins.
- **Decision 2: Dual Streak System + Coin-Based Streak Recovery**
  - *Chosen Approach*: Track both **Study Streak** (homework & tutor sessions) and **Quiz Streak** (daily quizzes) alongside a **Streak Recovery / Time-Warp** mechanic funded by earned Star Coins (plus a "Simulate Missed Day" test action in the Streak Modal so users can immediately test and experience coin-based streak recovery).
  - *Why*: Directly fulfills the requirement for study/quiz streaks and coin-based streak recovery while making the recovery flow immediately verifiable in the emulator.
- **Decision 3: Cloud Firestore + Google Sign-In Persistence**
  - *Chosen Approach*: Authenticate via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`) and store student profiles, homework sessions, quiz results, streaks, and reminders in a named Cloud Firestore database (`R.string.firestore_database_id`).
  - *Why*: Ensures a student's hard-earned coins, streaks, and saved homework explanations survive app reinstalls and sync across family tablets and phones.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram
```
┌────────────────────────────────────────────────────────────────────────────┐
│                        MainActivity (Edge-to-Edge)                         │
│             AuthGate: SignInScreen (Google CredentialManager)              │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │ Authenticated UID
                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                   LuminoAppShell (Adaptive Navigation)                     │
│  ┌──────────────────┬──────────────────┬────────────────┬───────────────┐  │
│  │ Cosmic Dashboard │  Homework & AI   │  Daily Quiz    │ Progress,     │  │
│  │ (Grade/Lang Bar, │  Tutor Studio    │  Arena         │ Badges &      │  │
│  │ Streaks, Coins,  │  (Photo Picker,  │  (5-Q Adaptive │ Reminders     │  │
│  │ Recovery Modal)  │  Step-by-Step)   │  Quiz & XP)    │ (Schedules)   │  │
│  └────────┬─────────┴────────┬─────────┴────────┬───────┴───────┬───────┘  │
└───────────┼──────────────────┼──────────────────┼───────────────┼──────────┘
            │                  │                  │               │
            ▼                  ▼                  ▼               ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                    LuminoViewModel (StateFlow + MVVM)                      │
└──────────────────────┬─────────────────────────────────┬───────────────────┘
                       │                                 │
                       ▼                                 ▼
┌─────────────────────────────────────────┐ ┌────────────────────────────────┐
│       LuminoFirestoreRepository         │ │       GeminiTutorService       │
│  • /users/{uid} (Profile, Coins,        │ │  • Multimodal Homework Solver  │
│    Grade 1-12, Language, Streaks,       │ │    (Bitmap Base64 + Prompt)    │
│    Shields, Achievements, Reminders)    │ │  • Structured Step-by-Step     │
│  • /users/{uid}/homework/{id}           │ │    JSON Explanations           │
│  • /users/{uid}/quizzes/{id}            │ │  • Adaptive Grade 1-12 Quiz    │
│  • Real-time snapshots() + Rules        │ │    Generator (14 Languages)    │
└─────────────────────────────────────────┘ └────────────────────────────────┘
```

### Data Model & State
- **UserProfile (`/users/{userId}`)**:
  - `userId`, `displayName`, `selectedGrade` (1–12), `selectedLanguage` (14 supported languages), `curriculumTags` (list of active subjects), `coins` (Int), `xp` (Int), `studyStreak` (Int), `quizStreak` (Int), `brokenStudyStreakToRecover` (Int), `brokenQuizStreakToRecover` (Int), `streakShields` (Int), `unlockedBadges` (List<String>), `reminderEnabled` (Boolean), `reminderHour` (Int), `reminderMinute` (Int), `reminderDays` (List<String>), `lastStudyDate` (String), `lastQuizDate` (String), `createdAt`, `updatedAt`.
- **HomeworkSession (`/users/{userId}/homework/{sessionId}`)**:
  - `userId`, `questionTitle`, `subjectTag`, `gradeLevel`, `language`, `steps` (List<String>), `finalAnswer`, `keyConcept`, `practiceQuestion`, `practiceOptions`, `practiceCorrectIndex`, `coinsEarned`, `createdAt`.
- **QuizAttempt (`/users/{userId}/quizzes/{quizId}`)**:
  - `userId`, `subjectTag`, `gradeLevel`, `language`, `score`, `totalQuestions`, `coinsEarned`, `createdAt`.

### Interactive Component & State Mapping
- **Grade (1–12) & 14-Language Selector Sheet**: Accessible from the top bar on any screen; updates `UserProfile` in Firestore and immediately adapts subsequent AI explanations, quizzes, and localized UI strings (including RTL layout direction when Arabic is selected).
- **Homework Photo Picker & Stepper**: Uses `rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia())` to load a homework image, converts to Base64 JPEG, sends to Gemini with grade and language instructions, and renders interactive step cards plus a bonus coin comprehension check.
- **Streak Recovery & Shield Shop Modal**: Allows students to spend Star Coins to buy a Streak Shield, recover a broken Study/Quiz Streak using coins, or trigger a safe "Simulate Missed Streak" action to test the recovery workflow on demand.
- **Study Reminder Scheduler**: Interactive time picker and day-of-week chips that persist reminder preferences to Firestore and schedule/trigger local study reminder notifications.
