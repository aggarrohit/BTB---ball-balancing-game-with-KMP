package com.rohit.balancetheball.core.theme

import android.content.Context
import com.rohit.balancetheball.core.sensor.AndroidSensorContext

private const val PREFS_NAME = "theme_prefs"
private const val KEY_THEME_MODE = "theme_mode"

actual object ThemePreferences {
    private val prefs by lazy {
        AndroidSensorContext.appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun getThemeMode(): ThemeMode {
        val stored = prefs.getString(KEY_THEME_MODE, null) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.SYSTEM)
    }

    actual fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }
}
