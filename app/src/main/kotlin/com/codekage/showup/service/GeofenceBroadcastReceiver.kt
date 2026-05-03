package com.codekage.showup.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.codekage.showup.OfficeAttendanceApp
import com.codekage.showup.domain.model.AttendanceRecordStatus
import com.codekage.showup.domain.model.EntryMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.e(TAG, "Geofence error ${GeofenceStatusCodes.getStatusCodeString(event.errorCode)}")
            return
        }
        val triggering = event.triggeringGeofences ?: return
        val app = context.applicationContext as? OfficeAttendanceApp ?: return
        val container = app.appContainer
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                triggering.forEach { gf ->
                    val jobId = runCatching { UUID.fromString(gf.requestId) }.getOrNull() ?: return@forEach
                    val today = LocalDate.now()
                    val existing = container.attendanceRepository.getAttendanceRecordForDay(jobId, today)
                    if (existing == null) {
                        container.markAttendanceUseCase(jobId, today, AttendanceRecordStatus.OFFICE, EntryMethod.AUTO_GPS)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object { const val TAG = "GeofenceReceiver" }
}
