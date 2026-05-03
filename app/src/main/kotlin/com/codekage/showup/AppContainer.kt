package com.codekage.showup

import android.content.Context
import androidx.room.Room
import com.codekage.showup.data.local.AppDatabase
import com.codekage.showup.data.remote.HolidayApiService
import com.codekage.showup.data.repository.AttendanceRepositoryImpl
import com.codekage.showup.data.repository.HolidayRepository
import com.codekage.showup.data.repository.JobRepositoryImpl
import com.codekage.showup.data.repository.NonWorkDayRepositoryImpl
import com.codekage.showup.data.repository.PlannedDayRepository
import com.codekage.showup.data.repository.SettingsRepository
import com.codekage.showup.domain.repository.AttendanceRepository
import com.codekage.showup.domain.repository.JobRepository
import com.codekage.showup.domain.repository.NonWorkDayRepository
import com.codekage.showup.domain.usecase.DeleteJobUseCase
import com.codekage.showup.domain.usecase.GetDashboardDataUseCase
import com.codekage.showup.domain.usecase.GetReportDataUseCase
import com.codekage.showup.domain.usecase.MarkAttendanceUseCase
import com.codekage.showup.domain.usecase.SaveJobUseCase
import com.codekage.showup.domain.usecase.SyncHolidaysUseCase
import com.codekage.showup.service.GeofenceManager
import com.codekage.showup.service.NotificationScheduler
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
    val generatePlanUseCase: com.codekage.showup.domain.usecase.GeneratePlanUseCase by lazy {
        com.codekage.showup.domain.usecase.GeneratePlanUseCase(attendanceRepository, nonWorkDayRepository)
    }
}
