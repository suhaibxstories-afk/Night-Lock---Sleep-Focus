package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.ScheduleRepository
import com.example.service.FocusMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context.applicationContext)
                    val repo = ScheduleRepository(db.lockScheduleDao(), context.applicationContext)
                    val masterEnabled = repo.masterEnabled.first()
                    val schedules = repo.allSchedules.first()

                    if (masterEnabled && schedules.any { it.isEnabled }) {
                        FocusMonitorService.start(context)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
