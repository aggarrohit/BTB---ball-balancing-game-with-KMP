package com.rohit.balancetheball.core.theme

/**
 * Persists the user's manual theme override across app restarts. Backed by SharedPreferences on
 * Android and NSUserDefaults on iOS — both are cheap, synchronous, local key-value stores, so no
 * async loading state is needed at the call site.
 */
expect object ThemePreferences {
    fun getThemeMode(): ThemeMode
    fun setThemeMode(mode: ThemeMode)
}
