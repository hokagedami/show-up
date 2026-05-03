package com.codekage.showup.v2

import android.content.Context
import androidx.room.Room
import com.codekage.showup.v2.data.local.AppDatabase
import com.codekage.showup.v2.data.remote.HolidayApiService
import com.codekage.showup.v2.data.repository.AttendanceRepositoryImpl
import com.codekage.showup.v2.data.repository.HolidayRepository
import com.codekage.showup.v2.data.repository.JobRepositoryImpl
import com.codekage.showup.v2.data.repository.NonWorkDayRepositoryImpl
import com.codekage.showup.v2.data.repository.PlannedDayRepository
import com.codekage.showup.v2.data.repository.SettingsRepository
import com.codekage.showup.v2.domain.repository.AttendanceRepository
import com.codekage.showup.v2.domain.repository.JobRepository
import com.codekage.showup.v2.domain.repository.NonWorkDayRepository
import com.codekage.showup.v2.domain.usecase.DeleteJobUseCase
import com.codekage.showup.v2.domain.usecase.GetDashboardDataUseCase
import com.codekage.showup.v2.domain.usecase.GetReportDataUseCase
import com.codekage.showup.v2.domain.usecase.MarkAttendanceUseCase
import com.codekage.showup.v2.domain.usecase.SaveJobUseCase
import com.codekage.showup.v2.domain.usecase.SyncHolidaysUseCase
import com.codekage.showup.v2.service.GeofenceManager
import com.codekage.showup.v2.service.NotificationScheduler
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "office_attendance_db").build()
    }

    private val json: Json by lazy {
        Json { ignoreUnknownKeys = true; coerceInputValues = true }
    }

    private val okHttpClient: OkHttpClient by lazy { OkHttpClient.Builder().build() }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://date.nager.at/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private val holidayApiService: HolidayApiService by lazy {
        retrofit.create(HolidayApiService::class.java)
    }

    val jobRepository: JobRepository by lazy { JobRepositoryImpl(database.jobDao()) }
    val attendanceRepository: AttendanceRepository by lazy { AttendanceRepositoryImpl(database.attendanceRecordDao()) }
    val nonWorkDayRepository: NonWorkDayRepository by lazy { NonWorkDayRepositoryImpl(database.nonWorkDayDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val plannedDayRepository: PlannedDayRepository by lazy { PlannedDayRepository(appContext) }
    val holidayRepository: HolidayRepository by lazy { HolidayRepository(holidayApiService, nonWorkDayRepository) }

    val geofenceManager: GeofenceManager by lazy { GeofenceManager(appContext) }
    val notificationScheduler: NotificationScheduler by lazy { NotificationScheduler(appContext) }

    val getDashboardDataUseCase: GetDashboardDataUseCase by lazy {
        GetDashboardDataUseCase(jobRepository, attendanceRepository, nonWorkDayRepository)
    }
    val markAttendanceUseCase: MarkAttendanceUseCase by lazy {
        MarkAttendanceUseCase(attendanceRepository, jobRepository, nonWorkDayRepository, notificationScheduler, settingsRepository)
    }
    val getReportDataUseCase: GetReportDataUseCase by lazy {
        GetReportDataUseCase(attendanceRepository, nonWorkDayRepository)
    }
    val syncHolidaysUseCase: SyncHolidaysUseCase by lazy {
        SyncHolidaysUseCase(holidayRepository, settingsRepository)
    }
    val saveJobUseCase: SaveJobUseCase by lazy { SaveJobUseCase(jobRepository, geofenceManager) }
    val deleteJobUseCase: DeleteJobUseCase by lazy {
        DeleteJobUseCase(jobRepository, attendanceRepository, nonWorkDayRepository, geofenceManager)
    }
    val generatePlanUseCase: com.codekage.showup.v2.domain.usecase.GeneratePlanUseCase by lazy {
        com.codekage.showup.v2.domain.usecase.GeneratePlanUseCase(attendanceRepository, nonWorkDayRepository)
    }
}
