package com.axzydev.puertonuevoapp.feature.schedules

import com.axzydev.puertonuevoapp.core.network.schedules.ScheduleDayDto

/** Un día del formulario de horario (horas "de pared" en texto). */
data class ScheduleDayForm(
    val weekday: Int,
    val restDay: Boolean = false,
    val startTime: String = "08:00",
    val endTime: String = "16:00",
    val split: Boolean = false,
    val splitStartTime: String = "14:00",
    val splitEndTime: String = "15:00",
) {
    val validTimes: Boolean
        get() = if (restDay) {
            true
        } else {
            isValidTime(startTime) && isValidTime(endTime) && startTime != endTime &&
                (!split || (isValidTime(splitStartTime) && isValidTime(splitEndTime) && splitStartTime != splitEndTime))
        }

    fun toDto(): ScheduleDayDto = ScheduleDayDto(
        weekday = weekday,
        startTime = if (restDay) null else startTime,
        endTime = if (restDay) null else endTime,
        splitStartTime = if (restDay || !split) null else splitStartTime,
        splitEndTime = if (restDay || !split) null else splitEndTime,
        restDay = restDay,
    )
}

internal fun defaultScheduleDays(): List<ScheduleDayForm> =
    (1..7).map { weekday ->
        ScheduleDayForm(weekday = weekday, restDay = weekday == 7)
    }

/**
 * Estado del alta/edición de un horario. El formulario es una rejilla de siete
 * días (laboral/descanso, entrada/salida y segundo tramo opcional) más las
 * tolerancias y el mínimo de tiempo extra, con el resumen semanal calculado en
 * vivo igual que la web.
 */
data class ScheduleFormUiState(
    val creating: Boolean = true,
    val loading: Boolean = false,
    val notFound: Boolean = false,
    val error: String? = null,
    /** Mensaje de validación del último intento de guardar. */
    val validationError: String? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val name: String = "",
    val entryToleranceText: String = "10",
    val exitToleranceText: String = "10",
    val mealBreakText: String = "0",
    val minOvertimeText: String = "60",
    val crossesMidnight: Boolean = false,
    val days: List<ScheduleDayForm> = defaultScheduleDays(),
) {
    val dtoDays: List<ScheduleDayDto> get() = days.map { it.toDto() }

    val workedDays: Int get() = days.count { !it.restDay }

    val weeklyMinutesTotal: Int
        get() = weeklyMinutes(dtoDays, mealBreakText.toIntOrNull() ?: 0)

    /** true si alguna jornada termina antes de empezar (cruza medianoche). */
    val crossesMidnightDetected: Boolean
        get() = days.any { !it.restDay && isValidTime(it.startTime) && isValidTime(it.endTime) &&
            timeToMinutes(it.endTime) < timeToMinutes(it.startTime) }
}
