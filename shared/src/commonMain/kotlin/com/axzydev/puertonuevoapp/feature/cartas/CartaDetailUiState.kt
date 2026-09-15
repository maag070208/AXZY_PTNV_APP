package com.axzydev.puertonuevoapp.feature.cartas

import com.axzydev.puertonuevoapp.core.network.cartas.CartaDto

data class CartaDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val carta: CartaDto? = null,
    val actionSaving: Boolean = false,
    val actionError: String? = null,
    val showDeleteConfirm: Boolean = false,
    val showReturnModal: Boolean = false,
    val showUndoConfirm: Boolean = false,
    val returnedBy: String = "",
    val returnCondition: String = "",
    val deleted: Boolean = false,
)
