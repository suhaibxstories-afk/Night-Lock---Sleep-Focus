package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LockSchedule
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentCyanGlow
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.NightBackground
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.NightSurfaceVariant
import com.example.ui.theme.StrictPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.util.InstalledAppInfo
import com.example.util.ScheduleCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleScreen(
    initialSchedule: LockSchedule?,
    is24Hour: Boolean,
    installedApps: List<InstalledAppInfo>,
    onSave: (LockSchedule) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var name by remember { mutableStateOf(initialSchedule?.name ?: "Sleep Time") }
    var startHour by remember { mutableIntStateOf(initialSchedule?.startHour ?: 23) }
    var startMinute by remember { mutableIntStateOf(initialSchedule?.startMinute ?: 0) }
    var endHour by remember { mutableIntStateOf(initialSchedule?.endHour ?: 7) }
    var endMinute by remember { mutableIntStateOf(initialSchedule?.endMinute ?: 0) }
    var selectedDays by remember {
        mutableStateOf(initialSchedule?.daysOfWeek ?: listOf(1, 2, 3, 4, 5, 6, 7))
    }
    var blockedPackages by remember {
        mutableStateOf(
            initialSchedule?.blockedPackages?.toSet() ?: setOf(
                "com.google.android.youtube",
                "com.instagram.android",
                "com.zhiliaoapp.musically",
                "com.facebook.katana",
                "com.twitter.android",
                "com.reddit.frontpage"
            )
        )
    }
    var allowedPackages by remember {
        mutableStateOf(
            initialSchedule?.allowedPackages?.toSet() ?: setOf(
                "com.google.android.dialer",
                "com.android.phone",
                "com.google.android.deskclock"
            )
        )
    }
    var lockScreenOnly by remember { mutableStateOf(initialSchedule?.lockScreenOnly ?: false) }
    var isStrictSleepMode by remember { mutableStateOf(initialSchedule?.isStrictSleepMode ?: false) }

    var selectedAppTab by remember { mutableIntStateOf(0) } // 0: Blocked Apps, 1: Allowed Apps

    val crossesMidnight = (startHour * 60 + startMinute) > (endHour * 60 + endMinute)

    // Time Pickers
    fun showStartTimePicker() {
        TimePickerDialog(
            context,
            { _, h, m ->
                startHour = h
                startMinute = m
            },
            startHour,
            startMinute,
            is24Hour
        ).show()
    }

    fun showEndTimePicker() {
        TimePickerDialog(
            context,
            { _, h, m ->
                endHour = h
                endMinute = m
            },
            endHour,
            endMinute,
            is24Hour
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialSchedule == null) "Create Lockout" else "Edit Lockout",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NightBackground)
            )
        },
        bottomBar = {
            Surface(
                color = NightSurface,
                border = BorderStroke(1.dp, NightCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = BorderStroke(1.dp, NightCardBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("cancel_schedule_button")
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val scheduleToSave = LockSchedule(
                                id = initialSchedule?.id ?: 0L,
                                name = name.ifBlank { "Sleep Time" },
                                startHour = startHour,
                                startMinute = startMinute,
                                endHour = endHour,
                                endMinute = endMinute,
                                daysOfWeek = selectedDays.ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) },
                                isEnabled = initialSchedule?.isEnabled ?: true,
                                blockedPackages = blockedPackages.toList(),
                                allowedPackages = allowedPackages.toList(),
                                isStrictSleepMode = isStrictSleepMode,
                                lockScreenOnly = lockScreenOnly
                            )
                            onSave(scheduleToSave)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .testTag("save_schedule_button")
                    ) {
                        Text(
                            text = "Save Schedule",
                            color = NightBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = NightBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Name Input
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NightSurface),
                    border = BorderStroke(1.dp, NightCardBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Schedule Name",
                            style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("e.g. Sleep Time, Bedtime Lock") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("schedule_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = NightCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // Time Pickers Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NightSurface),
                    border = BorderStroke(1.dp, NightCardBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Schedule Timing",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Start Time
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showStartTimePicker() }
                                    .testTag("start_time_picker"),
                                color = NightSurfaceVariant,
                                border = BorderStroke(1.dp, NightCardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "STARTS AT",
                                        fontSize = 11.sp,
                                        color = AccentCyanGlow,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ScheduleCalculator.formatTime(startHour, startMinute, is24Hour),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap to set",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            // End Time
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showEndTimePicker() }
                                    .testTag("end_time_picker"),
                                color = NightSurfaceVariant,
                                border = BorderStroke(1.dp, NightCardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "ENDS AT",
                                        fontSize = 11.sp,
                                        color = AccentCyanGlow,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ScheduleCalculator.formatTime(endHour, endMinute, is24Hour),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap to set",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        if (crossesMidnight) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = AccentCyanGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Overnight schedule: ends the following morning.",
                                    fontSize = 12.sp,
                                    color = AccentCyanGlow
                                )
                            }
                        }
                    }
                }
            }

            // Days of the Week Selection
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NightSurface),
                    border = BorderStroke(1.dp, NightCardBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Repeat Days",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )

                            Text(
                                text = if (selectedDays.size == 7) "Every day" else "${selectedDays.size} days",
                                style = MaterialTheme.typography.bodySmall.copy(color = AccentCyanGlow)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val days = listOf(
                            1 to "M",
                            2 to "T",
                            3 to "W",
                            4 to "T",
                            5 to "F",
                            6 to "S",
                            7 to "S"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            days.forEach { (dayNum, label) ->
                                val isSelected = selectedDays.contains(dayNum)
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) AccentCyan else NightSurfaceVariant)
                                        .clickable {
                                            selectedDays = if (isSelected) {
                                                if (selectedDays.size > 1) selectedDays - dayNum else selectedDays
                                            } else {
                                                (selectedDays + dayNum).sorted()
                                            }
                                        }
                                        .testTag("day_button_$dayNum"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) NightBackground else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Lockout Mode Options
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NightSurface),
                    border = BorderStroke(1.dp, NightCardBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Lockout Behavior",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Lock-screen interface only
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Only show Night Lock screen",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "Replaces launcher and locks all apps except dialer/emergency",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                )
                            }
                            Switch(
                                checked = lockScreenOnly,
                                onCheckedChange = { lockScreenOnly = it },
                                modifier = Modifier.testTag("lock_screen_only_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AccentCyan,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = NightSurfaceVariant
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Strict Sleep Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Strict Sleep Mode",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = StrictPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Blocks distracting apps without easy dismiss toggles",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                )
                            }
                            Switch(
                                checked = isStrictSleepMode,
                                onCheckedChange = { isStrictSleepMode = it },
                                modifier = Modifier.testTag("strict_mode_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = StrictPurple,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = NightSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // App Selection Tabs & List
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NightSurface),
                    border = BorderStroke(1.dp, NightCardBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Apps Management",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        TabRow(
                            selectedTabIndex = selectedAppTab,
                            containerColor = NightSurfaceVariant,
                            contentColor = AccentCyan,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedAppTab]),
                                    color = AccentCyan
                                )
                            }
                        ) {
                            Tab(
                                selected = selectedAppTab == 0,
                                onClick = { selectedAppTab = 0 },
                                text = {
                                    Text(
                                        "Blocked (${blockedPackages.size})",
                                        fontWeight = if (selectedAppTab == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                            Tab(
                                selected = selectedAppTab == 1,
                                onClick = { selectedAppTab = 1 },
                                text = {
                                    Text(
                                        "Allowed (${allowedPackages.size})",
                                        fontWeight = if (selectedAppTab == 1) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val displayApps = installedApps.ifEmpty {
                            // Fallback common distracting apps if installedApps not yet fully populated
                            listOf(
                                InstalledAppInfo("com.google.android.youtube", "YouTube"),
                                InstalledAppInfo("com.instagram.android", "Instagram"),
                                InstalledAppInfo("com.zhiliaoapp.musically", "TikTok"),
                                InstalledAppInfo("com.facebook.katana", "Facebook"),
                                InstalledAppInfo("com.twitter.android", "X (Twitter)"),
                                InstalledAppInfo("com.reddit.frontpage", "Reddit"),
                                InstalledAppInfo("com.netflix.mediaclient", "Netflix"),
                                InstalledAppInfo("com.google.android.dialer", "Phone (Essential)"),
                                InstalledAppInfo("com.google.android.deskclock", "Clock & Alarm")
                            )
                        }

                        displayApps.forEach { app ->
                            val isBlocked = blockedPackages.contains(app.packageName)
                            val isAllowed = allowedPackages.contains(app.packageName)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (selectedAppTab == 0) {
                                            blockedPackages = if (isBlocked) {
                                                blockedPackages - app.packageName
                                            } else {
                                                allowedPackages = allowedPackages - app.packageName
                                                blockedPackages + app.packageName
                                            }
                                        } else {
                                            allowedPackages = if (isAllowed) {
                                                allowedPackages - app.packageName
                                            } else {
                                                blockedPackages = blockedPackages - app.packageName
                                                allowedPackages + app.packageName
                                            }
                                        }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = if (selectedAppTab == 0) isBlocked else isAllowed,
                                    onCheckedChange = { checked ->
                                        if (selectedAppTab == 0) {
                                            blockedPackages = if (checked) {
                                                allowedPackages = allowedPackages - app.packageName
                                                blockedPackages + app.packageName
                                            } else {
                                                blockedPackages - app.packageName
                                            }
                                        } else {
                                            allowedPackages = if (checked) {
                                                blockedPackages = blockedPackages - app.packageName
                                                allowedPackages + app.packageName
                                            } else {
                                                allowedPackages - app.packageName
                                            }
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = AccentCyan,
                                        uncheckedColor = TextMuted,
                                        checkmarkColor = NightBackground
                                    )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
