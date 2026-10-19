package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.LuminoCatalog
import com.example.data.model.UserProfile
import com.example.ui.theme.CosmicIndigo
import com.example.ui.theme.CosmicViolet
import com.example.ui.theme.SolarFlareAmber
import com.example.ui.theme.StarlightGold
import com.example.ui.theme.SupernovaCoral
import com.example.ui.theme.TurquoiseNebula

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GradeAndLanguageDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (grade: Int, language: String, tags: List<String>) -> Unit
) {
    var selectedGrade by remember { mutableIntStateOf(profile.selectedGrade) }
    var selectedLanguage by remember { mutableStateOf(profile.selectedLanguage) }
    val selectedTags = remember {
        mutableStateListOf<String>().apply { addAll(profile.curriculumTags) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Grade & Language",
                            tint = CosmicViolet
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Grade (1–12) & Language",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog"
                        )
                    }
                }

                Text(
                    text = "1. Select Your Grade Level (Tailors AI Tone & Quiz Rigor)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (grade in 1..12) {
                        val isSelected = selectedGrade == grade
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGrade = grade },
                            label = {
                                Text(
                                    text = "Grade $grade",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CosmicViolet,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("grade_chip_$grade")
                        )
                    }
                }

                Text(
                    text = "2. Choose Explanation & Quiz Language (14 Global Languages)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                val groupedLanguages = LuminoCatalog.languages.groupBy { it.regionGroup }
                groupedLanguages.forEach { (region, langs) ->
                    Text(
                        text = region,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        langs.forEach { lang ->
                            val isSelected = selectedLanguage == lang.name
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) TurquoiseNebula.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) BorderStroke(2.dp, TurquoiseNebula) else null,
                                modifier = Modifier
                                    .clickable { selectedLanguage = lang.name }
                                    .testTag("lang_chip_${lang.name.lowercase().replace(" ", "_")}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = lang.flagEmoji)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = lang.nativeLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = TurquoiseNebula,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "3. Curriculum Focus Tags (Recommended for Grade $selectedGrade)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LuminoCatalog.curriculumTopics.forEach { topic ->
                        val isGradeMatch = selectedGrade in topic.minGrade..topic.maxGrade
                        val isSelected = selectedTags.contains(topic.tag)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected && selectedTags.size > 1) {
                                    selectedTags.remove(topic.tag)
                                } else if (!isSelected) {
                                    selectedTags.add(topic.tag)
                                }
                            },
                            label = {
                                Text(
                                    text = if (isGradeMatch) "★ ${topic.tag}" else topic.tag
                                )
                            }
                        )
                    }
                }

                Button(
                    onClick = {
                        onSave(selectedGrade, selectedLanguage, selectedTags.toList())
                        onDismiss()
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_grade_language_button")
                ) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Apply Grade $selectedGrade • $selectedLanguage",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun StreakRecoveryVaultDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onRecoverStudyStreak: () -> Unit,
    onRecoverQuizStreak: () -> Unit,
    onBuyShield: () -> Unit,
    onSimulateMissedDay: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak Vault",
                            tint = SolarFlareAmber
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cosmic Streak Recovery Vault",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StarlightGold.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Available Star Coins:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "🪙 ${profile.coins} Coins",
                            style = MaterialTheme.typography.titleLarge,
                            color = SolarFlareAmber
                        )
                    }
                }

                Text(
                    text = "Missed a study day or daily quiz? Spend your earned Star Coins to restore your streak with a Cosmic Time-Warp or equip a Streak Freeze Shield in advance!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (profile.brokenStudyStreakToRecover > 0) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SupernovaCoral.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, SupernovaCoral),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Broken Study Streak: ${profile.brokenStudyStreakToRecover} Days!",
                                style = MaterialTheme.typography.titleMedium,
                                color = SupernovaCoral
                            )
                            Button(
                                onClick = onRecoverStudyStreak,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SolarFlareAmber,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recover_study_streak_button")
                            ) {
                                Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Recover ${profile.brokenStudyStreakToRecover}-Day Study Streak (75 Coins)")
                            }
                        }
                    }
                }

                if (profile.brokenQuizStreakToRecover > 0) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SupernovaCoral.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, SupernovaCoral),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Broken Quiz Streak: ${profile.brokenQuizStreakToRecover} Days!",
                                style = MaterialTheme.typography.titleMedium,
                                color = SupernovaCoral
                            )
                            Button(
                                onClick = onRecoverQuizStreak,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CosmicViolet,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recover_quiz_streak_button")
                            ) {
                                Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Recover ${profile.brokenQuizStreakToRecover}-Day Quiz Streak (75 Coins)")
                            }
                        }
                    }
                }

                Button(
                    onClick = onBuyShield,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TurquoiseNebula,
                        contentColor = CosmicIndigo
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("buy_streak_shield_button")
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Buy Streak Freeze Shield (75 Coins) • Owned: ${profile.streakShields}",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onSimulateMissedDay,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_missed_streak_button")
                ) {
                    Text("Simulate Missed Day (Test Streak Recovery)")
                }
            }
        }
    }
}
