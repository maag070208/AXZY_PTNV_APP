package com.axzydev.puertonuevoapp.core.session

/**
 * Almacenamiento simple de sesión (token JWT + refresh token + datos básicos
 * del usuario) persistido en disco: SharedPreferences en Android,
 * NSUserDefaults en iOS. Sin librerías externas — solo pares clave/valor de
 * texto.
 *
 * El refresh token es lo que permite renovar la sesión sin volver a pedir la
 * contraseña (`POST /auth/refresh`); el API lo rota en cada refresh.
 */
expect class TokenStore() {
    fun getToken(): String?
    fun saveToken(token: String)

    /** Refresh token vigente (rotado por el API); null si la sesión es anterior a este soporte. */
    fun getRefreshToken(): String?
    fun saveRefreshToken(refreshToken: String)

    fun getServerUrl(): String?
    fun saveServerUrl(url: String)

    fun getUserId(): String?
    fun getUsername(): String?
    fun getUserName(): String?
    fun getUserRole(): String?
    fun getUserDepartmentId(): String?
    fun saveUser(id: String, username: String, name: String, role: String, departmentId: String?)

    /** Roles efectivos de la sesión serializados como JSON (array de claves). */
    fun getRoles(): String?
    fun saveRoles(json: String)

    /**
     * `mustChangePassword` de la sesión persistido como texto ("true"/"false"):
     * la compuerta de contraseña provisional sobrevive al reinicio aunque el
     * arranque ocurra sin red.
     */
    fun getMustChangePassword(): Boolean
    fun saveMustChangePassword(value: Boolean)

    /** Permisos efectivos de la sesión serializados como JSON (clave → alcance). */
    fun getPermissions(): String?
    fun savePermissions(json: String)

    fun clear()
}
