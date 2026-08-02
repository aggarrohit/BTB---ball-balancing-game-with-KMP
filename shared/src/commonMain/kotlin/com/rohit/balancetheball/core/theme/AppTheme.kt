package com.rohit.balancetheball.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AppTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDarkTheme) BalanceDarkColorScheme else BalanceLightColorScheme

    MaterialTheme(colorScheme = colorScheme) {
        // MaterialTheme alone doesn't set a default text color — only Surface does, by deriving
        // it from its own background. Without this wrap, plain Text() falls back to Compose's
        // hardcoded black default regardless of theme.
        Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.background) {
            content()
        }
    }
}
