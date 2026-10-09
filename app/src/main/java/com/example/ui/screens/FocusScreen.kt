package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LockSchedule
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentCyanGlow
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NightBackground
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.NightSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.util.ScheduleCalculator
import kotlinx.coroutines.delay

@Composable
fun FocusScreen(
    activeSchedule: LockSchedule?,
    blockedPackageName: String?,
    emergencyAccessCount: Int,
    onEmergencyAccess: () -> Unit,
    onDismissToHome: () -> Unit
) {
    val context = LocalContext.current
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var remainingMillis by remember {
        mutableLongStateOf(
            activeSchedule?.let { ScheduleCalculator.getRemainingMillis(it) } ?: (6 * 3600 * 1000L)
        )
    }

    // Keep remaining time updating every second
    LaunchedEffect(activeSchedule) {
        while (true) {
            if (activeSchedule != null) {
                remainingMillis = ScheduleCalculator.getRemainingMillis(activeSchedule)
            }
            delay(1000L)
        }
    }

    val hours = remainingMillis / (1000 * 3600)
    val minutes = (remainingMillis % (1000 * 3600)) / (1000 * 60)
    val seconds = (remainingMillis % (1000 * 60)) / 1000

    // Gentle breathing pulse animation for the lock orb
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lock_pulse"
    )

    BackHandler {
        // Prevent accidental back exit during strict focus mode, but allow returning to app home
        onDismissToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        NightBackground,
                        Color(0xFF0D1424),
                        NightBackground
                    )
                )
            )
            .padding(24.dp)
            .testTag("focus_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Glowing Moon & Lock Avatar
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(pulseScale)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                AccentCyan.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Focus Lock Icon",
                        tint = AccentCyan,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Main Sleep text requested by user
            Text(
                text = "It's Sleep Time",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 32.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // User prompt required quote:
            Text(
                text = "Your future self will thank you.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = AccentCyanGlow,
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp
                ),
                textAlign = TextAlign.Center
            )

            if (blockedPackageName != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = NightSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NightCardBorder)
                ) {
                    Text(
                        text = "App paused for sleep: $blockedPackageName",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Remaining Time Countdown Card
            Card(
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                border = BorderStroke(1.dp, NightCardBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "REMAINING SLEEP TIME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            fontSize = 38.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Scheduled until ${(activeSchedule?.endHour ?: 7)}:${String.format("%02d", activeSchedule?.endMinute ?: 0)}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Emergency Access & Return Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Emergency Dial / Call button
                Button(
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(dialIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NightSurfaceVariant),
                    border = BorderStroke(1.dp, NightCardBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("emergency_phone_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = AccentCyanGlow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Phone",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Emergency Access button
                Button(
                    onClick = { showEmergencyDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF261821)),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                        .testTag("emergency_access_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Emergency",
                        color = DangerRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Return to app settings/schedule
            TextButton(
                onClick = onDismissToHome,
                modifier = Modifier.testTag("return_to_night_lock_button")
            ) {
                Text(
                    text = "Return to Night Lock Dashboard",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }
    }

    // Emergency Access Confirmation Dialog
    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = DangerRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Emergency Unlock",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Are you experiencing an urgent situation? Essential phone calling and emergency safety functions are always unlocked.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Emergency uses recorded today: $emergencyAccessCount",
                        color = WarningAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmergencyDialog = false
                        onEmergencyAccess()
                        onDismissToHome()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Unlock 10 Mins", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = NightSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
