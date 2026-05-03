package com.codekage.showup.domain.repository

import com.codekage.showup.domain.model.NonWorkDay
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

interface NonWorkDayRepository {
    fun getNonWorkDaysForJob(jobId: UUID?): Flow<List<NonWorkDay>>
    fun getNonWorkDaysInRange(jobId: UUID?, startDate: LocalDate, endDate: LocalDate): Flow<List<NonWorkDay>>
    suspend fun getNonWorkDayForDate(date: LocalDate, jobId: UUID?): NonWorkDay?
    suspend fun insertNonWorkDay(nonWorkDay: NonWorkDay)
    suspend fun updateNonWorkDay(nonWorkDay: NonWorkDay)
    suspend fun deleteNonWorkDay(nonWorkDay: NonWorkDay)
    suspend fun insertAll(nonWorkDays: List<NonWorkDay>)
    suspend fun deleteAllForJob(jobId: UUID)
}
