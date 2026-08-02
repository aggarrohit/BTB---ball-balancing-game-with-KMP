package com.rohit.balancetheball.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Full-bleed decorative backdrop: a soft tinted gradient plus two large glowing color blobs. The
 * blobs are drawn as radial gradients fading to transparent (not `Modifier.blur`, which needs
 * API 31+ RenderEffect on Android and no-ops with a hard edge below that) so they render soft on
 * every device.
 */
@Composable
fun AppBackground(content: @Composable () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.primary.copy(alpha = 0.16f),
                        colorScheme.background,
                        colorScheme.tertiary.copy(alpha = 0.14f)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.TopEnd)
                .offset(x = 120.dp, y = (-120).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(colorScheme.primary.copy(alpha = 0.32f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-100).dp, y = 100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(colorScheme.tertiary.copy(alpha = 0.28f), Color.Transparent)
                    )
                )
        )
        content()
    }
}
