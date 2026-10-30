package com.axzydev.puertonuevoapp.feature.langostin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.langostin.LangostinMascot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.ui.text.style.TextOverflow

/** Formulario del panel que se quiere abrir desde fuera del shell. */
enum class LangostinAgentKind { TICKET, TASK }

/** Petición de apertura del panel: qué agente mostrar (y si hay ticket prellenado). */
data class LangostinRequest(
    val agent: LangostinAgentKind,
    /** Solo para [LangostinAgentKind.TASK]: ticket ya elegido (desde su detalle). */
    val ticketId: String? = null,
)

/**
 * Puerta de entrada al panel de Langostín para las pantallas que lo abren sin
 * pasar por el botón de la barra inferior (hoy, el globo proactivo de Tickets).
 *
 * El shell mantiene su propio estado de apertura (su dueño es `AppShell.kt`);
 * este controlador permite que una pantalla pida el panel y que
 * [LangostinLauncherHost] lo pinte mientras el shell no lo centralice. Al
 * cablearlo ahí basta observar [request], abrir el panel y pasar `initialAgent`.
 */
object LangostinLauncher {
    private val _request = MutableStateFlow<LangostinRequest?>(null)
    val request: StateFlow<LangostinRequest?> = _request.asStateFlow()

    fun openTicketAgent() {
        _request.value = LangostinRequest(LangostinAgentKind.TICKET)
    }

    /** Agente de tareas; [ticketId] prellena el paso del ticket (desde su detalle). */
    fun openTaskAgent(ticketId: String? = null) {
        _request.value = LangostinRequest(LangostinAgentKind.TASK, ticketId)
    }

    fun close() {
        _request.value = null
    }
}

/**
 * Descarte del globo proactivo **durante la sesión**: se recuerda por persona,
 * así que al cambiar de cuenta el globo vuelve a ofrecerse y al volver a Tickets
 * con la misma sesión no vuelve a molestar.
 */
object LangostinHints {
    private var dismissedForUserId by mutableStateOf<String?>(null)

    /** ¿Toca ofrecer el globo de ticket nuevo a esta persona? */
    fun showsTicketHint(user: SessionUser?): Boolean =
        user != null && user.canCreateTickets && dismissedForUserId != user.id

    fun dismissTicketHint(user: SessionUser?) {
        dismissedForUserId = user?.id
    }
}

/**
 * Pinta el panel de Langostín cuando una pantalla lo pidió por [LangostinLauncher].
 *
 * Vive dentro de la pantalla que dispara el aviso (Tickets) porque el estado de
 * apertura del shell no es accesible desde aquí; el panel es el mismo
 * [LangostinOverlay], así que la ayuda, el alta de ticket y el agente de tareas
 * funcionan igual. Cuando el shell observe el controlador, este host se retira.
 */
@Composable
fun LangostinLauncherHost() {
    val request by LangostinLauncher.request.collectAsState()
    val active = request ?: return
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user

    LangostinOverlay(
        current = navigator.current,
        user = user,
        onNavigate = { navigator.push(it) },
        onClose = { LangostinLauncher.close() },
        initialAgent = active.agent,
        preselectedTicketId = active.ticketId,
    )
}

/**
 * Globo proactivo de Tickets: la mascota ofrece levantar el ticket ahí mismo.
 * "Nuevo ticket" abre el panel en el agente de ticket y "Ahora no" descarta el
 * aviso durante la sesión (ver [LangostinHints]).
 */
@Composable
internal fun LangostinTicketsHint(
    user: SessionUser?,
    onOpenAgent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = AppColors.Surface,
        borderColor = AppColors.Outline.copy(alpha = 0.6f),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            LangostinMascot(size = 38.dp, animated = true, showBadge = false)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "¿Un ticket nuevo?",
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Te lo levanto yo aquí mismo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(modifier = Modifier.padding(top = 12.dp)) {
            LangostinOptionChips(
                listOf(
                    LangostinChip("Nuevo ticket", Icons.Filled.ConfirmationNumber) {
                        LangostinHints.dismissTicketHint(user)
                        onOpenAgent()
                    },
                    LangostinChip("Ahora no") {
                        LangostinHints.dismissTicketHint(user)
                    },
                ),
            )
        }
    }
}
