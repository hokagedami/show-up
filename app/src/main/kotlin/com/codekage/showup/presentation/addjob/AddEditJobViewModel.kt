package com.codekage.showup.presentation.addjob

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.codekage.showup.AppContainer
import com.codekage.showup.domain.model.Job
import com.codekage.showup.domain.repository.JobRepository
import com.codekage.showup.domain.usecase.SaveJobUseCase
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job as CoroutineJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.URLEncoder
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AddEditJobViewModel(
    private val jobRepository: JobRepository,
    private val saveJobUseCase: SaveJobUseCase,
    private val context: Context?,
    jobId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditJobUiState())
    val uiState = _uiState.asStateFlow()
    private var existingJobId: UUID? = jobId?.let(UUID::fromString)
    private var geocodeJob: CoroutineJob? = null

    init {
        existingJobId?.let { loadJob(it) }
    }

    private fun loadJob(jobId: UUID) {
        viewModelScope.launch {
            val job = jobRepository.getJobById(jobId) ?: return@launch
            _uiState.value = AddEditJobUiState(
                name = job.name,
                officeAddress = job.officeAddress,
                officeLat = job.officeLat,
                officeLng = job.officeLng,
                officeGoogleMapsLink = buildGoogleMapsLink(job.officeLat, job.officeLng, job.officeAddress),
                geofenceRadiusMeters = job.geofenceRadiusMeters,
                monthlyGoalPercent = job.monthlyGoalPercent,
                workDays = job.workDays.toSet(),
                workStartTime = job.workStartTime,
                workEndTime = job.workEndTime,
                isEditing = true,
            )
        }
    }

    fun updateName(name: String) = _uiState.update { it.copy(name = name) }
    fun updateAddress(address: String) {
        _uiState.update { it.copy(officeAddress = address) }
        // If pasted text contains coordinates (e.g. from a Maps URL), use them directly
        // and skip the slower forward-geocode pass.
        val coords = extractCoordinates(address)
        if (coords != null) {
            geocodeJob?.cancel()
            _uiState.update {
                it.copy(
                    officeLat = coords.first,
                    officeLng = coords.second,
                    officeGoogleMapsLink = buildGoogleMapsLink(coords.first, coords.second, address),
                )
            }
        } else {
            geocodeAddress(address)
        }
    }

    fun useCurrentLocation() {
        val ctx = context ?: return
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            _uiState.update { it.copy(error = "Location permission required") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val client = LocationServices.getFusedLocationProviderClient(ctx)
                val tokenSource = CancellationTokenSource()
                val location = try {
                    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token).await()
                } catch (_: SecurityException) {
                    null
                } ?: client.lastLocation.await()

                if (location != null) {
                    val resolved = runCatching {
                        @Suppress("DEPRECATION")
                        Geocoder(ctx).getFromLocation(location.latitude, location.longitude, 1)
                            ?.firstOrNull()
                            ?.getAddressLine(0)
                    }.getOrNull()
                    val address = resolved ?: "%.6f, %.6f".format(location.latitude, location.longitude)
                    _uiState.update {
                        it.copy(
                            officeLat = location.latitude,
                            officeLng = location.longitude,
                            officeAddress = address,
                            officeGoogleMapsLink = buildGoogleMapsLink(location.latitude, location.longitude, address),
                            error = null,
                        )
                    }
                } else {
                    _uiState.update { it.copy(error = "Couldn't get current location") }
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "Location error: ${e.message}") }
            }
        }
    }

    private fun geocodeAddress(address: String) {
        geocodeJob?.cancel()
        if (address.length < 5 || context == null || !Geocoder.isPresent()) return
        geocodeJob = viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            runCatching {
                @Suppress("DEPRECATION")
                val results = Geocoder(context).getFromLocationName(address, 1)
                results?.firstOrNull()?.let { addr ->
                    _uiState.update {
                        it.copy(
                            officeLat = addr.latitude,
                            officeLng = addr.longitude,
                            officeGoogleMapsLink = buildGoogleMapsLink(addr.latitude, addr.longitude, it.officeAddress),
                        )
                    }
                }
            }
        }
    }

    fun updateLocation(lat: Double, lng: Double) = _uiState.update {
        it.copy(officeLat = lat, officeLng = lng, officeGoogleMapsLink = buildGoogleMapsLink(lat, lng, it.officeAddress))
    }
    fun updateGeofenceRadius(radius: Int) = _uiState.update { it.copy(geofenceRadiusMeters = radius) }
    fun updateGoalPercent(percent: Int) = _uiState.update { it.copy(monthlyGoalPercent = percent) }
    fun toggleWorkDay(day: DayOfWeek) = _uiState.update {
        val next = it.workDays.toMutableSet().apply { if (!add(day)) remove(day) }
        it.copy(workDays = next)
    }
    fun updateStartTime(time: LocalTime) = _uiState.update { it.copy(workStartTime = time) }
    fun updateEndTime(time: LocalTime) = _uiState.update { it.copy(workEndTime = time) }

    fun saveJob() {
        val state = _uiState.value
        when {
            state.name.isBlank() -> _uiState.update { it.copy(error = "Job name is required") }
            state.officeAddress.isBlank() -> _uiState.update { it.copy(error = "Office address is required") }
            state.workDays.isEmpty() -> _uiState.update { it.copy(error = "At least one work day is required") }
            else -> {
                _uiState.update { it.copy(isSaving = true, error = null) }
                viewModelScope.launch {
                    runCatching {
                        val job = Job(
                            id = existingJobId ?: UUID.randomUUID(),
                            name = state.name,
                            officeAddress = state.officeAddress,
                            officeLat = state.officeLat,
                            officeLng = state.officeLng,
                            geofenceRadiusMeters = state.geofenceRadiusMeters,
                            monthlyGoalPercent = state.monthlyGoalPercent,
                            workDays = state.workDays.toList().sorted(),
                            workStartTime = state.workStartTime,
                            workEndTime = state.workEndTime,
                            isActive = true,
                            createdAt = LocalDateTime.now(),
                        )
                        saveJobUseCase(job, state.isEditing)
                    }.onSuccess {
                        _uiState.update { it.copy(isSaved = true, isSaving = false) }
                    }.onFailure { e ->
                        _uiState.update { it.copy(error = "Failed to save: ${e.message}", isSaving = false) }
                    }
                }
            }
        }
    }

    companion object {
        private val COORD_REGEX = Regex("""(-?\d{1,3}\.\d+)\s*,\s*(-?\d{1,3}\.\d+)""")

        /** Pull a (lat, lng) pair out of arbitrary text — Maps URLs (`@lat,lng`, `q=lat,lng`,
         *  `query=lat,lng`) and bare `lat,lng` all match. Range-checked to avoid
         *  matching things like "8.99,400.0" from prices or version strings. */
        internal fun extractCoordinates(text: String): Pair<Double, Double>? {
            COORD_REGEX.findAll(text).forEach { match ->
                val lat = match.groupValues[1].toDoubleOrNull() ?: return@forEach
                val lng = match.groupValues[2].toDoubleOrNull() ?: return@forEach
                if (lat in -90.0..90.0 && lng in -180.0..180.0) return lat to lng
            }
            return null
        }

        private fun buildGoogleMapsLink(lat: Double, lng: Double, address: String): String? {
            if (lat == 0.0 && lng == 0.0) return null
            val label = address.ifBlank { "$lat,$lng" }
            val query = URLEncoder.encode("$lat,$lng ($label)", Charsets.UTF_8.name())
            return "https://www.google.com/maps/search/?api=1&query=$query"
        }

        private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
            addOnSuccessListener { cont.resume(it) }
            addOnFailureListener { cont.resumeWithException(it) }
        }

        fun factory(
            appContainer: AppContainer,
            context: Context,
            jobId: String?,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AddEditJobViewModel(
                    jobRepository = appContainer.jobRepository,
                    saveJobUseCase = appContainer.saveJobUseCase,
                    context = context.applicationContext,
                    jobId = jobId,
                )
            }
        }
    }
}
