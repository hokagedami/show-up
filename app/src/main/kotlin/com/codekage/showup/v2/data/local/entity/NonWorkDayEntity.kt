package com.codekage.showup.v2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.codekage.showup.v2.domain.model.NonWorkDayType
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "non_work_days")
data class NonWorkDayEntity(
    @PrimaryKey val id: UUID,
    val jobId: UUID?,
    val date: LocalDate,
    val type: NonWorkDayType,
    val label: String,
)
