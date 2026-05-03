package com.codekage.showup.v2.data.local

import com.codekage.showup.v2.domain.model.AttendanceRecordStatus
import com.codekage.showup.v2.domain.model.EntryMethod
import com.codekage.showup.v2.domain.model.NonWorkDayType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `uuid round trip`() {
        val uuid = UUID.randomUUID()
        val s = converters.fromUUID(uuid)
        assertEquals(uuid, converters.toUUID(s))
    }

    @Test
    fun `null uuid round trip`() {
        assertNull(converters.fromUUID(null))
        assertNull(converters.toUUID(null))
    }

    @Test
    fun `local date round trip`() {
        val date = LocalDate.of(2026, 5, 1)
        assertEquals(date, converters.toLocalDate(converters.fromLocalDate(date)))
    }

    @Test
    fun `local time round trip`() {
        val time = LocalTime.of(8, 0)
        assertEquals(time, converters.toLocalTime(converters.fromLocalTime(time)))
    }

    @Test
    fun `local date time round trip`() {
        val dt = LocalDateTime.of(2026, 5, 1, 8, 30, 15)
        assertEquals(dt, converters.toLocalDateTime(converters.fromLocalDateTime(dt)))
    }

    @Test
    fun `dayOfWeek list round trip preserves order`() {
        val list = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        assertEquals(list, converters.toDayOfWeekList(converters.fromDayOfWeekList(list)))
    }

    @Test
    fun `dayOfWeek list ignores invalid entries`() {
        val parsed = converters.toDayOfWeekList("MONDAY,GARBAGE,FRIDAY")
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), parsed)
    }

    @Test
    fun `attendance status round trip`() {
        AttendanceRecordStatus.entries.forEach { status ->
            assertEquals(status, converters.toAttendanceRecordStatus(converters.fromAttendanceRecordStatus(status)))
        }
    }

    @Test
    fun `entry method round trip`() {
        EntryMethod.entries.forEach { method ->
            assertEquals(method, converters.toEntryMethod(converters.fromEntryMethod(method)))
        }
    }

    @Test
    fun `non-work-day type round trip`() {
        NonWorkDayType.entries.forEach { type ->
            assertEquals(type, converters.toNonWorkDayType(converters.fromNonWorkDayType(type)))
        }
    }
}
