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

/** Detalle de un acta administrativa. Esqueleto de la Fase 2 (lo reemplaza la tarea de pantalla). */
@Composable
fun DisciplinaryReportDetailScreen(reportId: String) {
    Box(modifier = Modifier.fillMaxSize().background(AppBento.canvas).padding(16.dp)) {
        BentoState(message = "Acta administrativa: en construcción")
    }
}
