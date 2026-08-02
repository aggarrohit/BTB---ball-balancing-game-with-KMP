package com.rohit.balancetheball.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohit.balancetheball.core.theme.ThemeMode

private data class ThemeOption(val mode: ThemeMode, val glyph: String, val label: String)

private val THEME_OPTIONS = listOf(
    ThemeOption(ThemeMode.LIGHT, "☀️", "Light"),
    ThemeOption(ThemeMode.DARK, "🌙", "Dark"),
    ThemeOption(ThemeMode.SYSTEM, "🌗", "System")
)

/**
 * Root-level 3-dot menu for picking the theme. Kept as a single dropdown with a labeled section
 * so more items (unrelated to theming) can be appended below the divider later without needing a
 * new menu trigger.
 */
@Composable
fun ThemeMenu(currentMode: ThemeMode, onModeSelected: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Text(text = "⋮", fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider()
            THEME_OPTIONS.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${option.glyph}  ${option.label}",
                            fontWeight = if (option.mode == currentMode) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    trailingIcon = if (option.mode == currentMode) {
                        { Text("✓") }
                    } else null,
                    onClick = {
                        expanded = false
                        onModeSelected(option.mode)
                    }
                )
            }
        }
    }
}
