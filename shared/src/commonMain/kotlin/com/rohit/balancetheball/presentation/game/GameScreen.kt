package com.rohit.balancetheball.presentation.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rohit.balancetheball.core.sensor.RequestStepPermission
import com.rohit.balancetheball.core.sensor.StepCounter
import com.rohit.balancetheball.core.sensor.TiltSensor
import kotlin.math.roundToInt

private val BALL_RADIUS = 24.dp
private val CENTER_MARKER_RADIUS = 5.dp
private val CENTER_MARKER_ARM_LENGTH = 14.dp

/** kotlin.math has no cross-platform sprintf, so format one decimal place by hand. */
private fun Float.formatOneDecimal(): String {
    val tenths = kotlin.math.round(this * 10).toInt()
    val sign = if (tenths < 0) "-" else ""
    val absTenths = kotlin.math.abs(tenths)
    return "$sign${absTenths / 10}.${absTenths % 10}"
}

@Composable
fun GameScreen(
    username: String,
    viewModel: GameViewModel = viewModel {
        // Manual dependency wiring — swap in a DI framework when needed
        GameViewModel(TiltSensor(), StepCounter())
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val ballRadiusPx = with(density) { BALL_RADIUS.toPx() }

    RequestStepPermission { granted -> viewModel.onStepPermissionResult(granted) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                viewModel.onBallRadiusPx(ballRadiusPx)
                viewModel.onCanvasSizeChanged(size.width.toFloat(), size.height.toFloat())
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Marks the exact center of the screen as a reference point for balancing.
            val center = Offset(size.width / 2f, size.height / 2f)
            val armPx = CENTER_MARKER_ARM_LENGTH.toPx()
            val markerColor = Color.Gray.copy(alpha = 0.5f)
            drawLine(markerColor, Offset(center.x - armPx, center.y), Offset(center.x + armPx, center.y))
            drawLine(markerColor, Offset(center.x, center.y - armPx), Offset(center.x, center.y + armPx))
            drawCircle(
                color = markerColor,
                radius = CENTER_MARKER_RADIUS.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        if (uiState.ballReady) {
            Box(
                modifier = Modifier
                    .size(BALL_RADIUS * 2)
                    .offset {
                        IntOffset(
                            (uiState.ballX - ballRadiusPx).roundToInt(),
                            (uiState.ballY - ballRadiusPx).roundToInt()
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⚽", fontSize = (BALL_RADIUS.value * 1.8f).sp)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = uiState.stepsUnavailableReason?.let { "Steps: unavailable ($it)" }
                    ?: "Steps: ${uiState.stepCount}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Pitch: ${uiState.pitchDegrees.formatOneDecimal()}°  Roll: ${uiState.rollDegrees.formatOneDecimal()}°",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
