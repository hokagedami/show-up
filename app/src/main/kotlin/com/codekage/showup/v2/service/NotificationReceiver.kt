package com.codekage.showup.v2.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.codekage.showup.v2.OfficeAttendanceApp
import com.codekage.showup.v2.presentation.MainActivity

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val (channel, title, text) = when (intent.action) {
            NotificationScheduler.ACTION_OFFICE_REMINDER -> Triple(
                OfficeAttendanceApp.CHANNEL_OFFICE_REMINDER,
                "Office day reminder",
                "Don't forget — ${intent.getStringExtra(NotificationScheduler.EXTRA_JOB_NAME).orEmpty()} office day today",
            )
            NotificationScheduler.ACTION_GPS_FAILURE -> Triple(
                OfficeAttendanceApp.CHANNEL_GPS_FAILURE,
                "Did you make it in?",
                "GPS didn't detect your office visit. Tap to confirm.",
            )
            NotificationScheduler.ACTION_GOAL_ALERT -> Triple(
                OfficeAttendanceApp.CHANNEL_GOAL_ALERT,
                "Attendance goal at risk",
                "${intent.getStringExtra(NotificationScheduler.EXTRA_JOB_NAME).orEmpty()} — need ${intent.getIntExtra(NotificationScheduler.EXTRA_DAYS_NEEDED, 0)} more office day(s)",
            )
            NotificationScheduler.ACTION_WEEKLY_SUMMARY -> Triple(
                OfficeAttendanceApp.CHANNEL_WEEKLY_SUMMARY,
                "Weekly summary",
                intent.getStringExtra(NotificationScheduler.EXTRA_SUMMARY_TEXT).orEmpty(),
            )
            else -> return
        }

        val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
            ?: Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context, intent.action.hashCode(), openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(intent.action.hashCode(), notification)
    }
}
