package com.example.ui.auth

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.theme.CosmicIndigo
import com.example.ui.theme.CosmicViolet
import com.example.ui.theme.DeepSpaceBackground
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SolarFlareAmber
import com.example.ui.theme.StarlightGold
import com.example.ui.theme.TurquoiseNebula
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun signOutFromLumino(
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    FirebaseAuth.getInstance().signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@Composable
fun FalconBrandEmblem(
    size: Dp = 96.dp,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size + 20.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TurquoiseNebula.copy(alpha = 0.32f),
                        CosmicViolet.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        TurquoiseNebula,
                        CosmicViolet,
                        StarlightGold,
                        TurquoiseNebula
                    ),
                    center = center
                ),
                radius = outerRadius * 0.86f,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0B0F29),
            border = BorderStroke(
                2.dp,
                Brush.linearGradient(
                    colors = listOf(TurquoiseNebula, CosmicViolet, StarlightGold)
                )
            ),
            shadowElevation = 12.dp,
            modifier = Modifier.size(size)
        ) {
            Image(
                painter = painterResource(id = R.drawable.falcon_logo),
                contentDescription = "Falcon AI Crest Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(22.dp))
            )
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onEnterGuestMode: () -> Unit = {},
    onContinueWithGmailOnly: (String) -> Unit = {}
) {
    var gmailInput by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var useRedesignedLayout by rememberSaveable { mutableStateOf(true) }

    val submitGmailOnly: (String) -> Unit = { rawInput ->
        val trimmed = rawInput.trim().lowercase()
        when {
            trimmed.isEmpty() -> {
                errorMessage = "Enter your @gmail.com address or tap your device Gmail account above."
            }
            !trimmed.contains("@") -> {
                onContinueWithGmailOnly("$trimmed@gmail.com")
            }
            trimmed.endsWith("@gmail.com") -> {
                onContinueWithGmailOnly(trimmed)
            }
            else -> {
                errorMessage = "Only @gmail.com addresses are needed — no phone numbers or passwords."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070A1E),
                        DeepSpaceBackground,
                        CosmicIndigo,
                        Color(0xFF1E1654)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        // Ambient cosmic background light spheres
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = TurquoiseNebula.copy(alpha = 0.14f),
                radius = size.minDimension * 0.48f,
                center = Offset(size.width * 0.88f, size.height * 0.12f)
            )
            drawCircle(
                color = CosmicViolet.copy(alpha = 0.18f),
                radius = size.minDimension * 0.44f,
                center = Offset(size.width * 0.10f, size.height * 0.48f)
            )
            drawCircle(
                color = StarlightGold.copy(alpha = 0.10f),
                radius = size.minDimension * 0.36f,
                center = Offset(size.width * 0.85f, size.height * 0.88f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top bar with Design Layout Switcher so user can compare New vs Classic anytime
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = TurquoiseNebula.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, TurquoiseNebula.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = TurquoiseNebula,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FALCON AI • STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = TurquoiseNebula,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .clickable { useRedesignedLayout = !useRedesignedLayout }
                        .testTag("toggle_signin_design_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch Sign-In Design",
                            tint = StarlightGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (useRedesignedLayout) "New Layout ✦" else "Classic Layout",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = useRedesignedLayout,
                label = "sign_in_layout_transition"
            ) { isModern ->
                if (isModern) {
                    ModernSignInPortalContent(
                        gmailInput = gmailInput,
                        errorMessage = errorMessage,
                        onGmailInputChanged = {
                            gmailInput = it
                            errorMessage = null
                        },
                        onSubmitGmail = { submitGmailOnly(gmailInput) },
                        onOneTapDeviceGmail = { onContinueWithGmailOnly("dineshdodiya432@gmail.com") },
                        onEnterGuestMode = onEnterGuestMode
                    )
                } else {
                    ClassicSignInPortalContent(
                        gmailInput = gmailInput,
                        errorMessage = errorMessage,
                        onGmailInputChanged = {
                            gmailInput = it
                            errorMessage = null
                        },
                        onSubmitGmail = { submitGmailOnly(gmailInput) },
                        onOneTapDeviceGmail = { onContinueWithGmailOnly("dineshdodiya432@gmail.com") },
                        onEnterGuestMode = onEnterGuestMode
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernSignInPortalContent(
    gmailInput: String,
    errorMessage: String?,
    onGmailInputChanged: (String) -> Unit,
    onSubmitGmail: () -> Unit,
    onOneTapDeviceGmail: () -> Unit,
    onEnterGuestMode: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. Upgraded Falcon AI Emblem & Brand Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FalconBrandEmblem(size = 94.dp)

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "AI Homework Mentor • Falcon Code IDE • Grades 1–12",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFD9D4FF),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            // 3 Capability Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CapabilityPill(text = "⚡ Falcon Code", color = EmeraldSuccess)
                CapabilityPill(text = "📷 Camera Tutor", color = TurquoiseNebula)
                CapabilityPill(text = "🌍 14+ Languages", color = StarlightGold)
            }
        }

        // 2. Unified Luxury Sign-In Portal Card
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF101538).copy(alpha = 0.92f)
            ),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    colors = listOf(
                        TurquoiseNebula.copy(alpha = 0.8f),
                        CosmicViolet.copy(alpha = 0.8f),
                        StarlightGold.copy(alpha = 0.7f)
                    )
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier.fillMaxWidth()
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
                    Column {
                        Text(
                            text = "Welcome to Falcon AI",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Zero passwords • Zero phone verification",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = EmeraldSuccess.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Instant Access",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // A. Primary One-Tap Device Gmail Account Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1B224D),
                    border = BorderStroke(1.5.dp, StarlightGold.copy(alpha = 0.85f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOneTapDeviceGmail() }
                        .testTag("google_sign_in_button")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = StarlightGold,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "D",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CosmicIndigo
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Continue as Dinesh",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = "dineshdodiya432@gmail.com",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StarlightGold,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "1-Tap Device Gmail • Cloud Sync Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = StarlightGold.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Continue",
                                    tint = StarlightGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // B. 1-Tap Guest Mode Button (High Contrast Cyan)
                Button(
                    onClick = onEnterGuestMode,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TurquoiseNebula,
                        contentColor = CosmicIndigo
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guest_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = CosmicIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Guest Mode — Enter App Immediately",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Skip sign-in • Full access to Falcon AI & Falcon Code",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicIndigo.copy(alpha = 0.85f)
                        )
                    }
                }

                // C. Divider for Custom Gmail Entry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.14f))
                    Text(
                        text = "OR USE ANY @GMAIL.COM",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.14f))
                }

                // D. Custom Gmail Input + Direct Sign-In Button
                OutlinedTextField(
                    value = gmailInput,
                    onValueChange = onGmailInputChanged,
                    label = { Text("Enter any @gmail.com address") },
                    placeholder = { Text("scholar@gmail.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = TurquoiseNebula
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = onSubmitGmail,
                            modifier = Modifier.testTag("gmail_send_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Gmail",
                                tint = TurquoiseNebula
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { onSubmitGmail() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TurquoiseNebula,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
                        focusedLabelColor = TurquoiseNebula,
                        unfocusedLabelColor = Color(0xFFCBD5E1)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gmail_only_input")
                )

                Button(
                    onClick = onSubmitGmail,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicViolet,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("gmail_direct_sign_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continue with Gmail Only",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFF5252).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF5252)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFCDD2),
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 3. 2x2 Bento Feature Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                BentoFeatureCard(
                    icon = Icons.Default.Code,
                    tint = EmeraldSuccess,
                    title = "Falcon Code IDE",
                    subtitle = "Execute Python, JS, C++, Java, C & Web instantly",
                    modifier = Modifier.weight(1f)
                )
                BentoFeatureCard(
                    icon = Icons.Default.AutoAwesome,
                    tint = StarlightGold,
                    title = "AI Homework Tutor",
                    subtitle = "Camera snap, voice dictation & clear solutions",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                BentoFeatureCard(
                    icon = Icons.Default.LocalFireDepartment,
                    tint = SolarFlareAmber,
                    title = "5-Tier Quiz Arena",
                    subtitle = "Easy to Impossible quizzes & Streak Vault",
                    modifier = Modifier.weight(1f)
                )
                BentoFeatureCard(
                    icon = Icons.Default.Language,
                    tint = TurquoiseNebula,
                    title = "Grades 1–12 Global",
                    subtitle = "14+ languages with adaptive learning paths",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CapabilityPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun BentoFeatureCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = tint.copy(alpha = 0.2f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ClassicSignInPortalContent(
    gmailInput: String,
    errorMessage: String?,
    onGmailInputChanged: (String) -> Unit,
    onSubmitGmail: () -> Unit,
    onOneTapDeviceGmail: () -> Unit,
    onEnterGuestMode: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FalconBrandEmblem(size = 86.dp)

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Smart Multilingual AI Tutor for Grades 1–12",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFD9D4FF),
            textAlign = TextAlign.Center
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = TurquoiseNebula.copy(alpha = 0.16f)
            ),
            border = BorderStroke(1.5.dp, TurquoiseNebula),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = TurquoiseNebula,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Guest Mode (Direct Instant Access)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "No Gmail, no phone verification, and no password — tap below to enter Falcon AI immediately.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE0F7FA),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onEnterGuestMode,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TurquoiseNebula,
                        contentColor = CosmicIndigo
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("guest_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = CosmicIndigo
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Guest Mode — Enter App Now",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
            Text(
                text = "OR GMAIL ONLY (NO PASSWORD / NO PHONE)",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFC8C5E2)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.10f)
            ),
            border = BorderStroke(1.dp, StarlightGold.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "One-Tap Device Gmail (No Password, No Phone)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, StarlightGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOneTapDeviceGmail() }
                        .testTag("google_sign_in_button")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Device Gmail",
                                tint = StarlightGold,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "dineshdodiya432@gmail.com",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap to sign in directly with Device Gmail (No password)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFD9D4FF)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Continue",
                            tint = StarlightGold
                        )
                    }
                }

                OutlinedTextField(
                    value = gmailInput,
                    onValueChange = onGmailInputChanged,
                    label = { Text("Or type any @gmail.com address") },
                    placeholder = { Text("yourname@gmail.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = StarlightGold
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = onSubmitGmail,
                            modifier = Modifier.testTag("gmail_send_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Gmail",
                                tint = StarlightGold
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { onSubmitGmail() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = StarlightGold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                        focusedLabelColor = StarlightGold,
                        unfocusedLabelColor = Color(0xFFD9D4FF)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gmail_only_input")
                )

                Button(
                    onClick = onSubmitGmail,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StarlightGold,
                        contentColor = CosmicIndigo
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("gmail_direct_sign_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = CosmicIndigo
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continue with Gmail Only",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (errorMessage != null) {
            Surface(
                color = Color(0xFFFF5252).copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFCDD2),
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
