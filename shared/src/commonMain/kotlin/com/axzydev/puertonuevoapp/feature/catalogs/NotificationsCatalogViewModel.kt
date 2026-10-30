package com.axzydev.puertonuevoapp.feature.catalogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.catalogs.CatalogsApi
import com.axzydev.puertonuevoapp.core.network.catalogs.SysConfigKeys
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Fallback de la app cuando `WEEK_START_DAY` no existe o no es válido (igual que la web). */
const val DEFAULT_WEEK_START_DAY: String = "WEDNESDAY"

/** Días que acepta `WEEK_START_DAY`, en el orden de `Date.getDay()` de la web. */
val weekStartDayOptions: List<Pair<String, String>> = listOf(
    "SUNDAY" to "Domingo",
    "MONDAY" to "Lunes",
    "TUESDAY" to "Martes",
    "WEDNESDAY" to "Miércoles",
    "THURSDAY" to "Jueves",
    "FRIDAY" to "Viernes",
    "SATURDAY" to "Sábado",
)

/** Validación simple de correo, la misma que usa la web antes de guardar. */
private val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/**
 * Estado de la pestaña de configuración de notificaciones: los tres ajustes de
 * `sys_config` que la web edita (`EMAIL_NOTIFICATION_RECIPIENTS`,
 * `ENABLE_SEND_EMAIL` y `WEEK_START_DAY`), cada uno con su borrador y su guardado.
 */
data class NotificationsCatalogUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val recipients: String = "",
    val recipientsDraft: String = "",
    val sendEmail: Boolean = true,
    val sendEmailDraft: Boolean = true,
    val weekStartDay: String = DEFAULT_WEEK_START_DAY,
    val weekStartDraft: String = DEFAULT_WEEK_START_DAY,
    val savingRecipients: Boolean = false,
    val savingSendEmail: Boolean = false,
    val savingWeekStart: Boolean = false,
    val actionError: String? = null,
) {
    val recipientsDirty: Boolean get() = recipientsDraft != recipients
    val sendEmailDirty: Boolean get() = sendEmailDraft != sendEmail
    val weekStartDirty: Boolean get() = weekStartDraft != weekStartDay
}

/**
 * Configuración de notificaciones (`sys-config`).
 *
 * La lectura de una clave suelta está abierta a la sesión, así que la pestaña
 * puede mostrar los valores; guardar exige `system.configure` (lo valida el API
 * y la UI esconde los botones). `GET /sys-config` completo no se usa aquí para
 * que la pestaña funcione sin ese permiso.
 */
class NotificationsCatalogViewModel(
    private val catalogsApi: CatalogsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsCatalogUiState())
    val uiState: StateFlow<NotificationsCatalogUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val failures = mutableListOf<Throwable>()

            suspend fun read(key: String, fallback: String): String =
                runCatching { catalogsApi.get(key).value }
                    .getOrElse { error ->
                        failures += error
                        fallback
                    }

            val recipients = read(SysConfigKeys.EMAIL_NOTIFICATION_RECIPIENTS, "")
            val sendEmailRaw = read(SysConfigKeys.ENABLE_SEND_EMAIL, "true")
            val weekStartRaw = read(SysConfigKeys.WEEK_START_DAY, DEFAULT_WEEK_START_DAY)
            val weekStart = weekStartRaw.trim().uppercase()
                .takeIf { raw -> weekStartDayOptions.any { it.first == raw } }
                ?: DEFAULT_WEEK_START_DAY
            val sendEmail = !sendEmailRaw.trim().equals("false", ignoreCase = true)

            _uiState.update {
                it.copy(
                    loading = false,
                    // Solo es error si no se pudo leer ninguno de los tres ajustes.
                    error = if (failures.size == 3) networkMessage(failures.first()) else null,
                    recipients = recipients,
                    recipientsDraft = recipients,
                    sendEmail = sendEmail,
                    sendEmailDraft = sendEmail,
                    weekStartDay = weekStart,
                    weekStartDraft = weekStart,
                )
            }
        }
    }

    // ── Destinatarios CC ─────────────────────────────────────────────────────

    fun onRecipientsChange(value: String) =
        _uiState.update { it.copy(recipientsDraft = value, actionError = null) }

    fun resetRecipients() =
        _uiState.update { it.copy(recipientsDraft = it.recipients, actionError = null) }

    fun saveRecipients() {
        val state = _uiState.value
        if (state.savingRecipients || !state.recipientsDirty) return
        val tokens = state.recipientsDraft.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val invalid = tokens.filterNot { emailRegex.matches(it) }
        if (invalid.isNotEmpty()) {
            _uiState.update { it.copy(actionError = "Correo inválido: ${invalid.joinToString(", ")}") }
            return
        }
        if (tokens.map { it.lowercase() }.distinct().size != tokens.size) {
            _uiState.update { it.copy(actionError = "Hay correos repetidos en la lista.") }
            return
        }
        val value = tokens.joinToString(", ")
        _uiState.update { it.copy(savingRecipients = true, actionError = null) }
        viewModelScope.launch {
            val result = runCatching { catalogsApi.setEmailRecipients(value) }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(savingRecipients = false, recipients = value, recipientsDraft = value) }
                    AppSnackbar.show("Destinatarios guardados")
                },
                onFailure = { error ->
                    _uiState.update { it.copy(savingRecipients = false, actionError = networkMessage(error)) }
                },
            )
        }
    }

    // ── Interruptor del correo ───────────────────────────────────────────────

    fun onSendEmailChange(enabled: Boolean) =
        _uiState.update { it.copy(sendEmailDraft = enabled, actionError = null) }

    fun resetSendEmail() =
        _uiState.update { it.copy(sendEmailDraft = it.sendEmail, actionError = null) }

    fun saveSendEmail() {
        val state = _uiState.value
        if (state.savingSendEmail || !state.sendEmailDirty) return
        _uiState.update { it.copy(savingSendEmail = true, actionError = null) }
        viewModelScope.launch {
            val result = runCatching { catalogsApi.setSendEmail(state.sendEmailDraft) }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(savingSendEmail = false, sendEmail = state.sendEmailDraft) }
                    AppSnackbar.show("Configuración de correo guardada")
                },
                onFailure = { error ->
                    _uiState.update { it.copy(savingSendEmail = false, actionError = networkMessage(error)) }
                },
            )
        }
    }

    // ── Primer día de la semana ──────────────────────────────────────────────

    fun onWeekStartChange(value: String) =
        _uiState.update { it.copy(weekStartDraft = value, actionError = null) }

    fun resetWeekStart() =
        _uiState.update { it.copy(weekStartDraft = it.weekStartDay, actionError = null) }

    fun saveWeekStart() {
        val state = _uiState.value
        if (state.savingWeekStart || !state.weekStartDirty) return
        _uiState.update { it.copy(savingWeekStart = true, actionError = null) }
        viewModelScope.launch {
            val result = runCatching { catalogsApi.setWeekStartDay(state.weekStartDraft) }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(savingWeekStart = false, weekStartDay = state.weekStartDraft) }
                    AppSnackbar.show("Primer día de la semana guardado")
                },
                onFailure = { error ->
                    _uiState.update { it.copy(savingWeekStart = false, actionError = networkMessage(error)) }
                },
            )
        }
    }

    fun dismissActionError() = _uiState.update { it.copy(actionError = null) }
}
