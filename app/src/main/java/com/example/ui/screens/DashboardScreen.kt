package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.example.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HomeworkSession
import com.example.data.model.LuminoCatalog
import com.example.data.model.UserProfile
import com.example.ui.theme.CosmicIndigo
import com.example.ui.theme.CosmicViolet
import com.example.ui.theme.DeepSpaceBackground
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SolarFlareAmber
import com.example.ui.theme.StarlightGold
import com.example.ui.theme.SupernovaCoral
import com.example.ui.theme.TurquoiseNebula
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    profile: UserProfile,
    homeworkSessions: List<HomeworkSession>,
    onOpenGradeLanguageDialog: () -> Unit,
    onOpenStreakVault: () -> Unit,
    onClaimDailyStreak: () -> Unit = {},
    onOpenDailyCheckIn: () -> Unit = {},
    onNavigateToHomework: (prefillPrompt: String?) -> Unit,
    onSendDirectHomeworkPrompt: (prompt: String) -> Unit = {},
    onNavigateToCoding: () -> Unit = {},
    onNavigateToQuiz: (subjectTag: String?) -> Unit,
    onNavigateToProgress: () -> Unit,
    onSelectHomeworkSession: (HomeworkSession) -> Unit,
    onSignOut: () -> Unit
) {
    val activeLang = LuminoCatalog.languages.find { it.name == profile.selectedLanguage }
        ?: LuminoCatalog.languages.first()
    var quickPrompt by rememberSaveable { mutableStateOf("") }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val isStreakClaimedToday = profile.lastStreakCheckInDate == today
    val nextStreakDay = if (isStreakClaimedToday) profile.studyStreak else profile.studyStreak + 1
    val nextRewardCoins = LuminoCatalog.getDailyStreakRewardCoins(nextStreakDay)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Falcon AI Hero Banner
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicIndigo),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(DeepSpaceBackground, CosmicIndigo, Color(0xFF392B85))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, TurquoiseNebula.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .clickable { onOpenGradeLanguageDialog() }
                                    .testTag("grade_language_selector_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = activeLang.flagEmoji)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Grade ${profile.selectedGrade} • ${profile.selectedLanguage}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "Change Grade or Language",
                                        tint = TurquoiseNebula,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = StarlightGold.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, StarlightGold),
                                    modifier = Modifier
                                        .clickable { onOpenStreakVault() }
                                        .testTag("coin_balance_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🪙 ${profile.coins}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = StarlightGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = onSignOut,
                                    modifier = Modifier.testTag("sign_out_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Sign Out",
                                        tint = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.falcon_logo),
                                contentDescription = "Falcon AI Logo",
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = activeLang.greeting,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = activeLang.dashboardSubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFD5D1FF)
                                )
                            }
                        }

                        // Dual Streaks & Shield Row (Tapping opens Daily Check-In Screen)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StreakBadgePill(
                                label = "Study Streak",
                                value = "${profile.studyStreak}d 🔥",
                                accentColor = SolarFlareAmber,
                                onClick = onOpenDailyCheckIn,
                                modifier = Modifier.weight(1f)
                            )
                            StreakBadgePill(
                                label = "Quiz Streak",
                                value = "${profile.quizStreak}d ⚡",
                                accentColor = TurquoiseNebula,
                                onClick = onOpenDailyCheckIn,
                                modifier = Modifier.weight(1f)
                            )
                            StreakBadgePill(
                                label = "Shields",
                                value = "${profile.streakShields} 🛡️",
                                accentColor = StarlightGold,
                                onClick = onOpenStreakVault,
                                modifier = Modifier.weight(0.85f)
                            )
                        }
                    }
                }
            }
        }

        // 2. Dedicated Daily Streak Tracker & Coin Reward Card
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.5.dp, SolarFlareAmber.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_streak_tracker_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header with Streak Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = SolarFlareAmber,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Daily Streak Tracker",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Current: Day ${profile.studyStreak} Streak 🔥",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SolarFlareAmber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = StarlightGold.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "🪙 ${profile.coins} Coins",
                                style = MaterialTheme.typography.labelMedium,
                                color = SolarFlareAmber,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Daily Congratulatory Banner
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SolarFlareAmber.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, SolarFlareAmber.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = LuminoCatalog.getStreakCongratulatoryTitle(profile.studyStreak),
                                style = MaterialTheme.typography.titleSmall,
                                color = SolarFlareAmber,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = LuminoCatalog.getStreakMotivationMessage(profile.studyStreak),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // 7-Day Visual Journey Row
                    Text(
                        text = "7-Day Streak Rewards Calendar",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items((1..7).toList()) { day ->
                            val isCompleted = day <= profile.studyStreak
                            val isCurrent = day == profile.studyStreak
                            val dayReward = LuminoCatalog.getDailyStreakRewardCoins(day)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = when {
                                    isCurrent -> SolarFlareAmber
                                    isCompleted -> EmeraldSuccess.copy(alpha = 0.18f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrent) SolarFlareAmber else if (isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.width(62.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Day $day",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Completed",
                                            tint = if (isCurrent) Color.White else EmeraldSuccess,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "+$dayReward🪙",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SolarFlareAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action Buttons: Claim Daily Streak Coins, Check-In Screen & Coin-based Streak Recovery
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onClaimDailyStreak,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStreakClaimedToday) EmeraldSuccess else SolarFlareAmber,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("claim_daily_streak_button")
                        ) {
                            Icon(
                                imageVector = if (isStreakClaimedToday) Icons.Default.Check else Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isStreakClaimedToday) "Claimed Today" else "Claim +$nextRewardCoins 🪙",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenStreakVault,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("recover_streak_with_coins_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                tint = TurquoiseNebula,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Recover (75🪙)")
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenDailyCheckIn,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_daily_checkin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = SolarFlareAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Full Daily Check-In Screen 📅",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 3. Instant Ask Bar on Home Screen
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, CosmicViolet.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CosmicViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Falcon AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { onNavigateToHomework(null) },
                            modifier = Modifier
                                .size(46.dp)
                                .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                                .testTag("dashboard_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Camera Homework Solver",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = quickPrompt,
                            onValueChange = { quickPrompt = it },
                            placeholder = { Text("Message Falcon AI...") },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    val q = quickPrompt.trim()
                                    if (q.isNotEmpty()) {
                                        quickPrompt = ""
                                        onSendDirectHomeworkPrompt(q)
                                    } else {
                                        onNavigateToHomework(null)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicViolet,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_quick_prompt_input")
                        )

                        IconButton(
                            onClick = { onNavigateToHomework(null) },
                            modifier = Modifier
                                .size(46.dp)
                                .background(color = SupernovaCoral.copy(alpha = 0.14f), shape = CircleShape)
                                .testTag("dashboard_mic_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Dictation",
                                tint = SupernovaCoral
                            )
                        }

                        IconButton(
                            onClick = {
                                val q = quickPrompt.trim()
                                if (q.isNotEmpty()) {
                                    quickPrompt = ""
                                    onSendDirectHomeworkPrompt(q)
                                } else {
                                    onNavigateToHomework(null)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    brush = Brush.linearGradient(colors = listOf(CosmicViolet, TurquoiseNebula)),
                                    shape = CircleShape
                                )
                                .testTag("dashboard_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 4. Quick Launch Cards
        item {
            Text(
                text = "Learning Hub (Grade ${profile.selectedGrade})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Falcon Chat",
                    subtitle = "Smart homework & question solver",
                    badgeText = "+20 Coins",
                    icon = Icons.Default.AutoAwesome,
                    accentColor = CosmicViolet,
                    onClick = { onNavigateToHomework(null) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_homework")
                )
                QuickActionCard(
                    title = "Quiz Arena",
                    subtitle = "Questions, 5 Difficulties & Coins",
                    badgeText = "Up to +65🪙/Q",
                    icon = Icons.Default.Quiz,
                    accentColor = TurquoiseNebula,
                    onClick = { onNavigateToQuiz(profile.curriculumTags.firstOrNull()) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_quiz")
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            QuickActionCard(
                title = "Falcon Coding Academy & IDE",
                subtitle = "Python (26 modules), JS, C++, Java, C, HTML/CSS • Live Code Runner & AI Tutor",
                badgeText = "Interactive IDE",
                icon = Icons.Default.Code,
                accentColor = EmeraldSuccess,
                onClick = onNavigateToCoding,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_action_coding")
            )
        }

        // 5. Daily Quest & Topics
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val progressFraction = ((profile.xp % 200) / 200f).coerceIn(0.15f, 1f)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(68.dp)
                        ) {
                            Canvas(modifier = Modifier.size(64.dp)) {
                                drawArc(
                                    color = CosmicViolet.copy(alpha = 0.16f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = EmeraldSuccess,
                                    startAngle = -90f,
                                    sweepAngle = 360f * progressFraction,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "Lv.${(profile.xp / 200) + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Falcon Scholar • ${profile.xp} XP",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap any topic below to ask Falcon AI:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val recommendedTopics = LuminoCatalog.curriculumTopics.filter {
                            profile.selectedGrade in it.minGrade..it.maxGrade || profile.curriculumTags.contains(it.tag)
                        }
                        recommendedTopics.forEach { topic ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = CosmicViolet.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, CosmicViolet.copy(alpha = 0.35f)),
                                modifier = Modifier.clickable {
                                    onSendDirectHomeworkPrompt(topic.samplePrompt)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = CosmicViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = topic.tag,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Recent Chat Conversations
        item {
            Text(
                text = "Recent Chats",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(homeworkSessions.take(5), key = { it.id.ifEmpty { it.questionTitle } }) { session ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectHomeworkSession(session) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CosmicViolet.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "Grade ${session.gradeLevel}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CosmicViolet,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (session.subjectTag.isNotBlank() && session.subjectTag != "General") {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = TurquoiseNebula.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = session.subjectTag,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TurquoiseNebula,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = session.questionTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = session.displayParagraph,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open chat",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakBadgePill(
    label: String,
    value: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.16f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = StarlightGold.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelLarge,
                        color = SolarFlareAmber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
