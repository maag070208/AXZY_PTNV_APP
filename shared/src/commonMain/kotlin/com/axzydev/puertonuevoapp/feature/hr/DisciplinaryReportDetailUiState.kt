package com.axzydev.puertonuevoapp.feature.hr

import com.axzydev.puertonuevoapp.core.network.hr.DisciplinaryReportDto

/** Estado del detalle de un acta administrativa. */
data class DisciplinaryReportDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val report: DisciplinaryReportDto? = null,
    val showDeleteConfirm: Boolean = false,
    val deleting: Boolean = false,
    val deleteError: String? = null,
    /** El acta se eliminó: la pantalla regresa. */
    val deleted: Boolean = false,
)
