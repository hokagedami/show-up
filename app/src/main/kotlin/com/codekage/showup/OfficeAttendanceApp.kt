package com.codekage.showup

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.codekage.showup.service.DailyNotificationWorker
import com.codekage.showup.service.HolidaySyncWorker
import com.codekage.showup.service.WeeklySummaryWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OfficeAttendanceApp : Application() {

    val appContainer: AppContainer by lazy { AppContainer(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        registerGeofencesForActiveJobs()
        HolidaySyncWorker.schedule(this)
        WeeklySummaryWorker.schedule(this)
        DailyNotificationWorker.schedule(this)
    }

    private fun registerGeofencesForActiveJobs() {
        applicationScope.launch {
            runCatching {
                val jobs = appContainer.jobRepository.getActiveJobs().first()
                jobs.forEach { appContainer.geofenceManager.registerGeofence(it) }
            }
        }
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        listOf(
            NotificationChannel(CHANNEL_OFFICE_REMINDER, "Office Day Reminders", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Reminders for scheduled office days" },
            NotificationChannel(CHANNEL_GPS_FAILURE, "GPS Check Reminders", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Reminders when GPS auto-detection didn't trigger" },
            NotificationChannel(CHANNEL_GOAL_ALERT, "Goal Alerts", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Alerts when attendance drops below goal" },
            NotificationChannel(CHANNEL_WEEKLY_SUMMARY, "Weekly Summary", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Weekly attendance summary" },
        ).forEach { nm.createNotificationChannel(it) }
    }

    companion object {
        const val CHANNEL_OFFICE_REMINDER = "office_reminder"
        const val CHANNEL_GPS_FAILURE = "gps_failure"
        const val CHANNEL_GOAL_ALERT = "goal_alert"
        const val CHANNEL_WEEKLY_SUMMARY = "weekly_summary"
    }
}
