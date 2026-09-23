package com.axzydev.puertonuevoapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Navigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.PuertoNuevoTheme
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.feature.auth.LoginScreen
import com.axzydev.puertonuevoapp.feature.home.AppShell

@Composable
@Preview
fun App() {
    PuertoNuevoTheme {
        val authRepository = AppContainer.authRepository
        val authState by authRepository.state.collectAsState()

        LaunchedEffect(Unit) {
            authRepository.restoreSession()
        }

        Surface(modifier = Modifier.fillMaxSize(), color = AppColors.Background) {
            when (val state = authState) {
                AuthState.Loading -> LoadingState(modifier = Modifier.fillMaxSize())
                AuthState.LoggedOut -> LoginScreen()
                is AuthState.LoggedIn -> {
                    // El guardia aterriza directo en la pantalla de escaneo.
                    val start = if (state.user.role == "GUARD") Screen.AccessScan else Screen.Home
                    val navigator = remember(state.user.id) { Navigator(start) }
                    CompositionLocalProvider(LocalNavigator provides navigator) {
                        AppShell()
                    }
                }
            }
        }
    }
}
