package com.axzydev.puertonuevoapp.core.session

import android.content.Context

/**
 * Puente para que las clases `actual` en Android (que necesitan un [Context])
 * puedan construirse sin argumentos, igual que en iOS. Se inicializa una vez
 * desde MainActivity antes de usar cualquier cosa que dependa de él.
 */
object AndroidAppContext {
    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
        }
    }
}
