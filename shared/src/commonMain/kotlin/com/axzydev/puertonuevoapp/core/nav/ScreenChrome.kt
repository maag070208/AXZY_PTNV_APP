package com.axzydev.puertonuevoapp.core.nav

data class ScreenChrome(
    val title: String,
    val showBack: Boolean = false,
    val showBottomNavigation: Boolean = true,
    val section: MainSection = MainSection.HOME,
)

enum class MainSection {
    HOME,
}

fun Screen.chrome(): ScreenChrome = when (this) {
    Screen.Home -> ScreenChrome("Inicio", section = MainSection.HOME)
}