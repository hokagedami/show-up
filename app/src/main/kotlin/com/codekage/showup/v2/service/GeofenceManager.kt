package com.codekage.showup.v2.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.codekage.showup.v2.domain.model.Job
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Per-job state of the geofence pipeline. Surfaced on the Dashboard so the user can act. */
sealed class GeofenceState {
    data object Armed : GeofenceState()
    data object NotArmed : GeofenceState()
    data object NeedsForegroundLocation : GeofenceState()
    data object NeedsBackgroundLocation : GeofenceState()
    data object LocationServicesOff : GeofenceState()
    data class InvalidCoordinates(val lat: Double, val lng: Double) : GeofenceState()
    data class Error(val code: Int, val message: String) : GeofenceState()
}

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val _states = MutableStateFlow<Map<String, GeofenceState>>(emptyMap())
    val states: StateFlow<Map<String, GeofenceState>> = _states.asStateFlow()

    private val geofencePendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    /** True only when the OS allows the app to register & fire geofences in the background. */
    fun preflight(): GeofenceState? {
        if (!hasFineLocation()) return GeofenceState.NeedsForegroundLocation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocation()) {
            return GeofenceState.NeedsBackgroundLocation
        }
        if (!isLocationServicesOn()) return GeofenceState.LocationServicesOff
        return null
    }

    fun registerGeofence(job: Job) {
        val jobId = job.id.toString()

        // Reject obviously-default coordinates (the seeded DfE backup uses 0/0).
        if (job.officeLat == 0.0 && job.officeLng == 0.0) {
            update(jobId, GeofenceState.InvalidCoordinates(job.officeLat, job.officeLng))
            return
        }

        preflight()?.let {
            update(jobId, it)
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(jobId)
            .setCircularRegion(job.officeLat, job.officeLng, job.geofenceRadiusMeters.toFloat())
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener { update(jobId, GeofenceState.Armed) }
                .addOnFailureListener { e ->
                    val code = (e as? com.google.android.gms.common.api.ApiException)?.statusCode ?: -1
                    val msg = if (code != -1) GeofenceStatusCodes.getStatusCodeString(code) else (e.message ?: "Unknown")
                    Log.w(TAG, "addGeofences failed for $jobId: $msg")
                    update(jobId, GeofenceState.Error(code, msg))
                }
        } catch (se: SecurityException) {
            Log.w(TAG, "addGeofences SecurityException for $jobId", se)
            update(jobId, GeofenceState.NeedsForegroundLocation)
        }
    }

    fun removeGeofence(jobId: String) {
        geofencingClient.removeGeofences(listOf(jobId))
        _states.update { it - jobId }
    }

    fun removeAllGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent)
        _states.value = emptyMap()
    }

    private fun update(jobId: String, state: GeofenceState) {
        _states.update { it + (jobId to state) }
    }

    private fun hasFineLocation(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    private fun hasBackgroundLocation(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    private fun isLocationServicesOn(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private companion object { const val TAG = "GeofenceManager" }
}
