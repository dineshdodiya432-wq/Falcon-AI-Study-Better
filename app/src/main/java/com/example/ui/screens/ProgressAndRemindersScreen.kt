package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LuminoCatalog
import com.example.data.model.QuizAttempt
import com.example.data.model.UserProfile
import com.example.ui.theme.CosmicViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SolarFlareAmber
import com.example.ui.theme.StarlightGold
import com.example.ui.theme.SupernovaCoral
import com.example.ui.theme.TurquoiseNebula
import com.example.util.StudyReminderHelper
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressAndRemindersScreen(
    profile: UserProfile,
    quizAttempts: List<QuizAttempt>,
    isDarkMode: Boolean,
    sessionSubtitle: String,
    onToggleDarkMode: (Boolean) -> Unit,
    onOpenGradeLanguageDialog: () -> Unit,
    onUpdateReminder: (enabled: Boolean, hour: Int, minute: Int, days: List<String>) -> Unit,
    onOpenStreakVault: () -> Unit,
    onSignOut: () -> Unit,
    onShowNotificationStatus: (String) -> Unit,
    onRecoverStudyStreak: () -> Unit = {},
    onRecoverQuizStreak: () -> Unit = {},
    onBuyShield: () -> Unit = {},
    onSimulateMissedDay: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentEmail = remember(sessionSubtitle) {
        FirebaseAuth.getInstance().currentUser?.email ?: sessionSubtitle
    }
    var reminderEnabled by remember(profile.reminderEnabled) { mutableStateOf(profile.reminderEnabled) }
    var reminderHour by remember(profile.reminderHour) { mutableIntStateOf(profile.reminderHour) }
    var reminderMinute by remember(profile.reminderMinute) { mutableIntStateOf(profile.reminderMinute) }
    val selectedDays = remember(profile.reminderDays) {
        mutableStateListOf<String>().apply { addAll(profile.reminderDays) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val sent = StudyReminderHelper.sendTestStudyReminderNotification(
                context = context,
                grade = profile.selectedGrade,
                language = profile.selectedLanguage,
                studyStreak = profile.studyStreak
            )
            onShowNotificationStatus(
                if (sent) "Study Reminder notification sent!" else "Could not post notification."
            )
        } else {
            onShowNotificationStatus("Notification permission declined; in-app reminders remain active.")
        }
    }

    val allWeekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("progress_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings, Progress & Reminders",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Customize Dark Mode, manage your session or Gmail, configure study reminders, and view badges.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Appearance & Dark Mode Settings Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_appearance_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                shape = CircleShape,
                                color = CosmicViolet.copy(alpha = 0.16f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = "Dark Mode",
                                        tint = if (isDarkMode) StarlightGold else CosmicViolet
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Dark Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isDarkMode) {
                                        "Cosmic Night Theme Active"
                                    } else {
                                        "Starlight Day Theme Active"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = onToggleDarkMode,
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }

                    // Grade & Language Setting Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Grade & Explanation Language",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Grade ${profile.selectedGrade} • ${profile.selectedLanguage}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenGradeLanguageDialog,
                            modifier = Modifier.testTag("settings_grade_language_button")
                        ) {
                            Icon(imageVector = Icons.Default.Language, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change")
                        }
                    }

                    // Session / Account Status Row (Guest Mode or Gmail)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Session Status",
                                    tint = TurquoiseNebula,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = profile.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = currentEmail,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onSignOut,
                                modifier = Modifier.testTag("settings_sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Switch Mode",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Switch")
                            }
                        }
                    }
                }
            }
        }

        // 2. Stats Summary Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    title = "Star Coins",
                    value = "🪙 ${profile.coins}",
                    accent = StarlightGold,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Total XP",
                    value = "✨ ${profile.xp}",
                    accent = CosmicViolet,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Badges",
                    value = "🏅 ${profile.unlockedBadges.size}/${LuminoCatalog.badges.size}",
                    accent = TurquoiseNebula,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. User Profile Streak Recovery UI Component (Spend Coins to Recover Missed Streaks)
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.5.dp, SolarFlareAmber.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_streak_recovery_component")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = SolarFlareAmber.copy(alpha = 0.18f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = null,
                                        tint = SolarFlareAmber,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Streak Recovery & Coin Vault",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Spend earned coins to resurrect missed streaks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = StarlightGold.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, StarlightGold)
                        ) {
                            Text(
                                text = "🪙 ${profile.coins} Coins",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = StarlightGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Current Streak & Shields Status Overview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Study Streak", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${profile.studyStreak} Days 🔥",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SolarFlareAmber
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Quiz Streak", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${profile.quizStreak} Days ⚡",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TurquoiseNebula
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Active Shields", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${profile.streakShields} 🛡️",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }

                    // Recovery Actions when a streak was missed
                    if (profile.brokenStudyStreakToRecover > 0) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SupernovaCoral.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, SupernovaCoral),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Missed Study Day: ${profile.brokenStudyStreakToRecover}-Day Streak Lost!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SupernovaCoral
                                )
                                Text(
                                    text = "Spend 75 of your Star Coins to time-warp back and fully restore your ${profile.brokenStudyStreakToRecover}-day study streak.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onRecoverStudyStreak,
                                    enabled = profile.coins >= 75,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SolarFlareAmber,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("profile_recover_study_streak_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (profile.coins >= 75) {
                                            "Spend 75 Coins to Recover Study Streak 🪙"
                                        } else {
                                            "Need 75 Coins (Have ${profile.coins} 🪙)"
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (profile.brokenQuizStreakToRecover > 0) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SupernovaCoral.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, SupernovaCoral),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Missed Quiz Day: ${profile.brokenQuizStreakToRecover}-Day Streak Lost!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SupernovaCoral
                                )
                                Text(
                                    text = "Spend 75 of your Star Coins to restore your ${profile.brokenQuizStreakToRecover}-day quiz streak.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onRecoverQuizStreak,
                                    enabled = profile.coins >= 75,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CosmicViolet,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("profile_recover_quiz_streak_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (profile.coins >= 75) {
                                            "Spend 75 Coins to Recover Quiz Streak 🪙"
                                        } else {
                                            "Need 75 Coins (Have ${profile.coins} 🪙)"
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // When streaks are safe
                    if (profile.brokenStudyStreakToRecover <= 0 && profile.brokenQuizStreakToRecover <= 0) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = EmeraldSuccess.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "All streaks are active and burning bright! Earn coins every day to keep your safety buffer loaded.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }

                    // Spend coins to buy a Streak Freeze Shield
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onBuyShield,
                            enabled = profile.coins >= 75,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TurquoiseNebula,
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_buy_shield_button")
                        ) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buy Shield (75 🪙)")
                        }

                        OutlinedButton(
                            onClick = onSimulateMissedDay,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_simulate_missed_day_button")
                        ) {
                            Text("Simulate Missed Day")
                        }
                    }
                }
            }
        }

        // 4. Configurable Study Reminders Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = CosmicViolet
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Daily Study Reminders",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            modifier = Modifier.testTag("reminder_enable_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reminder Time: %02d:%02d".format(reminderHour, reminderMinute),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { reminderHour = (reminderHour + 23) % 24 }
                            ) {
                                Text("-1h")
                            }
                            OutlinedButton(
                                onClick = { reminderHour = (reminderHour + 1) % 24 }
                            ) {
                                Text("+1h")
                            }
                            OutlinedButton(
                                onClick = { reminderMinute = (reminderMinute + 15) % 60 }
                            ) {
                                Text("+15m")
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allWeekDays.forEach { day ->
                            val isSelected = selectedDays.contains(day)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected && selectedDays.size > 1) {
                                        selectedDays.remove(day)
                                    } else if (!isSelected) {
                                        selectedDays.add(day)
                                    }
                                },
                                label = { Text(day) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onUpdateReminder(
                                    reminderEnabled,
                                    reminderHour,
                                    reminderMinute,
                                    selectedDays.toList()
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_reminder_schedule_button")
                        ) {
                            Text("Save Schedule")
                        }
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val sent = StudyReminderHelper.sendTestStudyReminderNotification(
                                        context = context,
                                        grade = profile.selectedGrade,
                                        language = profile.selectedLanguage,
                                        studyStreak = profile.studyStreak
                                    )
                                    onShowNotificationStatus(
                                        if (sent) "Study Reminder notification sent!" else "Reminder preview triggered!"
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_reminder_notification_button")
                        ) {
                            Text("Test Alert Now")
                        }
                    }
                }
            }
        }

        // 5. Collectible Achievements
        item {
            Text(
                text = "Falcon Achievements & Badges",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(LuminoCatalog.badges, key = { it.id }) { badge ->
            val isUnlocked = profile.unlockedBadges.contains(badge.id)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnlocked) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                ),
                border = if (isUnlocked) BorderStroke(1.5.dp, StarlightGold) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isUnlocked) StarlightGold.copy(alpha = 0.22f) else Color.Gray.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                                contentDescription = badge.title,
                                tint = if (isUnlocked) SolarFlareAmber else Color.Gray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = badge.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = badge.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isUnlocked) EmeraldSuccess.copy(alpha = 0.16f) else StarlightGold.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isUnlocked) "Unlocked" else "+${badge.rewardCoins} 🪙",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isUnlocked) EmeraldSuccess else SolarFlareAmber,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 6. Recent Quiz Attempts History
        if (quizAttempts.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Quiz History",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(quizAttempts.take(5), key = { it.id.ifEmpty { it.subjectTag + it.score } }) { attempt ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${attempt.subjectTag} • Grade ${attempt.gradeLevel} (${attempt.language})",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Score: ${attempt.score}/${attempt.totalQuestions}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "+${attempt.coinsEarned} 🪙",
                                style = MaterialTheme.typography.titleMedium,
                                color = SolarFlareAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
