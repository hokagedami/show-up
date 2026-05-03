package com.codekage.showup.v2.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.codekage.showup.v2.OfficeAttendanceApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? OfficeAttendanceApp ?: return
        val container = app.appContainer
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val activeJobs = container.jobRepository.getActiveJobs().first()
                activeJobs.forEach { container.geofenceManager.registerGeofence(it) }

                val settings = container.settingsRepository.settings.first()
                val reminderTime = java.time.LocalTime.of(settings.reminderTimeHour, settings.reminderTimeMinute)
                container.notificationScheduler.rescheduleAllNotifications(
                    activeJobs.map { it.id.toString() to it.name },
                    reminderTime,
                )
            } finally {
                pending.finish()
            }
        }
    }
}
