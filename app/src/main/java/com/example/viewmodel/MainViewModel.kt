package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.LockSchedule
import com.example.data.ScheduleRepository
import com.example.service.FocusMonitorService
import com.example.util.AppCategory
import com.example.util.InstalledAppInfo
import com.example.util.PermissionHelper
import com.example.util.ScheduleCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PermissionStatus(
    val hasUsagePermission: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = true
)

data class NightLockUiState(
    val schedules: List<LockSchedule> = emptyList(),
    val masterEnabled: Boolean = true,
    val is24HourFormat: Boolean = false,
    val strictSleepModeGlobal: Boolean = false,
    val emergencyAccessCountToday: Int = 0,
    val activeSchedule: LockSchedule? = null,
    val hasUsagePermission: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = true,
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val isLoadingApps: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScheduleRepository
    private val context: Context get() = getApplication<Application>().applicationContext

    init {
        val db = AppDatabase.getDatabase(context)
        repository = ScheduleRepository(db.lockScheduleDao(), context)
    }

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps = _isLoadingApps.asStateFlow()

    private val _permissionStatus = MutableStateFlow(
        PermissionStatus(
            hasUsagePermission = PermissionHelper.hasUsageStatsPermission(context),
            hasOverlayPermission = PermissionHelper.canDrawOverlays(context),
            isIgnoringBatteryOptimizations = PermissionHelper.isIgnoringBatteryOptimizations(context)
        )
    )

    // Combine flows cleanly using 2 stages to stay within standard combine limits
    private val _scheduleAndPrefsFlow = combine(
        repository.allSchedules,
        repository.masterEnabled,
        repository.is24HourFormat,
        repository.strictSleepModeGlobal,
        repository.emergencyAccessCountToday
    ) { schedules, masterEnabled, is24Hr, strictGlobal, emergencyCount ->
        SchedulePrefsBundle(
            schedules = schedules,
            masterEnabled = masterEnabled,
            is24HourFormat = is24Hr,
            strictSleepModeGlobal = strictGlobal,
            emergencyAccessCountToday = emergencyCount
        )
    }

    val uiState: StateFlow<NightLockUiState> = combine(
        _scheduleAndPrefsFlow,
        _permissionStatus,
        _installedApps,
        _isLoadingApps
    ) { bundle, perms, apps, loadingApps ->
        val active = ScheduleCalculator.findActiveSchedule(bundle.schedules, bundle.masterEnabled)
        NightLockUiState(
            schedules = bundle.schedules,
            masterEnabled = bundle.masterEnabled,
            is24HourFormat = bundle.is24HourFormat,
            strictSleepModeGlobal = bundle.strictSleepModeGlobal,
            emergencyAccessCountToday = bundle.emergencyAccessCountToday,
            activeSchedule = active,
            hasUsagePermission = perms.hasUsagePermission,
            hasOverlayPermission = perms.hasOverlayPermission,
            isIgnoringBatteryOptimizations = perms.isIgnoringBatteryOptimizations,
            installedApps = apps,
            isLoadingApps = loadingApps
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NightLockUiState()
    )

    init {
        refreshPermissions()
        loadInstalledApps()
        checkAndStartService()
    }

    fun refreshPermissions() {
        _permissionStatus.value = PermissionStatus(
            hasUsagePermission = PermissionHelper.hasUsageStatsPermission(context),
            hasOverlayPermission = PermissionHelper.canDrawOverlays(context),
            isIgnoringBatteryOptimizations = PermissionHelper.isIgnoringBatteryOptimizations(context)
        )
    }

    fun toggleMaster(enabled: Boolean) {
        repository.setMasterEnabled(enabled)
        if (enabled) {
            FocusMonitorService.start(context)
        } else {
            FocusMonitorService.stop(context)
        }
    }

    fun toggleSchedule(scheduleId: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.setScheduleEnabled(scheduleId, enabled)
            checkAndStartService()
        }
    }

    fun saveSchedule(schedule: LockSchedule) {
        viewModelScope.launch {
            if (schedule.id == 0L) {
                repository.insertSchedule(schedule)
            } else {
                repository.updateSchedule(schedule)
            }
            checkAndStartService()
        }
    }

    fun deleteSchedule(scheduleId: Long) {
        viewModelScope.launch {
            repository.deleteScheduleById(scheduleId)
            checkAndStartService()
        }
    }

    fun setTimeFormat(is24: Boolean) {
        repository.set24HourFormat(is24)
    }

    fun setStrictSleepMode(enabled: Boolean) {
        repository.setStrictSleepModeGlobal(enabled)
    }

    fun triggerEmergencyAccess() {
        repository.recordEmergencyAccess()
    }

    fun resetAllSchedules() {
        viewModelScope.launch {
            repository.resetAllSchedules()
            checkAndStartService()
        }
    }

    fun checkAndStartService() {
        if (repository.masterEnabled.value) {
            FocusMonitorService.start(context)
        }
    }

    fun loadInstalledApps() {
        if (_isLoadingApps.value || _installedApps.value.isNotEmpty()) return

        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
                val list = mutableListOf<InstalledAppInfo>()

                for (pkg in packages) {
                    val appInfo = pkg.applicationInfo ?: continue
                    if (pkg.packageName == context.packageName) continue

                    val launchIntent = pm.getLaunchIntentForPackage(pkg.packageName)
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    if (launchIntent != null || !isSystem) {
                        val name = appInfo.loadLabel(pm).toString()
                        val icon = try {
                            appInfo.loadIcon(pm)
                        } catch (_: Exception) {
                            null
                        }

                        val category = when {
                            pkg.packageName.contains("youtube") ||
                            pkg.packageName.contains("tiktok") ||
                            pkg.packageName.contains("musically") ||
                            pkg.packageName.contains("netflix") ||
                            pkg.packageName.contains("video") -> AppCategory.ENTERTAINMENT

                            pkg.packageName.contains("instagram") ||
                            pkg.packageName.contains("facebook") ||
                            pkg.packageName.contains("twitter") ||
                            pkg.packageName.contains("reddit") ||
                            pkg.packageName.contains("snapchat") ||
                            pkg.packageName.contains("threads") -> AppCategory.SOCIAL

                            pkg.packageName.contains("game") -> AppCategory.GAMES

                            isSystem -> AppCategory.SYSTEM

                            else -> AppCategory.OTHER
                        }

                        list.add(
                            InstalledAppInfo(
                                packageName = pkg.packageName,
                                appName = name,
                                icon = icon,
                                isSystemApp = isSystem,
                                category = category
                            )
                        )
                    }
                }
                list.sortedBy { it.appName.lowercase() }
            }
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }
}

private data class SchedulePrefsBundle(
    val schedules: List<LockSchedule>,
    val masterEnabled: Boolean,
    val is24HourFormat: Boolean,
    val strictSleepModeGlobal: Boolean,
    val emergencyAccessCountToday: Int
)
