package com.codekage.showup.data.local.entity

import com.codekage.showup.domain.model.AttendanceRecord
import com.codekage.showup.domain.model.Job
import com.codekage.showup.domain.model.NonWorkDay

fun JobEntity.toDomain(): Job = Job(
    id = id,
    name = name,
    officeAddress = officeAddress,
    officeLat = officeLat,
    officeLng = officeLng,
    geofenceRadiusMeters = geofenceRadiusMeters,
    monthlyGoalPercent = monthlyGoalPercent,
    workDays = workDays,
    workStartTime = workStartTime,
    workEndTime = workEndTime,
    isActive = isActive,
    createdAt = createdAt,
)

fun Job.toEntity(): JobEntity = JobEntity(
    id = id,
    name = name,
    officeAddress = officeAddress,
    officeLat = officeLat,
    officeLng = officeLng,
    geofenceRadiusMeters = geofenceRadiusMeters,
    monthlyGoalPercent = monthlyGoalPercent,
    workDays = workDays,
    workStartTime = workStartTime,
    workEndTime = workEndTime,
    isActive = isActive,
    createdAt = createdAt,
)

fun AttendanceRecordEntity.toDomain(): AttendanceRecord = AttendanceRecord(
    id = id,
    jobId = jobId,
    date = date,
    status = status,
    entryMethod = entryMethod,
    gpsConfirmed = gpsConfirmed,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun AttendanceRecord.toEntity(): AttendanceRecordEntity = AttendanceRecordEntity(
    id = id,
    jobId = jobId,
    date = date,
    status = status,
    entryMethod = entryMethod,
    gpsConfirmed = gpsConfirmed,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun NonWorkDayEntity.toDomain(): NonWorkDay = NonWorkDay(
    id = id,
    jobId = jobId,
    date = date,
    type = type,
    label = label,
)

fun NonWorkDay.toEntity(): NonWorkDayEntity = NonWorkDayEntity(
    id = id,
    jobId = jobId,
    date = date,
    type = type,
    label = label,
)
