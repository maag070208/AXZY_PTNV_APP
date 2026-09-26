package com.axzydev.puertonuevoapp.feature.custodyletters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.util.roleLabel
import com.axzydev.puertonuevoapp.core.ui.AppTextField

@Composable
fun CustodyLetterFormScreen(custodyLetterId: String? = null) {
    val navigator = LocalNavigator.current
    val viewModel: CustodyLetterFormViewModel = viewModel(key = "custody-letter-form-${custodyLetterId ?: "new"}") {
        CustodyLetterFormViewModel(custodyLetterId, AppContainer.custodyLettersApi, AppContainer.devicesApi, AppContainer.deviceTypesApi, AppContainer.usersApi)
    }
    val state by viewModel.uiState.collectAsState()
    val isEdit = custodyLetterId != null

    LaunchedEffect(state.saved) { if (state.saved) navigator.pop() }

    if (state.loading) {
        LoadingState(modifier = Modifier.fillMaxSize())
        return
    }

    val areaHeadOptions = state.filteredAreaHeads.map { u ->
        u.id to listOfNotNull(u.name, roleLabel(u.role), u.employeeNumber?.let { "#$it" }, u.department?.name).joinToString(" · ")
    }
    val employeeOptions = state.filteredEmployees.map { u ->
        u.id to listOfNotNull(u.name, u.employeeNumber?.let { "#$it" }, u.jobTitle, u.department?.name).joinToString(" · ")
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(16.dp),
    ) {
        state.error?.let {
            Text(it, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
        }

        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Recurso TIC")
            if (!state.changingDevice) {
                Text(state.currentItemSummary ?: "Sin dispositivo asignado", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = viewModel::startChangingDevice) { Text("Cambiar dispositivo") }
            } else {
                SimpleDropdownField(
                    label = "Tipo de dispositivo",
                    value = state.selectedTypeId ?: "",
                    options = state.typeOptions,
                    onSelect = viewModel::onTypeChange,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.previewFolio?.let {
                    Spacer(Modifier.height(6.dp))
                    Text("Folio: $it", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                }
                Spacer(Modifier.height(10.dp))
                SimpleDropdownField(
                    label = if (state.selectedTypeId == null) "Selecciona primero el tipo" else "Dispositivo disponible",
                    value = state.selectedDeviceId,
                    options = state.deviceOptions,
                    onSelect = viewModel::onDeviceChange,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isEdit) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = viewModel::keepCurrentDevice) { Text("Conservar dispositivo actual") }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Empleado (quien recibe)")
            AppSearchField(state.employeeQuery, viewModel::onEmployeeQueryChange, "Buscar por nombre, número, puesto…")
            Spacer(Modifier.height(8.dp))
            SimpleDropdownField(
                label = "Empleado",
                value = state.selectedEmployeeId,
                options = employeeOptions,
                onSelect = viewModel::onEmployeeChange,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = state.employeeNumber,
                onValueChange = viewModel::onEmployeeNumberChange,
                label = { Text("No. de empleado") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    value = state.company,
                    onValueChange = viewModel::onCompanyChange,
                    label = { Text("Empresa") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                AppTextField(
                    value = state.department,
                    onValueChange = viewModel::onDepartmentChange,
                    label = { Text("Departamento") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = state.area,
                onValueChange = viewModel::onAreaChange,
                label = { Text("Área (del recurso)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(14.dp))
        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Firmantes")
            AppSearchField(state.areaHeadQuery, viewModel::onAreaHeadQueryChange, "Buscar administrador, gerente o jefe…")
            Spacer(Modifier.height(8.dp))
            SimpleDropdownField(
                label = "Jefe de área (encargado)",
                value = state.selectedSupervisorId,
                options = areaHeadOptions,
                onSelect = viewModel::onSupervisorChange,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = state.deliveryBy,
                onValueChange = viewModel::onDeliveryByChange,
                label = { Text("Entrega (quien entrega)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = viewModel::submit,
            enabled = state.isValid && !state.saving,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (state.saving) CircularProgressIndicator(modifier = Modifier.height(20.dp), color = AppColors.Surface, strokeWidth = 2.dp)
            else Text(if (isEdit) "Guardar cambios" else "Crear carta", style = MaterialTheme.typography.titleMedium)
        }
    }
}
