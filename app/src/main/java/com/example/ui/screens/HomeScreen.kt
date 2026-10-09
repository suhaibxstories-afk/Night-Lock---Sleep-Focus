package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LockSchedule
import com.example.ui.components.ScheduleCard
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentCyanGlow
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.NightBackground
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.NightSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.util.PermissionHelper
import com.example.util.ScheduleCalculator
import com.example.viewmodel.NightLockUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: NightLockUiState,
    onToggleMaster: (Boolean) -> Unit,
    onToggleSchedule: (Long, Boolean) -> Unit,
    onEditSchedule: (LockSchedule) -> Unit,
    onDeleteSchedule: (Long) -> Unit,
    onCreateSchedule: () -> Unit,
    onOpenFocusScreen: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    val context = LocalContext.current
    val hasAllPermissions = uiState.hasUsagePermission && uiState.hasOverlayPermission

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    Brush.linearGradient(listOf(AccentCyan, AccentBlue)),
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightlightRound,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Night Lock",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Sleep Focus",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = AccentCyanGlow,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NightBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateSchedule,
                containerColor = AccentCyan,
                contentColor = NightBackground,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("create_schedule_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Schedule",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = NightBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Banner (if needed)
            if (!hasAllPermissions) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenPermissions() }
                            .testTag("permission_banner"),
                        colors = CardDefaults.cardColors(
                            containerColor = WarningAmber.copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Setup Required for App Blocking",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Usage access and overlay permission are needed to block apps reliably.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary
                                    )
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = WarningAmber
                            )
                        }
                    }
                }
            }

            // Active Focus Banner (when currently locked)
            if (uiState.activeSchedule != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onOpenFocusScreen() }
                            .testTag("active_focus_banner"),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F233A)
                        ),
                        border = BorderStroke(1.5.dp, AccentCyan)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(AccentCyan, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = NightBackground,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Sleep Mode is Active",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = uiState.activeSchedule.name,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = AccentCyanGlow
                                            )
                                        )
                                    }
                                }

                                val remaining = ScheduleCalculator.getRemainingMillis(uiState.activeSchedule)
                                val hours = remaining / (1000 * 3600)
                                val mins = (remaining % (1000 * 3600)) / (1000 * 60)
                                Text(
                                    text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m left",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = AccentCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = onOpenFocusScreen,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("view_focus_screen_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "View Sleep Screen",
                                    color = NightBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Master Lockout Toggle Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("master_lockout_card"),
                    colors = CardDefaults.cardColors(containerColor = NightSurface),
                    border = BorderStroke(1.dp, NightCardBorder),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                if (uiState.masterEnabled) AccentBlue else Color(0xFF334155),
                                                if (uiState.masterEnabled) AccentIndigo else Color(0xFF1E293B)
                                            )
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Master Lock Icon",
                                    tint = if (uiState.masterEnabled) Color.White else TextMuted,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = "Scheduled Lockouts",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (uiState.masterEnabled) "Automatic sleep locks active" else "All schedules paused",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (uiState.masterEnabled) AccentCyanGlow else TextMuted
                                    )
                                )
                            }
                        }

                        Switch(
                            checked = uiState.masterEnabled,
                            onCheckedChange = onToggleMaster,
                            modifier = Modifier.testTag("master_toggle_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentCyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = NightSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Regular Lockouts Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Regular Lockouts",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 18.sp
                        )
                    )

                    Text(
                        text = "${uiState.schedules.size} configured",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted
                        )
                    )
                }
            }

            // Existing Schedules List
            if (uiState.schedules.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NightSurface),
                        border = BorderStroke(1.dp, NightCardBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightlightRound,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Schedules Yet",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the + button to create a regular sleep lockout schedule.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary
                                )
                            )
                        }
                    }
                }
            } else {
                items(uiState.schedules, key = { it.id }) { schedule ->
                    val isActive = uiState.activeSchedule?.id == schedule.id
                    ScheduleCard(
                        schedule = schedule,
                        is24Hour = uiState.is24HourFormat,
                        isCurrentlyActive = isActive,
                        onToggle = { enabled -> onToggleSchedule(schedule.id, enabled) },
                        onClick = { onEditSchedule(schedule) },
                        onDelete = { onDeleteSchedule(schedule.id) }
                    )
                }
            }

            // Usage-Based Lockouts section (for future expansion)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Usage-Based Lockouts",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("usage_lockouts_card"),
                    colors = CardDefaults.cardColors(containerColor = NightSurface.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, NightCardBorder.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(AccentIndigo.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = AccentIndigo,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Night App Limits",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = AccentIndigo.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "COMING SOON",
                                            color = AccentCyanGlow,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Lock phone after 20 minutes of continuous bedtime usage",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp)) // Extra space for FAB
            }
        }
    }
}
