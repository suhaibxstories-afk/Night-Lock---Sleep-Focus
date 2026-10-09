package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.LockSchedule
import com.example.data.ScheduleRepository
import com.example.util.PermissionHelper
import com.example.util.ScheduleCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var repository: ScheduleRepository
    private var lastBlockedLaunchTime: Long = 0L

    companion object {
        const val CHANNEL_ID = "night_lock_focus_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_REFRESH = "com.example.service.ACTION_REFRESH"

        fun start(context: Context) {
            val intent = Intent(context, FocusMonitorService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FocusMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(applicationContext)
        repository = ScheduleRepository(db.lockScheduleDao(), applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification("Night Lock Active", "Monitoring scheduled sleep hours...")
        startForeground(NOTIFICATION_ID, notification)

        startMonitoringLoop()

        return START_STICKY
    }

    private fun startMonitoringLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    checkAndEnforceFocus()
                } catch (e: Exception) {
                    Log.e("FocusMonitorService", "Error during focus monitoring", e)
                }
                delay(1000L) // Poll every second for responsive app detection
            }
        }
    }

    private suspend fun checkAndEnforceFocus() {
        val masterEnabled = repository.masterEnabled.first()
        val schedules = repository.allSchedules.first()

        val activeSchedule = ScheduleCalculator.findActiveSchedule(schedules, masterEnabled)

        if (activeSchedule == null) {
            // Update notification to idle / standby
            updateNotification("Night Lock Standing By", "Focus mode will activate at scheduled sleep time.")
            return
        }

        val remainingMillis = ScheduleCalculator.getRemainingMillis(activeSchedule)
        val hours = remainingMillis / (1000 * 3600)
        val minutes = (remainingMillis % (1000 * 3600)) / (1000 * 60)
        val timeRemainingStr = if (hours > 0) "${hours}h ${minutes}m remaining" else "${minutes}m remaining"

        updateNotification(
            "Sleep Focus Active: ${activeSchedule.name}",
            "Sleep time in progress. $timeRemainingStr"
        )

        // Check the foreground app using UsageStatsManager if permitted
        if (PermissionHelper.hasUsageStatsPermission(this)) {
            val foregroundPkg = getForegroundAppPackage()
            if (foregroundPkg != null && foregroundPkg != packageName) {
                val shouldBlock = if (activeSchedule.lockScreenOnly) {
                    // Everything except dialer / emergency is blocked
                    !activeSchedule.allowedPackages.contains(foregroundPkg) &&
                            foregroundPkg != "com.google.android.dialer" &&
                            foregroundPkg != "com.android.phone"
                } else {
                    // Block if explicitly in blockedPackages or strict mode applies
                    activeSchedule.blockedPackages.contains(foregroundPkg)
                }

                if (shouldBlock) {
                    val now = System.currentTimeMillis()
                    if (now - lastBlockedLaunchTime > 2000L) { // Prevent rapid intent storm
                        lastBlockedLaunchTime = now
                        launchFocusLockScreen(activeSchedule.id, foregroundPkg)
                    }
                }
            }
        }
    }

    private fun getForegroundAppPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val time = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(time - 5000L, time)
        val event = UsageEvents.Event()
        var lastForegroundApp: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastForegroundApp = event.packageName
            }
        }
        return lastForegroundApp
    }

    private fun launchFocusLockScreen(scheduleId: Long, blockedPackage: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_FOCUS_ACTIVE", true)
            putExtra("EXTRA_SCHEDULE_ID", scheduleId)
            putExtra("EXTRA_BLOCKED_APP", blockedPackage)
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Night Lock Sleep Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors sleep schedules and blocks distracting applications"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
