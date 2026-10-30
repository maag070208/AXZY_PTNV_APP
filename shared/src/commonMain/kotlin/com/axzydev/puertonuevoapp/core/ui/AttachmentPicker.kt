package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.platform.CSV_MIME_TYPE
import com.axzydev.puertonuevoapp.core.platform.FileMimeTypes
import com.axzydev.puertonuevoapp.core.platform.PickedFile
import com.axzydev.puertonuevoapp.core.platform.rememberFilePicker
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import kotlin.math.roundToInt

/**
 * Fila estándar para elegir un archivo en un formulario (adjunto de ticket,
 * documento de expediente, Excel de carga masiva…). El estado vive fuera: la
 * pantalla/ViewModel guarda el [file] y el/los eventos lo actualizan.
 *
 * Es la variante "controlada": [onPick] abre el selector, que la pantalla
 * obtiene con `rememberFilePicker` (o usa [AttachmentPickerField], que ya
 * cablea el selector). Al elegir un archivo se muestra su nombre y tamaño con
 * un chip de "listo" y las acciones para cambiarlo o quitarlo.
 *
 * ```kotlin
 * val pick = rememberFilePicker { viewModel.attach(it) }
 * AttachmentPickerRow(
 *     label = "Comprobante",
 *     file = state.file,
 *     onPick = pick,
 *     onClear = viewModel::clearFile,
 * )
 * ```
 */
@Composable
fun AttachmentPickerRow(
    label: String,
    file: PickedFile?,
    onPick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true,
) {
    val accent = if (file == null) AppColors.EmeraldPrimary else AppColors.Success
    AppCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(AppShape.icon)
                    .background(accent.copy(alpha = 0.12f), AppShape.icon),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (file == null) Icons.Filled.AttachFile else fileIcon(file.mime),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = file?.let { "${it.name} · ${formatFileSize(it.sizeBytes)}" }
                        ?: hint
                        ?: "Sin archivo",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (file == null) AppColors.TextFaint else AppColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (file != null) {
                Spacer(Modifier.width(8.dp))
                BentoIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Quitar $label",
                    onClick = onClear,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (file == null) {
            PillButton(
                text = "Elegir archivo",
                onClick = onPick,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                leadingIcon = Icons.Filled.UploadFile,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(label = "Listo para subir", tone = BentoTone.Mint)
                Spacer(Modifier.weight(1f))
                PillButton(
                    text = "Cambiar",
                    onClick = onPick,
                    variant = PillButtonVariant.Ghost,
                    enabled = enabled,
                )
            }
        }
    }
}

/**
 * [AttachmentPickerRow] que ya trae el selector de archivos cablado: recuerda
 * `rememberFilePicker` con [mimeTypes] y entrega el archivo elegido a
 * [onFilePicked]. Es la forma corta para una pantalla que no necesita el
 * selector para nada más.
 *
 * ```kotlin
 * AttachmentPickerField(
 *     label = "Adjunto",
 *     file = state.file,
 *     onFilePicked = viewModel::attach,
 *     onClear = viewModel::clearFile,
 *     hint = "PDF o imagen, máx. 10 MB",
 *     mimeTypes = listOf(FileMimeTypes.PDF, FileMimeTypes.IMAGE),
 * )
 * ```
 */
@Composable
fun AttachmentPickerField(
    label: String,
    file: PickedFile?,
    onFilePicked: (PickedFile) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    mimeTypes: List<String> = emptyList(),
    enabled: Boolean = true,
) {
    val pick = rememberFilePicker(mimeTypes = mimeTypes, onPicked = onFilePicked)
    AttachmentPickerRow(
        label = label,
        file = file,
        onPick = { if (enabled) pick() },
        onClear = onClear,
        modifier = modifier,
        hint = hint,
        enabled = enabled,
    )
}

/**
 * Fila de solo lectura para un archivo que ya vive en el API (adjuntos de
 * ticket, documentos de expediente, exportaciones): nombre, subtítulo libre
 * (tipo, tamaño, fecha) y acciones opcionales de compartir/quitar.
 *
 * La descarga la hace la pantalla (p. ej. `ApiClient.getBytes`) y la comparte
 * con `shareFile`; esta fila solo pinta y dispara los callbacks.
 */
@Composable
fun AttachmentFileRow(
    name: String,
    modifier: Modifier = Modifier,
    mime: String = "",
    subtitle: String? = null,
    onShare: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = fileIcon(mime),
                contentDescription = null,
                tint = AppColors.Slate,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextFaint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (onShare != null) {
                BentoIconButton(
                    icon = Icons.Filled.Share,
                    contentDescription = "Compartir $name",
                    onClick = onShare,
                )
            }
            if (onClear != null) {
                BentoIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Quitar $name",
                    onClick = onClear,
                )
            }
        }
    }
}

/** Ícono estándar por MIME (imagen, PDF, Excel/CSV, video, texto u otro). */
fun fileIcon(mime: String): ImageVector = when {
    mime.startsWith("image/") -> Icons.Filled.Image
    mime == FileMimeTypes.PDF -> Icons.Filled.PictureAsPdf
    mime == CSV_MIME_TYPE ||
        mime.contains("spreadsheet") ||
        mime.contains("excel") -> Icons.Filled.TableChart
    mime.startsWith("video/") -> Icons.Filled.Movie
    mime.startsWith("text/") -> Icons.Filled.Description
    else -> Icons.AutoMirrored.Filled.InsertDriveFile
}

/** Tamaño legible para la UI: `12 B`, `340 KB`, `1.2 MB`. */
fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${kb.roundToInt()} KB"
    val mb = kb / 1024.0
    return "${(mb * 10).roundToInt() / 10.0} MB"
}
