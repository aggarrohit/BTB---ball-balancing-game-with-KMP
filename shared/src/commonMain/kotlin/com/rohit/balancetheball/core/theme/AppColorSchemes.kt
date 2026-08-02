package com.rohit.balancetheball.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// A teal ("balance") + warm amber ("fun") palette, hand-tuned per role rather than relying on
// Material's default baseline purple — see AppTheme.kt for where these get selected. Every role
// is set explicitly (including the newer surfaceContainer*/inverse*/scrim roles) so components
// like OutlinedTextField, which source their container fill from those roles, never fall back to
// Material's baseline purple-tinted defaults and show up as a mismatched patch.

val BalanceLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF00897B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00332C),
    secondary = Color(0xFFFF7043),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFCCBC),
    onSecondaryContainer = Color(0xFF3D1400),
    tertiary = Color(0xFFFFB300),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFFFFE082),
    onTertiaryContainer = Color(0xFF2A1D00),
    background = Color(0xFFF6F8F8),
    onBackground = Color(0xFF1A1C1C),
    surface = Color(0xFFFBFDFC),
    onSurface = Color(0xFF1A1C1C),
    surfaceVariant = Color(0xFFDDE4E2),
    onSurfaceVariant = Color(0xFF3F4948),
    surfaceTint = Color(0xFF00897B),
    surfaceBright = Color(0xFFFBFDFC),
    surfaceDim = Color(0xFFD5DBDA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F4F3),
    surfaceContainer = Color(0xFFEBEEED),
    surfaceContainerHigh = Color(0xFFE5E9E7),
    surfaceContainerHighest = Color(0xFFDFE3E2),
    inverseSurface = Color(0xFF2E3132),
    inverseOnSurface = Color(0xFFF0F2F1),
    inversePrimary = Color(0xFF80CBC4),
    outline = Color(0xFF6F7977),
    outlineVariant = Color(0xFFC1C9C7),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color(0xFF000000)
)

val BalanceDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF4DB6AC),
    onPrimary = Color(0xFF00332C),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFFB2DFDB),
    secondary = Color(0xFFFFAB91),
    onSecondary = Color(0xFF5F1600),
    secondaryContainer = Color(0xFF7D2C00),
    onSecondaryContainer = Color(0xFFFFDBCB),
    tertiary = Color(0xFFFFD54F),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4200),
    onTertiaryContainer = Color(0xFFFFE082),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE6E1E1),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE6E1E1),
    surfaceVariant = Color(0xFF2A2F2E),
    onSurfaceVariant = Color(0xFFC4CAC8),
    surfaceTint = Color(0xFF4DB6AC),
    surfaceBright = Color(0xFF383838),
    surfaceDim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF292929),
    surfaceContainerHighest = Color(0xFF333333),
    inverseSurface = Color(0xFFE6E1E1),
    inverseOnSurface = Color(0xFF121212),
    inversePrimary = Color(0xFF00695C),
    outline = Color(0xFF899391),
    outlineVariant = Color(0xFF444948),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color(0xFF000000)
)
