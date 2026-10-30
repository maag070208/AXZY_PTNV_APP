package com.axzydev.puertonuevoapp.feature.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.ui.BentoState

/** Tablero de expedientes (lista + avisos de documentos faltantes). Esqueleto de la Fase 2 (lo reemplaza la tarea de pantalla). */
@Composable
fun EmployeeRecordsScreen() {
    Box(modifier = Modifier.fillMaxSize().background(AppBento.canvas).padding(16.dp)) {
        BentoState(message = "Expedientes: en construcción")
    }
}
