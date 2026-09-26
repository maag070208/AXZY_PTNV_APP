package com.axzydev.puertonuevoapp.feature.custodyletters

import com.axzydev.puertonuevoapp.core.network.custodyletters.CustodyLetterDto

data class CustodyLetterDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val custodyLetter: CustodyLetterDto? = null,
    val actionSaving: Boolean = false,
    val actionError: String? = null,
    val showDeleteConfirm: Boolean = false,
    val showReturnModal: Boolean = false,
    val showUndoConfirm: Boolean = false,
    val returnedBy: String = "",
    val returnCondition: String = "",
    val deleted: Boolean = false,
)
