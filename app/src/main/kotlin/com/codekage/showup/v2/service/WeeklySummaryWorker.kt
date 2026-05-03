package com.codekage.showup.v2.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.codekage.showup.v2.OfficeAttendanceApp
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.concurrent.TimeUnit

class WeeklySummaryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val app = applicationContext as OfficeAttendanceApp
        val container = app.appContainer
        val settings = container.settingsRepository.settings.first()
        if (!settings.weeklySummaryEnabled) return@runCatching Result.success()

        val activeJobs = container.jobRepository.getActiveJobs().first()
        if (activeJobs.isEmpty()) return@runCatching Result.success()

        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        val today = LocalDate.now()
        val weekStart = today.with(firstDayOfWeek).minusWeeks(1)
        val weekEnd = weekStart.plusDays(6)

        val parts = activeJobs.map { job ->
            val report = com.codekage.showup.v2.domain.usecase.GetReportDataUseCase(
                container.attendanceRepository,
                container.nonWorkDayRepository,
            ).invoke(job, weekStart, weekEnd, includeWeeklyBreakdown = false)
            "${job.name}: ${report.officeDays} office / ${report.totalWorkingDays}"
        }
        container.notificationScheduler.scheduleWeeklySummary(parts.joinToString(" • "))
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        private const val WORK_NAME = "weekly_summary"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
