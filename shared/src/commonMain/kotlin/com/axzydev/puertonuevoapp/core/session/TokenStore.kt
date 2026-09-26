package com.axzydev.puertonuevoapp.core.session

/**
 * Almacenamiento simple de sesión (token JWT + datos básicos del usuario)
 * persistido en disco: SharedPreferences en Android, NSUserDefaults en iOS.
 * Sin librerías externas — solo pares clave/valor de texto.
 */
expect class TokenStore() {
    fun getToken(): String?
    fun saveToken(token: String)

    fun getServerUrl(): String?
    fun saveServerUrl(url: String)

    fun getUserId(): String?
    fun getUsername(): String?
    fun getUserName(): String?
    fun getUserRole(): String?
    fun getUserDepartmentId(): String?
    fun saveUser(id: String, username: String, name: String, role: String, departmentId: String?)

    /** Permisos efectivos de la sesión serializados como JSON (clave → alcance). */
    fun getPermissions(): String?
    fun savePermissions(json: String)

    fun clear()
}
