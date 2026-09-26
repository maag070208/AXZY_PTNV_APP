package com.axzydev.puertonuevoapp.feature.credential

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.RemoteImage
import com.axzydev.puertonuevoapp.core.util.roleLabel
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

/** Credencial digital: el empleado la muestra y el guardia la escanea en Portería. */
@Composable
fun MyCredentialScreen() {
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user ?: return
    val viewModel: MyCredentialViewModel = viewModel(key = "credential-${user.id}") {
        MyCredentialViewModel(user, AppContainer.authApi)
    }
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppCard(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            contentPadding = PaddingValues(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RemoteImage(
                    path = state.photoUrl,
                    loadBytes = { AppContainer.apiClient.getBytes(it) },
                    modifier = Modifier.size(72.dp).clip(CircleShape),
                    contentDescription = state.name,
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.name, style = MaterialTheme.typography.titleLarge, color = AppColors.TextPrimary)
                    Text(
                        listOfNotNull(state.jobTitle, state.department).joinToString(" · ").ifBlank { roleLabel(state.role) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextMuted,
                    )
                    state.employeeNumber?.let {
                        Text("No. de empleado: $it", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            // Fondo blanco con margen: el lector necesita contraste y zona libre alrededor.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(AppShape.card)
                    .background(AppColors.Surface)
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = rememberQrCodePainter(state.qrData),
                    contentDescription = "Código QR de tu credencial",
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Muéstrale este código al guardia al entrar y al salir.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                "No se pudieron cargar tus datos ($it). El código sigue siendo válido.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
                textAlign = TextAlign.Center,
            )
        }
    }
}
