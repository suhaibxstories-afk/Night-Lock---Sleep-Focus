package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LockSchedule
import com.example.ui.screens.EditScheduleScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.NightBackground
import com.example.ui.theme.NightLockTheme
import com.example.viewmodel.MainViewModel

sealed class Screen {
    object Home : Screen()
    data class EditSchedule(val schedule: LockSchedule?) : Screen()
    object Focus : Screen()
    object Settings : Screen()
    object Permissions : Screen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var initialBlockedApp: String? = null
    private var isFocusTriggered: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleFocusIntent(intent)

        setContent {
            NightLockTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = NightBackground
                ) {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    var currentScreen by remember {
                        mutableStateOf<Screen>(if (isFocusTriggered) Screen.Focus else Screen.Home)
                    }

                    // Check for intent refresh if focus launched from service
                    LaunchedEffect(isFocusTriggered) {
                        if (isFocusTriggered) {
                            currentScreen = Screen.Focus
                        }
                    }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            is Screen.Home -> {
                                HomeScreen(
                                    uiState = uiState,
                                    onToggleMaster = { enabled -> viewModel.toggleMaster(enabled) },
                                    onToggleSchedule = { id, enabled -> viewModel.toggleSchedule(id, enabled) },
                                    onEditSchedule = { schedule ->
                                        currentScreen = Screen.EditSchedule(schedule)
                                    },
                                    onDeleteSchedule = { id -> viewModel.deleteSchedule(id) },
                                    onCreateSchedule = {
                                        currentScreen = Screen.EditSchedule(null)
                                    },
                                    onOpenFocusScreen = {
                                        currentScreen = Screen.Focus
                                    },
                                    onOpenSettings = {
                                        currentScreen = Screen.Settings
                                    },
                                    onOpenPermissions = {
                                        currentScreen = Screen.Permissions
                                    }
                                )
                            }

                            is Screen.EditSchedule -> {
                                EditScheduleScreen(
                                    initialSchedule = screen.schedule,
                                    is24Hour = uiState.is24HourFormat,
                                    installedApps = uiState.installedApps,
                                    onSave = { schedule ->
                                        viewModel.saveSchedule(schedule)
                                        currentScreen = Screen.Home
                                    },
                                    onBack = {
                                        currentScreen = Screen.Home
                                    }
                                )
                            }

                            is Screen.Focus -> {
                                FocusScreen(
                                    activeSchedule = uiState.activeSchedule,
                                    blockedPackageName = initialBlockedApp,
                                    emergencyAccessCount = uiState.emergencyAccessCountToday,
                                    onEmergencyAccess = {
                                        viewModel.triggerEmergencyAccess()
                                    },
                                    onDismissToHome = {
                                        currentScreen = Screen.Home
                                    }
                                )
                            }

                            is Screen.Settings -> {
                                SettingsScreen(
                                    uiState = uiState,
                                    onToggle24Hour = { is24 -> viewModel.setTimeFormat(is24) },
                                    onToggleStrictGlobal = { enabled -> viewModel.setStrictSleepMode(enabled) },
                                    onResetSchedules = { viewModel.resetAllSchedules() },
                                    onOpenPermissions = { currentScreen = Screen.Permissions },
                                    onBack = { currentScreen = Screen.Home }
                                )
                            }

                            is Screen.Permissions -> {
                                PermissionsScreen(
                                    uiState = uiState,
                                    onRefreshPermissions = { viewModel.refreshPermissions() },
                                    onBack = { currentScreen = Screen.Home }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleFocusIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }

    private fun handleFocusIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("EXTRA_FOCUS_ACTIVE", false) == true) {
            isFocusTriggered = true
            initialBlockedApp = intent.getStringExtra("EXTRA_BLOCKED_APP")
        }
    }
}
