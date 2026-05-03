package com.codekage.showup.domain.model

import java.time.LocalDate
import java.util.UUID

data class NonWorkDay(
    val id: UUID,
    val jobId: UUID?,
    val date: LocalDate,
    val type: NonWorkDayType,
    val label: String,
)
