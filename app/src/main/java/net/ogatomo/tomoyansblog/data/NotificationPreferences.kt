package net.ogatomo.tomoyansblog.data

import android.content.Context

class NotificationPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isNotificationsEnabled(): Boolean = preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

    fun setNotificationsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun isAlwaysOpenInExternalBrowser(): Boolean =
        preferences.getBoolean(KEY_ALWAYS_OPEN_IN_EXTERNAL_BROWSER, false)

    fun setAlwaysOpenInExternalBrowser(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ALWAYS_OPEN_IN_EXTERNAL_BROWSER, enabled).apply()
    }

    fun getThemeMode(): ThemeMode {
        return ThemeMode.entries.firstOrNull {
            it.name == preferences.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        } ?: ThemeMode.SYSTEM
    }

    fun setThemeMode(themeMode: ThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, themeMode.name).apply()
    }

    fun getLastSeenGuid(): String? = preferences.getString(KEY_LAST_SEEN_GUID, null)

    fun setLastSeenGuid(guid: String) {
        preferences.edit().putString(KEY_LAST_SEEN_GUID, guid).apply()
    }

    fun hasRequestedNotificationPermission(): Boolean =
        preferences.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)

    fun setHasRequestedNotificationPermission(requested: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, requested).apply()
    }

    fun getOfflineSyncMode(): OfflineSyncMode {
        return OfflineSyncMode.entries.firstOrNull {
            it.name == preferences.getString(KEY_OFFLINE_SYNC_MODE, OfflineSyncMode.WIFI_ONLY.name)
        } ?: OfflineSyncMode.WIFI_ONLY
    }

    fun setOfflineSyncMode(mode: OfflineSyncMode) {
        preferences.edit().putString(KEY_OFFLINE_SYNC_MODE, mode.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "blog_preferences"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_ALWAYS_OPEN_IN_EXTERNAL_BROWSER = "always_open_in_external_browser"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LAST_SEEN_GUID = "last_seen_guid"
        private const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
        private const val KEY_OFFLINE_SYNC_MODE = "offline_sync_mode"
    }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class OfflineSyncMode {
    ALWAYS,
    WIFI_ONLY,
    OFF
}
