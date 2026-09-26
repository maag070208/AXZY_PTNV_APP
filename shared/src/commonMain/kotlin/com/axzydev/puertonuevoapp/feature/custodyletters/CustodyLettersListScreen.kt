package com.axzydev.puertonuevoapp.feature.custodyletters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.custodyletters.CustodyLetterDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.util.formatShortDate

@Composable
fun CustodyLettersListScreen(viewModel: CustodyLettersListViewModel = viewModel { CustodyLettersListViewModel(AppContainer.custodyLettersApi) }) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canCreate = user?.canCreateCustodyLetters == true
    val canDelete = user?.canDeleteCustodyLetters == true
    val canGenerate = user?.canGenerateCustodyLetters == true

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${state.filtered.size} carta(s)", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                Row {
                    if (canGenerate) {
                        IconButton(onClick = { navigator.push(Screen.GenerateCustodyLetter) }) {
                            Icon(Icons.Filled.Bolt, contentDescription = "Generar carta por tipo", tint = AppColors.EmeraldPrimary)
                        }
                    }
                    if (canCreate) {
                        IconButton(onClick = { navigator.push(Screen.CustodyLetterForm()) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Nueva carta", tint = AppColors.EmeraldPrimary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            AppSearchField(state.query, viewModel::onQueryChange, "Buscar por folio, empleado, descripción…")
        }

        when {
            state.loading -> LoadingState(modifier = Modifier.weight(1f))
            state.error != null -> ErrorState(state.error ?: "Error", Modifier.weight(1f), onRetry = viewModel::load)
            state.filtered.isEmpty() -> EmptyState("No hay cartas responsivas registradas", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.filtered, key = { it.id }) { c ->
                    CustodyLetterCard(
                        custodyLetter = c,
                        canDelete = canDelete,
                        onClick = { navigator.push(Screen.CustodyLetterDetail(c.id)) },
                        onDelete = { viewModel.requestDelete(c) },
                    )
                }
            }
        }
    }

    state.deleteTarget?.let { c ->
        AppModal(
            title = "Eliminar carta",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissDelete,
            confirmLabel = "Eliminar",
            saving = state.actionSaving,
            onConfirm = viewModel::confirmDelete,
        ) {
            Text("¿Eliminar la carta ${c.consecutive} del historial? Esta acción no se puede deshacer.", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
        }
    }

    state.actionError?.let { message ->
        AppModal(
            title = "No se pudo completar la acción",
            icon = Icons.Filled.ErrorOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissActionError,
        ) { Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted) }
    }
}

@Composable
private fun CustodyLetterCard(custodyLetter: CustodyLetterDto, canDelete: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    val item = custodyLetter.items.firstOrNull()
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = AppColors.Outline,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(custodyLetter.consecutive, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary, maxLines = 1)
                Text(formatShortDate(custodyLetter.date), style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
            }
            Text("No. empleado ${custodyLetter.employeeNumber.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted, maxLines = 1)
            Text(item?.description ?: "Sin recurso asignado", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint, maxLines = 1)
            Text(custodyLetter.department, style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint, maxLines = 1)
            if (custodyLetter.returnDate != null) {
                Spacer(Modifier.height(4.dp))
                Text("Devuelta", style = MaterialTheme.typography.labelSmall, color = AppColors.Success)
            }
        }
        if (canDelete) {
            IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = AppColors.Danger, modifier = Modifier.size(18.dp)) }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AppColors.TextFaint, modifier = Modifier.size(20.dp))
        }
    }
}
