package com.codekage.showup.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.codekage.showup.data.local.dao.AttendanceRecordDao
import com.codekage.showup.data.local.dao.JobDao
import com.codekage.showup.data.local.dao.NonWorkDayDao
import com.codekage.showup.data.local.entity.AttendanceRecordEntity
import com.codekage.showup.data.local.entity.JobEntity
import com.codekage.showup.data.local.entity.NonWorkDayEntity

@Database(
    entities = [JobEntity::class, AttendanceRecordEntity::class, NonWorkDayEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao
    abstract fun nonWorkDayDao(): NonWorkDayDao
}
