package com.rohit.balancetheball.core.theme

import platform.Foundation.NSUserDefaults

private const val KEY_THEME_MODE = "theme_mode"

actual object ThemePreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getThemeMode(): ThemeMode {
        val stored = defaults.stringForKey(KEY_THEME_MODE) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.SYSTEM)
    }

    actual fun setThemeMode(mode: ThemeMode) {
        defaults.setObject(mode.name, forKey = KEY_THEME_MODE)
    }
}
