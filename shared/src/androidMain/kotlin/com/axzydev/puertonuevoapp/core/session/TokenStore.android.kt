package com.axzydev.puertonuevoapp.core.session

import android.content.Context
import android.content.SharedPreferences

actual class TokenStore actual constructor() {
    private val prefs: SharedPreferences by lazy {
        AndroidAppContext.appContext.getSharedPreferences("ptnv_session", Context.MODE_PRIVATE)
    }

    actual fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    actual fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    actual fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    actual fun saveRefreshToken(refreshToken: String) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, refreshToken).apply()
    }

    actual fun getServerUrl(): String? = prefs.getString(KEY_SERVER_URL, null)
    actual fun saveServerUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
    }

    actual fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    actual fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    actual fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    actual fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, null)
    actual fun getUserDepartmentId(): String? = prefs.getString(KEY_USER_DEPT, null)

    actual fun saveUser(id: String, username: String, name: String, role: String, departmentId: String?) {
        prefs.edit()
            .putString(KEY_USER_ID, id)
            .putString(KEY_USERNAME, username)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_ROLE, role)
            .putString(KEY_USER_DEPT, departmentId)
            .apply()
    }

    actual fun getRoles(): String? = prefs.getString(KEY_ROLES, null)
    actual fun saveRoles(json: String) {
        prefs.edit().putString(KEY_ROLES, json).apply()
    }

    actual fun getMustChangePassword(): Boolean = prefs.getBoolean(KEY_MUST_CHANGE_PASSWORD, false)
    actual fun saveMustChangePassword(value: Boolean) {
        prefs.edit().putBoolean(KEY_MUST_CHANGE_PASSWORD, value).apply()
    }

    actual fun getPermissions(): String? = prefs.getString(KEY_PERMISSIONS, null)
    actual fun savePermissions(json: String) {
        prefs.edit().putString(KEY_PERMISSIONS, json).apply()
    }

    actual fun clear() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_ROLE)
            .remove(KEY_USER_DEPT)
            .remove(KEY_ROLES)
            .remove(KEY_MUST_CHANGE_PASSWORD)
            .remove(KEY_PERMISSIONS)
            .apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_ROLE = "user_role"
        const val KEY_USER_DEPT = "user_dept"
        const val KEY_ROLES = "roles"
        const val KEY_MUST_CHANGE_PASSWORD = "must_change_password"
        const val KEY_PERMISSIONS = "permissions"
    }
}
