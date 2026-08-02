package com.rohit.balancetheball.presentation.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
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
import com.rohit.balancetheball.presentation.common.AnimatedButton
import com.rohit.balancetheball.presentation.common.AnimatedOutlinedButton
import com.rohit.balancetheball.presentation.common.GlassCard
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val BALL_RADIUS = 24.dp
private val CENTER_MARKER_RADIUS = 5.dp
private val CENTER_MARKER_ARM_LENGTH = 14.dp
private const val TABLE_WIDTH_FRACTION = 0.8f
private const val TABLE_HEIGHT_FRACTION = 0.54f // 10% smaller than the original 0.6f
private const val FALL_ANIMATION_MS = 650

@OptIn(ExperimentalComposeUiApi::class)
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
    val snackbarHostState = remember { SnackbarHostState() }

    RequestStepPermission { granted -> viewModel.onStepPermissionResult(granted) }

    fun requestExitGame() {
        viewModel.onPauseSensors()
        showExitConfirm = true
    }

    // System back button (Android) exits the *game* (with the same confirm-and-pause behavior as
    // the Exit Game button) rather than falling through to the default "finish the Activity"
    // behavior, which would silently close the whole app. No-ops on iOS (no back-gesture source
    // is registered outside a real navigation stack, which this app doesn't have).
    BackHandler(enabled = true) { requestExitGame() }

    // Sensors/physics pause while the app is backgrounded and resume when it's foregrounded again
    // — ON_START/ON_STOP (not ON_RESUME/ON_PAUSE) are the correct pair for real backgrounding on
    // both Android and iOS. The room observer inside GameViewModel keeps running regardless, so
    // multiplayer state stays fresh even while paused.
    LifecycleStartEffect(Unit) {
        viewModel.onResumeSensors()
        onStopOrDispose { viewModel.onPauseSensors() }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GameEvent.PlayerDeclinedPlayAgain ->
                    snackbarHostState.showSnackbar("${event.username} denied and left the room")
                GameEvent.LeftRoomAfterDecline -> onExitGame()
            }
        }
    }

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
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp), clip = false)
                .background(Color(0xFF2E7D32), RoundedCornerShape(24.dp))
                .border(2.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
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
            // Once eliminated, the ball visibly shrinks/fades/drops once, then stays gone — driven
            // purely by isEliminated, which GameViewModel also freezes the ball's position on.
            val fallAlpha by animateFloatAsState(
                targetValue = if (uiState.isEliminated) 0f else 1f,
                animationSpec = tween(FALL_ANIMATION_MS),
                label = "ballFallAlpha"
            )
            val fallScale by animateFloatAsState(
                targetValue = if (uiState.isEliminated) 0.3f else 1f,
                animationSpec = tween(FALL_ANIMATION_MS),
                label = "ballFallScale"
            )
            val fallDropPx by animateFloatAsState(
                targetValue = if (uiState.isEliminated) 80f else 0f,
                animationSpec = tween(FALL_ANIMATION_MS),
                label = "ballFallDrop"
            )

            Box(
                modifier = Modifier
                    .size(BALL_RADIUS * 2)
                    .offset {
                        IntOffset(
                            (uiState.ballX - ballRadiusPx).roundToInt(),
                            (uiState.ballY - ballRadiusPx).roundToInt()
                        )
                    }
                    .graphicsLayer {
                        alpha = fallAlpha
                        scaleX = fallScale
                        scaleY = fallScale
                        translationY = fallDropPx
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⚽", fontSize = (BALL_RADIUS.value * 1.8f).sp)
            }
        }

        GlassCard(
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            contentPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = uiState.stepsUnavailableReason?.let { "Steps: unavailable ($it)" }
                        ?: "Steps: ${uiState.stepCount}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Distance: ${(uiState.distanceFraction * 100).roundToInt()}% " +
                        "(threshold ${uiState.progressValidDistancePercent}%)" +
                        if (uiState.isEliminated) " — off the table" else "",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Progress: ${uiState.validSteps} / ${uiState.targetSteps}",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (uiState.players.isNotEmpty()) {
                    PlayerGrid(
                        players = uiState.players,
                        targetSteps = uiState.targetSteps,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedOutlinedButton(onClick = { showLogoutConfirm = true }) { Text("Logout") }
            AnimatedOutlinedButton(onClick = { requestExitGame() }) { Text("Exit Game") }
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
            onDismissRequest = {
                showExitConfirm = false
                viewModel.onResumeSensors()
            },
            title = { Text("Exit game?") },
            text = { Text("Sure you want to exit? You'll leave the room and head back to the Lobby.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    onExitGame()
                }) { Text("Exit") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    viewModel.onResumeSensors()
                }) { Text("Cancel") }
            }
        )
    }

    if (uiState.roomStatus == RoomStatus.FINISHED) {
        val request = uiState.playAgainRequest
        when {
            request == null -> {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("Game over") },
                    text = {
                        Text(
                            uiState.winnerUsername?.let { "$it wins!" }
                                ?: if (uiState.players.size <= 1) {
                                    "You fell off the table — try again!"
                                } else {
                                    "Everyone fell off the table — no winner this round."
                                }
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.onProposePlayAgain() }) { Text("Play Again") }
                    },
                    dismissButton = {
                        TextButton(onClick = onExitGame) { Text("Back to Lobby") }
                    }
                )
            }
            request.hasAccepted -> {
                val stillWaitingOn = (request.totalNeeded - request.acceptedCount).coerceAtLeast(0)
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("Waiting for players…") },
                    text = { Text("Waiting on $stillWaitingOn more player(s) to accept.") },
                    confirmButton = {
                        TextButton(onClick = onExitGame) { Text("Back to Lobby") }
                    }
                )
            }
            else -> {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("Play again?") },
                    text = { Text("${request.requestedByUsername} wants to play again.") },
                    confirmButton = {
                        TextButton(onClick = { viewModel.onAcceptPlayAgain() }) { Text("Play") }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.onDeclinePlayAgain() }) { Text("Deny") }
                    }
                )
            }
        }
    }
}

@Composable
private fun PlayerGrid(players: List<PlayerProgress>, targetSteps: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        players.chunked(2).forEach { rowPlayers ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowPlayers.forEach { player ->
                    PlayerCard(player, targetSteps, modifier = Modifier.weight(1f))
                }
                if (rowPlayers.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PlayerCard(player: PlayerProgress, targetSteps: Int, modifier: Modifier = Modifier) {
    val progressPct = if (targetSteps > 0) {
        (player.validSteps * 100 / targetSteps).coerceAtMost(100)
    } else 0
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = player.username,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (player.isSelf) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$progressPct%" + if (player.isEliminated) " ✗" else "",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
