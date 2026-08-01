package com.rohit.balancetheball.presentation.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rohit.balancetheball.core.sensor.RequestStepPermission
import com.rohit.balancetheball.core.sensor.StepCounter
import com.rohit.balancetheball.core.sensor.TiltSensor
import com.rohit.balancetheball.data.auth.FirebaseAuthRepository
import com.rohit.balancetheball.data.remote.FirebaseRoomDataSource
import com.rohit.balancetheball.data.repository.RoomRepositoryImpl
import com.rohit.balancetheball.domain.model.RoomStatus
import com.rohit.balancetheball.domain.model.User
import com.rohit.balancetheball.domain.repository.AuthRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val BALL_RADIUS = 24.dp
private val CENTER_MARKER_RADIUS = 5.dp
private val CENTER_MARKER_ARM_LENGTH = 14.dp
private val TABLE_WIDTH_FRACTION = 0.8f
private val TABLE_HEIGHT_FRACTION = 0.6f

/** kotlin.math has no cross-platform sprintf, so format one decimal place by hand. */
private fun Float.formatOneDecimal(): String {
    val tenths = kotlin.math.round(this * 10).toInt()
    val sign = if (tenths < 0) "-" else ""
    val absTenths = kotlin.math.abs(tenths)
    return "$sign${absTenths / 10}.${absTenths % 10}"
}

@Composable
fun GameScreen(
    user: User,
    roomCode: String,
    onExitGame: () -> Unit,
    onLoggedOut: () -> Unit,
    authRepository: AuthRepository = remember { FirebaseAuthRepository() },
    viewModel: GameViewModel = viewModel(key = roomCode) {
        // Manual dependency wiring — swap in a DI framework when needed
        GameViewModel(
            roomCode = roomCode,
            uid = user.uid,
            tiltSensor = TiltSensor(),
            stepCounter = StepCounter(),
            roomRepository = RoomRepositoryImpl(FirebaseRoomDataSource())
        )
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val ballRadiusPx = with(density) { BALL_RADIUS.toPx() }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    RequestStepPermission { granted -> viewModel.onStepPermissionResult(granted) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                viewModel.onBallRadiusPx(ballRadiusPx)
                viewModel.onCanvasSizeChanged(size.width.toFloat(), size.height.toFloat())
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(TABLE_WIDTH_FRACTION)
                .fillMaxHeight(TABLE_HEIGHT_FRACTION)
                .align(Alignment.Center)
                .background(Color(0xFF2E7D32), RoundedCornerShape(24.dp))
                .onSizeChanged { size ->
                    viewModel.onTableSizeChanged(size.width.toFloat(), size.height.toFloat())
                }
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Marks the exact center of the table as a reference point for balancing.
            val center = Offset(size.width / 2f, size.height / 2f)
            val armPx = CENTER_MARKER_ARM_LENGTH.toPx()
            val markerColor = Color.White.copy(alpha = 0.6f)
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
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = uiState.stepsUnavailableReason?.let { "Steps: unavailable ($it)" }
                    ?: "Steps: ${uiState.stepCount}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Distance: ${(uiState.distanceFraction * 100).roundToInt()}%" +
                    if (uiState.isEliminated) " — off the table" else "",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Progress: ${uiState.validSteps} / ${uiState.targetSteps}",
                style = MaterialTheme.typography.bodyMedium
            )

            if (uiState.players.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    uiState.players.forEach { player ->
                        val progressPct = if (uiState.targetSteps > 0) {
                            (player.validSteps * 100 / uiState.targetSteps).coerceAtMost(100)
                        } else 0
                        Text(
                            text = "${player.username}: $progressPct%" + if (player.isEliminated) " (out)" else "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (player.isSelf) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = { showLogoutConfirm = true }) { Text("Logout") }
            OutlinedButton(onClick = { showExitConfirm = true }) { Text("Exit Game") }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log out?") },
            text = { Text("You'll need to sign in again to play.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    coroutineScope.launch {
                        authRepository.signOut()
                        onLoggedOut()
                    }
                }) { Text("Log out") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Exit game?") },
            text = { Text("Sure you want to exit? You'll leave the room and head back to the Lobby.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    onExitGame()
                }) { Text("Exit") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (uiState.roomStatus == RoomStatus.FINISHED) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Game over") },
            text = {
                Text(
                    uiState.winnerUsername?.let { "$it wins!" }
                        ?: "Everyone fell off the table — no winner this round."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onPlayAgain() }) { Text("Play Again") }
            },
            dismissButton = {
                TextButton(onClick = onExitGame) { Text("Back to Lobby") }
            }
        )
    }
}
