package com.codekage.showup.v2.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class NotificationScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntent(requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun schedule(requestCode: Int, intent: Intent, triggerMillis: Long) {
        val pi = pendingIntent(requestCode, intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        }
    }

    fun scheduleOfficeDayReminder(jobId: String, jobName: String, time: LocalTime) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_OFFICE_REMINDER
            putExtra(EXTRA_JOB_ID, jobId)
            putExtra(EXTRA_JOB_NAME, jobName)
        }
        schedule(jobId.hashCode(), intent, time.todayMillis())
    }

    fun schedulePlannedOfficeReminder(jobId: String, jobName: String, date: LocalDate, time: LocalTime) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_OFFICE_REMINDER
            putExtra(EXTRA_JOB_ID, jobId)
            putExtra(EXTRA_JOB_NAME, jobName)
        }
        val triggerMillis = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (triggerMillis <= System.currentTimeMillis()) return
        schedule(jobId.hashCode() xor date.hashCode(), intent, triggerMillis)
    }

    fun cancelPlannedOfficeReminder(jobId: String, date: LocalDate) {
        val intent = Intent(context, NotificationReceiver::class.java).apply { action = ACTION_OFFICE_REMINDER }
        alarmManager.cancel(pendingIntent(jobId.hashCode() xor date.hashCode(), intent))
    }

    fun scheduleGpsFailureReminder(jobId: String, jobName: String, time: LocalTime) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_GPS_FAILURE
            putExtra(EXTRA_JOB_ID, jobId)
            putExtra(EXTRA_JOB_NAME, jobName)
        }
        schedule(jobId.hashCode() + 1000, intent, time.todayMillis())
    }

    fun cancelGpsFailureReminder(jobId: String) {
        val intent = Intent(context, NotificationReceiver::class.java).apply { action = ACTION_GPS_FAILURE }
        alarmManager.cancel(pendingIntent(jobId.hashCode() + 1000, intent))
    }

    fun scheduleGoalAlert(jobId: String, jobName: String, daysNeeded: Int) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_GOAL_ALERT
            putExtra(EXTRA_JOB_ID, jobId)
            putExtra(EXTRA_JOB_NAME, jobName)
            putExtra(EXTRA_DAYS_NEEDED, daysNeeded)
        }
        schedule(jobId.hashCode() + 2000, intent, System.currentTimeMillis() + 1000)
    }

    fun scheduleWeeklySummary(summaryText: String) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_WEEKLY_SUMMARY
            putExtra(EXTRA_SUMMARY_TEXT, summaryText)
        }
        schedule(WEEKLY_SUMMARY_REQUEST_CODE, intent, System.currentTimeMillis() + 1000)
    }

    fun rescheduleAllNotifications(jobs: List<Pair<String, String>>, reminderTime: LocalTime) {
        jobs.forEach { (id, name) ->
            scheduleOfficeDayReminder(id, name, reminderTime)
        }
    }

    private fun LocalTime.todayMillis(): Long =
        LocalDate.now().atTime(this).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    companion object {
        const val ACTION_OFFICE_REMINDER = "com.codekage.showup.v2.OFFICE_REMINDER"
        const val ACTION_GPS_FAILURE = "com.codekage.showup.v2.GPS_FAILURE"
        const val ACTION_GOAL_ALERT = "com.codekage.showup.v2.GOAL_ALERT"
        const val ACTION_WEEKLY_SUMMARY = "com.codekage.showup.v2.WEEKLY_SUMMARY"
        const val EXTRA_JOB_ID = "job_id"
        const val EXTRA_JOB_NAME = "job_name"
        const val EXTRA_DAYS_NEEDED = "days_needed"
        const val EXTRA_SUMMARY_TEXT = "summary_text"
        const val WEEKLY_SUMMARY_REQUEST_CODE = 9999
    }
}
