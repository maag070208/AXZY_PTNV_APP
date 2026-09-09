package com.axzydev.puertonuevoapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform