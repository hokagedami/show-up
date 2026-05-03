package com.codekage.showup.data.local.entity

import com.codekage.showup.domain.model.AttendanceRecordStatus
import com.codekage.showup.domain.model.EntryMethod
import com.codekage.showup.domain.model.Job
import com.codekage.showup.domain.model.NonWorkDay
import com.codekage.showup.domain.model.NonWorkDayType
import com.codekage.showup.domain.model.AttendanceRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class EntityMappersTest {

    @Test
    fun `Job round-trips through entity`() {
        val job = Job(
            id = UUID.randomUUID(),
            name = "DfE",
            officeAddress = "Bishopsgate House",
            officeLat = 54.5,
            officeLng = -1.5,
            geofenceRadiusMeters = 150,
            monthlyGoalPercent = 45,
            workDays = listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            workStartTime = LocalTime.of(8, 0),
            workEndTime = LocalTime.of(16, 0),
            isActive = true,
            createdAt = LocalDateTime.of(2026, 1, 1, 9, 0),
        )
        assertEquals(job, job.toEntity().toDomain())
    }

    @Test
    fun `AttendanceRecord round-trips through entity`() {
        val record = AttendanceRecord(
            id = UUID.randomUUID(),
            jobId = UUID.randomUUID(),
            date = LocalDate.of(2026, 5, 1),
            status = AttendanceRecordStatus.OFFICE,
            entryMethod = EntryMethod.AUTO_GPS,
            gpsConfirmed = true,
            notes = "in by 0830",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        assertEquals(record, record.toEntity().toDomain())
    }

    @Test
    fun `NonWorkDay with null jobId round-trips`() {
        val nwd = NonWorkDay(
            id = UUID.randomUUID(),
            jobId = null,
            date = LocalDate.of(2026, 12, 25),
            type = NonWorkDayType.BANK_HOLIDAY,
            label = "Christmas Day",
        )
        assertEquals(nwd, nwd.toEntity().toDomain())
    }
}
