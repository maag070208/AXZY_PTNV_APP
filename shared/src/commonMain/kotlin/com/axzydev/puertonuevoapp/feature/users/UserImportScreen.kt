package com.axzydev.puertonuevoapp.feature.users

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.platform.FileMimeTypes
import com.axzydev.puertonuevoapp.core.platform.rememberFilePicker
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatFileSize

/**
 * Carga masiva de cuentas por Excel (`/users/import` de la web).
 *
 * Asistente de dos pasos más el resultado: se elige el `.xlsx`, se revisan los
 * requisitos y el archivo elegido, y al confirmar se sube por multipart. La API
 * crea cada fila válida con rol EMPLEADO y devuelve las omitidas con su motivo
 * (usuario repetido, datos faltantes o contraseña corta), que se listan al
 * final. No hay plantilla ni previsualización en el API: el formato se explica
 * en pantalla.
 */
@Composable
fun UserImportScreen(
    viewModel: UserImportViewModel = viewModel { UserImportViewModel(AppContainer.usersApi) },
) {
    val state by viewModel.uiState.collectAsState()
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val navigator = LocalNavigator.current

    if (user == null || !user.can(PermissionKeys.USERS_CREATE)) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            BentoState(
                message = "No autorizado — se requiere el permiso Crear usuarios",
                tone = BentoTone.Rose,
                icon = Icons.Filled.Lock,
            )
        }
        return
    }

    // El selector filtra por Excel .xlsx, el formato que lee el API.
    val pickFile = rememberFilePicker(
        mimeTypes = listOf(FileMimeTypes.EXCEL),
        onPicked = viewModel::onFilePicked,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ImportFormatCard()
        ImportSteps(state.step)

        when (state.step) {
            UserImportStep.Choose -> ImportChooseCard(onPick = pickFile)

            UserImportStep.Review -> ImportReviewCard(
                state = state,
                onChangeFile = pickFile,
                onConfirm = viewModel::confirm,
            )

            UserImportStep.Done -> ImportResultCard(
                state = state,
                onAnother = viewModel::pickAnother,
                onBackToList = { navigator.pop() },
            )
        }
    }
}

/** Indicador de pasos del asistente: elegir → revisar → resultado. */
@Composable
private fun ImportSteps(step: UserImportStep) {
    val steps = listOf(
        UserImportStep.Choose to "1. Elegir archivo",
        UserImportStep.Review to "2. Revisar",
        UserImportStep.Done to "3. Resultado",
    )
    val currentIndex = steps.indexOfFirst { it.first == step }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        steps.forEachIndexed { index, (_, label) ->
            StatusChip(
                label = label,
                tone = when {
                    index == currentIndex -> BentoTone.Sea
                    index < currentIndex -> BentoTone.Mint
                    else -> BentoTone.Plain
                },
            )
        }
    }
}

/** Formato esperado del archivo (la API no sirve plantilla). */
@Composable
private fun ImportFormatCard() {
    val tone = AppBento.tone(BentoTone.Sea)
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = tone.container,
        borderColor = null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Description, contentDescription = null, tint = tone.content)
            Spacer(Modifier.size(10.dp))
            Text(
                "Formato del archivo",
                style = MaterialTheme.typography.titleSmall,
                color = tone.content,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Un libro .xlsx con una fila por persona y los encabezados en la primera fila.",
            style = MaterialTheme.typography.bodySmall,
            color = tone.content.copy(alpha = 0.9f),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Columnas: NOMBRE DEL EMPLEADO, NOMBRE DE USUARIO y CONTRASEÑA. Cada fila crea una cuenta con rol Empleado.",
            style = MaterialTheme.typography.bodySmall,
            color = tone.content.copy(alpha = 0.9f),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Las filas repetidas, incompletas o con contraseña de menos de 6 caracteres se omiten y se reportan al final.",
            style = MaterialTheme.typography.bodySmall,
            color = tone.content.copy(alpha = 0.75f),
        )
    }
}

/** Paso 1: elegir el archivo. */
@Composable
private fun ImportChooseCard(onPick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Paso 1 de 2 · Elige el archivo",
            style = MaterialTheme.typography.titleSmall,
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Puedes cambiarlo antes de confirmar la carga; todavía no se sube nada.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        Spacer(Modifier.height(14.dp))
        PillButton(
            text = "Elegir archivo .xlsx",
            onClick = onPick,
            leadingIcon = Icons.Filled.UploadFile,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Paso 2: revisar el archivo elegido y confirmar la carga. */
@Composable
private fun ImportReviewCard(
    state: UserImportUiState,
    onChangeFile: () -> Unit,
    onConfirm: () -> Unit,
) {
    val file = state.file
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Paso 2 de 2 · Revisa y confirma",
            style = MaterialTheme.typography.titleSmall,
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.UploadFile,
                contentDescription = null,
                tint = AppColors.EmeraldPrimary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file?.name ?: "Sin archivo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                )
                Text(
                    file?.let { "${it.mime} · ${formatFileSize(it.sizeBytes)}" } ?: "Elige un archivo .xlsx",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                )
            }
            StatusChip(label = ".xlsx", tone = BentoTone.Mint)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Al confirmar se crean las cuentas del archivo. Las filas con problemas no detienen la carga: se informan al final.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        state.error?.let { message ->
            Spacer(Modifier.height(10.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PillButton(
                text = "Cambiar archivo",
                onClick = onChangeFile,
                variant = PillButtonVariant.Secondary,
                enabled = !state.uploading,
            )
            PillButton(
                text = "Importar cuentas",
                onClick = onConfirm,
                enabled = state.canImport,
                loading = state.uploading,
            )
        }
    }
}

/** Resultado: creadas, omitidas y vuelta a la lista. */
@Composable
private fun ImportResultCard(
    state: UserImportUiState,
    onAnother: () -> Unit,
    onBackToList: () -> Unit,
) {
    val result = state.result
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AppColors.Success)
            Spacer(Modifier.size(10.dp))
            Text(
                "Resultado de la carga",
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(
                label = if (result?.created == 1) "1 cuenta creada" else "${result?.created ?: 0} cuentas creadas",
                tone = BentoTone.Mint,
            )
            if (state.skippedCount > 0) {
                StatusChip(
                    label = if (state.skippedCount == 1) "1 fila omitida" else "${state.skippedCount} filas omitidas",
                    tone = BentoTone.Sun,
                )
            }
        }

        if (result != null && result.created == 0 && result.skipped.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "El archivo no trajo filas de datos. Revisa que los encabezados estén en la primera fila.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
        }

        if (result != null && result.skipped.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = AppColors.Warning,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Filas que no se crearon",
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextPrimary,
                )
            }
            Spacer(Modifier.height(8.dp))
            result.skipped.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Text(
                        "Fila ${row.row}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextFaint,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "@${row.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextPrimary,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        "— ${row.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PillButton(
                text = "Importar otro",
                onClick = onAnother,
                variant = PillButtonVariant.Secondary,
            )
            PillButton(
                text = "Volver a la lista",
                onClick = onBackToList,
            )
        }
    }
}
