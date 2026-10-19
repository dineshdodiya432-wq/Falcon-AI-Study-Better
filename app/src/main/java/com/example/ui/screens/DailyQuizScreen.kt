package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LuminoCatalog
import com.example.data.model.UserProfile
import com.example.ui.theme.CosmicIndigo
import com.example.ui.theme.CosmicViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SolarFlareAmber
import com.example.ui.theme.StarlightGold
import com.example.ui.theme.SupernovaCoral
import com.example.ui.theme.TurquoiseNebula
import com.example.ui.viewmodel.DailyQuizArenaState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyQuizScreen(
    profile: UserProfile,
    quizState: DailyQuizArenaState,
    onSelectGrade: (Int) -> Unit,
    onSelectSubject: (String) -> Unit,
    onSelectDifficulty: (String) -> Unit,
    onSelectQuestionCount: (Int) -> Unit,
    onStartQuiz: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onToggleHint: () -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onOpenGradeLanguageDialog: () -> Unit
) {
    val activeLang = LuminoCatalog.languages.find { it.name == profile.selectedLanguage }
        ?: LuminoCatalog.languages.first()
    val stageDescription = LuminoCatalog.gradeStageDescription(profile.selectedGrade)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("daily_quiz_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header & Grade/Language Status
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.falcon_logo),
                        contentDescription = "Falcon AI Logo",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Falcon AI Quiz Arena",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Grade ${profile.selectedGrade} • $stageDescription",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(
                    onClick = onOpenGradeLanguageDialog,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("quiz_language_grade_button")
                ) {
                    Text("${activeLang.flagEmoji} Gr.${profile.selectedGrade}")
                }
            }
        }

        // 2. Daily Streak Motivation Banner
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SolarFlareAmber.copy(alpha = 0.15f),
                border = BorderStroke(1.5.dp, SolarFlareAmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_streak_motivation_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SolarFlareAmber,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quiz Streak: ${profile.quizStreak} Day(s) ⚡",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SolarFlareAmber
                        )
                        Text(
                            text = LuminoCatalog.getStreakMotivationMessage(profile.quizStreak),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 3. Quiz Customization Builder: Question Count, Difficulty (Easy..Impossible), Subject & Grade
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // A. How Many Questions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = null,
                            tint = TurquoiseNebula,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. How Many Questions?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_question_count_row")
                    ) {
                        items(LuminoCatalog.quizQuestionCounts) { count ->
                            val isSelected = quizState.selectedQuestionCount == count
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectQuestionCount(count) },
                                label = {
                                    Text(
                                        text = "$count Questions",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TurquoiseNebula,
                                    selectedLabelColor = CosmicIndigo
                                ),
                                modifier = Modifier.testTag("quiz_count_chip_$count")
                            )
                        }
                    }

                    // B. Difficulty Selector: Easy, Medium, Hard, Extreme, Impossible
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = SupernovaCoral,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. Select Difficulty (Coins per Correct Answer)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("quiz_difficulty_row")
                    ) {
                        LuminoCatalog.quizDifficulties.forEach { diff ->
                            val isSelected = quizState.selectedDifficulty.equals(diff, ignoreCase = true)
                            val coinRate = LuminoCatalog.coinsPerCorrectForDifficulty(diff)
                            val chipColor = when (diff.lowercase()) {
                                "easy" -> EmeraldSuccess
                                "medium" -> TurquoiseNebula
                                "hard" -> SolarFlareAmber
                                "extreme" -> SupernovaCoral
                                "impossible" -> CosmicViolet
                                else -> CosmicViolet
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectDifficulty(diff) },
                                label = {
                                    Text(
                                        text = "$diff (+$coinRate🪙)",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipColor,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("quiz_difficulty_chip_${diff.lowercase()}")
                            )
                        }
                    }

                    // C. Subject Selector
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = CosmicViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "3. Choose Subject",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LuminoCatalog.quizSubjects.forEach { subject ->
                            val isSelected = quizState.selectedSubjectTag == subject
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectSubject(subject) },
                                label = {
                                    Text(
                                        text = subject,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CosmicViolet,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("quiz_topic_${subject.lowercase().replace(" ", "_")}")
                            )
                        }
                    }

                    // D. Grade Level Selector (1–12)
                    Text(
                        text = "4. Grade Level (1–12)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_grade_selector_row")
                    ) {
                        items((1..12).toList()) { grade ->
                            val isSelected = profile.selectedGrade == grade
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectGrade(grade) },
                                label = {
                                    Text(
                                        text = "Grade $grade",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag("quiz_grade_chip_$grade")
                            )
                        }
                    }

                    Button(
                        onClick = onStartQuiz,
                        enabled = !quizState.isGeneratingQuiz,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CosmicViolet,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_daily_quiz_button")
                    ) {
                        if (quizState.isGeneratingQuiz) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Falcon AI Generating ${quizState.selectedQuestionCount} ${quizState.selectedDifficulty} Questions...")
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Generate ${quizState.selectedQuestionCount} ${quizState.selectedDifficulty} ${quizState.selectedSubjectTag} Questions",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. Active Dynamic Curriculum-Aware Question or Completion Summary
        if (quizState.isQuizFinished) {
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicIndigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quiz_completion_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = StarlightGold.copy(alpha = 0.22f),
                            modifier = Modifier.size(76.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Trophy",
                                    tint = StarlightGold,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        Text(
                            text = "${quizState.selectedDifficulty} Quiz Complete!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "You scored ${quizState.correctCount} / ${quizState.questions.size} in Grade ${profile.selectedGrade} ${quizState.selectedSubjectTag} (${quizState.selectedDifficulty})",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFD5D1FF),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = LuminoCatalog.getStreakMotivationMessage(profile.quizStreak),
                            style = MaterialTheme.typography.bodyLarge,
                            color = StarlightGold,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = StarlightGold.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "🪙 +${quizState.coinsRewarded} Star Coins",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = StarlightGold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SolarFlareAmber.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = SolarFlareAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Streak: ${profile.quizStreak}d",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SolarFlareAmber
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onStartQuiz,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StarlightGold,
                                contentColor = CosmicIndigo
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Play Another ${quizState.selectedDifficulty} Quiz",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else if (quizState.questions.isNotEmpty()) {
            val safeIndex = quizState.currentQuestionIndex.coerceIn(0, quizState.questions.size - 1)
            val currentQ = quizState.questions[safeIndex]
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dynamic_quiz_question_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Curriculum-Aware Standard & Difficulty Header
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CosmicViolet.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, CosmicViolet.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("curriculum_standard_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = CosmicViolet,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = currentQ.curriculumStandard,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = SupernovaCoral.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = quizState.selectedDifficulty.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SupernovaCoral,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question ${safeIndex + 1} of ${quizState.questions.size} • ${quizState.selectedSubjectTag}",
                                style = MaterialTheme.typography.labelLarge,
                                color = CosmicViolet
                            )
                            Text(
                                text = "Score: ${quizState.correctCount}/${quizState.questions.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldSuccess
                            )
                        }

                        LinearProgressIndicator(
                            progress = { (safeIndex + 1).toFloat() / quizState.questions.size.toFloat() },
                            color = CosmicViolet,
                            trackColor = CosmicViolet.copy(alpha = 0.16f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )

                        // Dynamic Curriculum Question Text
                        Text(
                            text = currentQ.question,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.testTag("quiz_question_text")
                        )

                        // Hint button
                        OutlinedButton(
                            onClick = onToggleHint,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("quiz_hint_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = SolarFlareAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (quizState.isHintVisible) "Hide Hint" else "Need a Hint?")
                        }

                        AnimatedVisibility(visible = quizState.isHintVisible) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = StarlightGold.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, SolarFlareAmber),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quiz_hint_card")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = SolarFlareAmber
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = currentQ.hint,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // Multiple-Choice Options
                        currentQ.options.forEachIndexed { idx, optionText ->
                            val isSelected = quizState.selectedOptionIndex == idx
                            val isCorrect = idx == currentQ.correctIndex
                            val borderColor = when {
                                quizState.isAnswerSubmitted && isCorrect -> EmeraldSuccess
                                quizState.isAnswerSubmitted && isSelected && !isCorrect -> SupernovaCoral
                                isSelected -> CosmicViolet
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            }
                            val bgColor = when {
                                quizState.isAnswerSubmitted && isCorrect -> EmeraldSuccess.copy(alpha = 0.16f)
                                quizState.isAnswerSubmitted && isSelected && !isCorrect -> SupernovaCoral.copy(alpha = 0.16f)
                                isSelected -> CosmicViolet.copy(alpha = 0.12f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = bgColor,
                                border = BorderStroke(1.5.dp, borderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !quizState.isAnswerSubmitted) {
                                        onSelectOption(idx)
                                    }
                                    .testTag("quiz_option_$idx")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = borderColor.copy(alpha = 0.25f),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = ('A' + idx).toString(),
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = optionText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (quizState.isAnswerSubmitted && isCorrect) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Correct",
                                            tint = EmeraldSuccess
                                        )
                                    }
                                }
                            }
                        }

                        if (!quizState.isAnswerSubmitted) {
                            Button(
                                onClick = onSubmitAnswer,
                                enabled = quizState.selectedOptionIndex != null,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("submit_quiz_answer_button")
                            ) {
                                Text("Check Answer")
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = TurquoiseNebula.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, TurquoiseNebula),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quiz_step_explanation_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Step-by-Step Explanation (${currentQ.conceptTip})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TurquoiseNebula
                                    )
                                    Text(
                                        text = currentQ.stepByStepExplanation,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            Button(
                                onClick = onNextQuestion,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CosmicViolet,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("next_quiz_question_button")
                            ) {
                                Text(
                                    if (safeIndex + 1 < quizState.questions.size) {
                                        "Next Question (${safeIndex + 2}/${quizState.questions.size})"
                                    } else {
                                        "Finish Quiz & Claim Star Coins"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
