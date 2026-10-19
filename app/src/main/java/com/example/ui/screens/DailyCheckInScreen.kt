package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyCheckInScreen(
    profile: UserProfile,
    onBack: () -> Unit,
    onClaimCoins: () -> Unit,
    onOpenStreakVault: () -> Unit,
    onNavigateToQuiz: () -> Unit = {},
    onNavigateToHomework: () -> Unit = {}
) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val isClaimedToday = profile.lastStreakCheckInDate == today
    val currentStreak = profile.studyStreak
    val nextStreakDay = if (isClaimedToday) currentStreak else currentStreak + 1
    val coinsToAward = LuminoCatalog.getDailyStreakRewardCoins(nextStreakDay)

    val congratulatoryTitle = LuminoCatalog.getStreakCongratulatoryTitle(currentStreak)
    val motivationalMessage = LuminoCatalog.getStreakMotivationMessage(currentStreak)

    var hasJustClaimed by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.falcon_logo),
                            contentDescription = "Falcon AI Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Daily Check-In",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("daily_checkin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = StarlightGold.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, StarlightGold),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { onOpenStreakVault() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🪙 ${profile.coins}",
                                style = MaterialTheme.typography.labelLarge,
                                color = StarlightGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("daily_checkin_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Emblem with Falcon Logo & Streak Flame
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
                                Brush.verticalGradient(
                                    colors = listOf(
                                        DeepSpaceBackground,
                                        CosmicIndigo,
                                        Color(0xFF2B2269)
                                    )
                                )
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // User's Falcon Logo Emblem with Glowing Ring
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(92.dp)
                                    .scale(if (!isClaimedToday) pulseScale else 1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = TurquoiseNebula.copy(alpha = 0.25f),
                                    modifier = Modifier.size(92.dp)
                                ) {}
                                Image(
                                    painter = painterResource(id = R.drawable.falcon_logo),
                                    contentDescription = "Falcon AI Logo",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                )
                            }

                            // Day Streak Counter Pill
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SolarFlareAmber.copy(alpha = 0.25f),
                                border = BorderStroke(1.5.dp, SolarFlareAmber)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = SolarFlareAmber,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Day $currentStreak Streak",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = congratulatoryTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = motivationalMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFFDED9FF),
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }

            // 2. Interactive 7-Day Streak Roadmap
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "7-Day Consistency Roadmap",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Check in every day to collect escalating Star Coin bonuses. Missed days can be recovered using your coins!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            (1..7).forEach { day ->
                                val dayReward = LuminoCatalog.getDailyStreakRewardCoins(day)
                                val isPastDay = day < currentStreak || (day == currentStreak && isClaimedToday)
                                val isCurrentDay = day == currentStreak && !isClaimedToday
                                val isFutureDay = day > currentStreak

                                val bgColor = when {
                                    isPastDay -> EmeraldSuccess.copy(alpha = 0.2f)
                                    isCurrentDay -> SolarFlareAmber.copy(alpha = 0.25f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                }
                                val borderColor = when {
                                    isPastDay -> EmeraldSuccess
                                    isCurrentDay -> SolarFlareAmber
                                    else -> Color.Transparent
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = bgColor,
                                    border = if (borderColor != Color.Transparent) BorderStroke(1.5.dp, borderColor) else null,
                                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "D$day",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrentDay) SolarFlareAmber else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isPastDay) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Completed",
                                                tint = EmeraldSuccess,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "+$dayReward",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isCurrentDay) SolarFlareAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "🪙",
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Prominent "Claim Coins" Button Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isClaimedToday || hasJustClaimed) {
                            EmeraldSuccess.copy(alpha = 0.12f)
                        } else {
                            SolarFlareAmber.copy(alpha = 0.14f)
                        }
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (isClaimedToday || hasJustClaimed) EmeraldSuccess else SolarFlareAmber
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (isClaimedToday || hasJustClaimed) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Coins Claimed Today!",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "You've checked in for Day $currentStreak. Keep studying today, and return tomorrow to claim your Day ${currentStreak + 1} bonus (+${LuminoCatalog.getDailyStreakRewardCoins(currentStreak + 1)} 🪙)!",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Ready to Claim Today's Bonus?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Claiming today's check-in advances your streak to Day $nextStreakDay and deposits +$coinsToAward Star Coins directly into your account.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    hasJustClaimed = true
                                    onClaimCoins()
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SolarFlareAmber,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("claim_coins_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Claim $coinsToAward Coins 🪙",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 4. Streak Recovery & Shield Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
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
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = null,
                                    tint = TurquoiseNebula
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Streak Recovery & Shields",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = TurquoiseNebula.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${profile.streakShields} 🛡️ Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TurquoiseNebula,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (profile.brokenStudyStreakToRecover > 0 || profile.brokenQuizStreakToRecover > 0) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SupernovaCoral.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, SupernovaCoral),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Missed Streak Detected!",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = SupernovaCoral
                                        )
                                        Text(
                                            text = "Recover ${profile.brokenStudyStreakToRecover.coerceAtLeast(profile.brokenQuizStreakToRecover)} days with 75 coins.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Button(
                                        onClick = onOpenStreakVault,
                                        colors = ButtonDefaults.buttonColors(containerColor = SupernovaCoral),
                                        modifier = Modifier.testTag("recover_streak_from_checkin_button")
                                    ) {
                                        Text("Recover")
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Your streak is protected! Spend 75 coins anytime in your Profile or Streak Vault to equip shields or recover accidental misses.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = onOpenStreakVault,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_vault_from_checkin_button")
                            ) {
                                Icon(imageVector = Icons.Default.Shield, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Recovery Vault & Shields")
                            }
                        }
                    }
                }
            }

            // 5. Daily Consistency Tips & Quick Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateToQuiz,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicViolet)
                    ) {
                        Icon(imageVector = Icons.Default.Quiz, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Daily Quiz")
                    }
                    OutlinedButton(
                        onClick = onNavigateToHomework,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ask Falcon AI")
                    }
                }
            }
        }
    }
}
