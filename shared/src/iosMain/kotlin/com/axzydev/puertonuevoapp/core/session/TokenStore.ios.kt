package com.axzydev.puertonuevoapp.core.session

import platform.Foundation.NSUserDefaults

actual class TokenStore actual constructor() {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getToken(): String? = defaults.stringForKey(KEY_TOKEN)
    actual fun saveToken(token: String) {
        defaults.setObject(token, KEY_TOKEN)
    }

    actual fun getServerUrl(): String? = defaults.stringForKey(KEY_SERVER_URL)
    actual fun saveServerUrl(url: String) {
        defaults.setObject(url, KEY_SERVER_URL)
    }

    actual fun getUserId(): String? = defaults.stringForKey(KEY_USER_ID)
    actual fun getUsername(): String? = defaults.stringForKey(KEY_USERNAME)
    actual fun getUserName(): String? = defaults.stringForKey(KEY_USER_NAME)
    actual fun getUserRole(): String? = defaults.stringForKey(KEY_USER_ROLE)
    actual fun getUserDepartmentId(): String? = defaults.stringForKey(KEY_USER_DEPT)

    actual fun saveUser(id: String, username: String, name: String, role: String, departmentId: String?) {
        defaults.setObject(id, KEY_USER_ID)
        defaults.setObject(username, KEY_USERNAME)
        defaults.setObject(name, KEY_USER_NAME)
        defaults.setObject(role, KEY_USER_ROLE)
        if (departmentId != null) {
            defaults.setObject(departmentId, KEY_USER_DEPT)
        } else {
            defaults.removeObjectForKey(KEY_USER_DEPT)
        }
    }

    actual fun getPermissions(): String? = defaults.stringForKey(KEY_PERMISSIONS)
    actual fun savePermissions(json: String) {
        defaults.setObject(json, KEY_PERMISSIONS)
    }

    actual fun clear() {
        defaults.removeObjectForKey(KEY_TOKEN)
        defaults.removeObjectForKey(KEY_USER_ID)
        defaults.removeObjectForKey(KEY_USERNAME)
        defaults.removeObjectForKey(KEY_USER_NAME)
        defaults.removeObjectForKey(KEY_USER_ROLE)
        defaults.removeObjectForKey(KEY_USER_DEPT)
        defaults.removeObjectForKey(KEY_PERMISSIONS)
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_ROLE = "user_role"
        const val KEY_USER_DEPT = "user_dept"
        const val KEY_PERMISSIONS = "permissions"
    }
}
