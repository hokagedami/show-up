package com.codekage.showup.data.local

import androidx.room.TypeConverter
import com.codekage.showup.domain.model.AttendanceRecordStatus
import com.codekage.showup.domain.model.EntryMethod
import com.codekage.showup.domain.model.NonWorkDayType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class Converters {
    @TypeConverter fun fromUUID(uuid: UUID?): String? = uuid?.toString()
    @TypeConverter fun toUUID(s: String?): UUID? = s?.let(UUID::fromString)

    @TypeConverter fun fromLocalDate(d: LocalDate?): String? = d?.toString()
    @TypeConverter fun toLocalDate(s: String?): LocalDate? = s?.let(LocalDate::parse)

    @TypeConverter fun fromLocalTime(t: LocalTime?): String? = t?.toString()
    @TypeConverter fun toLocalTime(s: String?): LocalTime? = s?.let(LocalTime::parse)

    @TypeConverter fun fromLocalDateTime(dt: LocalDateTime?): String? = dt?.toString()
    @TypeConverter fun toLocalDateTime(s: String?): LocalDateTime? = s?.let(LocalDateTime::parse)

    @TypeConverter
    fun fromDayOfWeekList(list: List<DayOfWeek>?): String? =
        list?.joinToString(",") { it.name }

    @TypeConverter
    fun toDayOfWeekList(s: String?): List<DayOfWeek>? = s?.split(",")
        ?.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }

    @TypeConverter fun fromAttendanceRecordStatus(v: AttendanceRecordStatus?): String? = v?.name
    @TypeConverter fun toAttendanceRecordStatus(s: String?): AttendanceRecordStatus? =
        s?.let(AttendanceRecordStatus::valueOf)

    @TypeConverter fun fromEntryMethod(v: EntryMethod?): String? = v?.name
    @TypeConverter fun toEntryMethod(s: String?): EntryMethod? = s?.let(EntryMethod::valueOf)

    @TypeConverter fun fromNonWorkDayType(v: NonWorkDayType?): String? = v?.name
    @TypeConverter fun toNonWorkDayType(s: String?): NonWorkDayType? =
        s?.let(NonWorkDayType::valueOf)
}
