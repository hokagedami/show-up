package com.codekage.showup.v2.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.codekage.showup.v2.OfficeAttendanceApp
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class DailyNotificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val app = applicationContext as OfficeAttendanceApp
        val container = app.appContainer
        val settings = container.settingsRepository.settings.first()
        if (!settings.officeReminderEnabled) return@runCatching Result.success()

        val today = LocalDate.now()
        val activeJobs = container.jobRepository.getActiveJobs().first()
        val reminderTime = LocalTime.of(settings.reminderTimeHour, settings.reminderTimeMinute)

        activeJobs.forEach { job ->
            if (today.dayOfWeek in job.workDays) {
                container.notificationScheduler.scheduleOfficeDayReminder(
                    job.id.toString(), job.name, reminderTime,
                )
            }
        }
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        private const val WORK_NAME = "daily_notification"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyNotificationWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        @Suppress("unused") private val WORK_DAY_MARKER: DayOfWeek = DayOfWeek.MONDAY
    }
}
