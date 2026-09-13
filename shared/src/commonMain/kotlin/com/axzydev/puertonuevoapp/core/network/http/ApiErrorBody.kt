package com.axzydev.puertonuevoapp.core.network.http

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorBody(
    val error: String? = null,
    val message: String? = null,
)