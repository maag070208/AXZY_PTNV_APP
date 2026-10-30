package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.InputTokens

/**
 * Campo de texto estándar de la app (fuente única de apariencia).
 *
 * Construido sobre `BasicTextField` (no `OutlinedTextField`) para controlar
 * por completo altura, borde, paddings y evitar el recorte de texto del
 * componente M3 cuando se reduce por debajo de su altura mínima. Tamaño,
 * forma, borde y colores viven en `core/theme/InputTokens.kt`.
 *
 * Look bento: relleno neutro, sin borde en reposo, píldora en una línea
 * (esquina de 20dp en multilínea) y aro de foco en el color primario.
 *
 * El `label` se dibuja como etiqueta compacta ARRIBA del campo (no flotante).
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    /** Etiqueta estable para E2E (Maestro): se expone como `resource-id` de accesibilidad. */
    testTag: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    // Estilo bento: relleno neutro sin borde en reposo; aro solo en foco/error.
    val fieldShape = if (singleLine) InputTokens.Shape else InputTokens.MultilineShape
    val ringColor = when {
        isError -> MaterialTheme.colorScheme.error
        focused -> MaterialTheme.colorScheme.primary
        else -> null
    }
    val containerColor = if (enabled) AppBento.field else AppBento.field.copy(alpha = 0.5f)

    Column(modifier = modifier) {
        if (label != null) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                LocalTextStyle provides MaterialTheme.typography.labelMedium,
            ) {
                Box(modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)) { label() }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .background(containerColor, fieldShape)
                .then(
                    if (ringColor != null) {
                        Modifier.border(InputTokens.RingWidth, ringColor, fieldShape)
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (singleLine) {
                        Modifier.height(InputTokens.Height)
                    } else {
                        Modifier.defaultMinSize(minHeight = InputTokens.MinHeight)
                    }
                )
                .padding(
                    horizontal = InputTokens.ContentPaddingHorizontal,
                    vertical = if (singleLine) 0.dp else InputTokens.ContentPaddingVertical,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(8.dp))
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                readOnly = readOnly,
                singleLine = singleLine,
                maxLines = maxLines,
                minLines = minLines,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                interactionSource = interaction,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder != null) {
                        Box {
                            CompositionLocalProvider(
                                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                            ) { placeholder() }
                            inner()
                        }
                    } else {
                        inner()
                    }
                },
            )

            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                trailingIcon()
            }
        }

        if (supportingText != null) {
            CompositionLocalProvider(
                LocalContentColor provides if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Box(modifier = Modifier.padding(start = 2.dp, top = 4.dp)) { supportingText() }
            }
        }
    }
}

/**
 * Campo de contraseña estándar: oculto por defecto, con botón de ojo a la
 * derecha para mostrar/ocultar.
 */
@Composable
fun AppPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
) {
    var visible by remember { mutableStateOf(false) }
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        label = label,
        placeholder = placeholder,
        isError = isError,
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                )
            }
        },
    )
}
