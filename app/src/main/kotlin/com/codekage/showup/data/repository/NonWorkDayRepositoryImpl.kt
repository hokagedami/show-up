package com.codekage.showup.data.repository

import com.codekage.showup.data.local.dao.NonWorkDayDao
import com.codekage.showup.data.local.entity.toDomain
import com.codekage.showup.data.local.entity.toEntity
import com.codekage.showup.domain.model.NonWorkDay
import com.codekage.showup.domain.repository.NonWorkDayRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class NonWorkDayRepositoryImpl(
    private val nonWorkDayDao: NonWorkDayDao,
) : NonWorkDayRepository {
    override fun getNonWorkDaysForJob(jobId: UUID?): Flow<List<NonWorkDay>> =
        nonWorkDayDao.getNonWorkDaysForJob(jobId)
            .map { list -> list.map { it.toDomain() } }

    override fun getNonWorkDaysInRange(
        jobId: UUID?, startDate: LocalDate, endDate: LocalDate,
    ): Flow<List<NonWorkDay>> =
        nonWorkDayDao.getNonWorkDaysInRange(jobId, startDate, endDate)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getNonWorkDayForDate(date: LocalDate, jobId: UUID?): NonWorkDay? =
        nonWorkDayDao.getNonWorkDayForDate(date, jobId)?.toDomain()

    override suspend fun insertNonWorkDay(nonWorkDay: NonWorkDay) =
        nonWorkDayDao.insertNonWorkDay(nonWorkDay.toEntity())

    override suspend fun updateNonWorkDay(nonWorkDay: NonWorkDay) =
        nonWorkDayDao.updateNonWorkDay(nonWorkDay.toEntity())

    override suspend fun deleteNonWorkDay(nonWorkDay: NonWorkDay) =
        nonWorkDayDao.deleteNonWorkDay(nonWorkDay.toEntity())

    override suspend fun insertAll(nonWorkDays: List<NonWorkDay>) =
        nonWorkDayDao.insertAll(nonWorkDays.map { it.toEntity() })

    override suspend fun deleteAllForJob(jobId: UUID) =
        nonWorkDayDao.deleteAllForJob(jobId)
}
