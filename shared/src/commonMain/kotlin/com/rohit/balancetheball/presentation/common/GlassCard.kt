package com.rohit.balancetheball.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A translucent, bordered, elevated surface that reads as a floating "glass" panel. Compose
 * Multiplatform has no cross-platform backdrop blur, so this is a stylized approximation — a
 * tinted gradient fill (not a flat color, so it doesn't read as a plain gray box) plus a soft
 * border and shadow — paired with [AppBackground]'s glow blobs, it still reads as glassy. Capped
 * width keeps it a floating card rather than stretching edge-to-edge on a phone screen.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    contentPadding: Dp = 28.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .widthIn(max = 420.dp)
            .shadow(elevation = 20.dp, shape = shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.surface.copy(alpha = 0.95f),
                        colorScheme.surfaceVariant.copy(alpha = 0.88f)
                    )
                )
            )
            .border(1.dp, colorScheme.onSurface.copy(alpha = 0.14f), shape)
            .padding(contentPadding),
        content = content
    )
}
