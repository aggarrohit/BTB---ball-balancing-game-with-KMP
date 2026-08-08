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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rohit.balancetheball.core.sensor.RequestStepPermission
import com.rohit.balancetheball.core.sensor.StepCounter
import com.rohit.balancetheball.core.sensor.TiltSensor
import com.rohit.balancetheball.data.remote.FirebaseHistoryDataSource
import com.rohit.balancetheball.data.remote.FirebaseRoomDataSource
import com.rohit.balancetheball.data.repository.HistoryRepositoryImpl
import com.rohit.balancetheball.data.repository.RoomRepositoryImpl
import com.rohit.balancetheball.domain.model.RoomStatus
import com.rohit.balancetheball.domain.model.User
import com.rohit.balancetheball.presentation.common.AnimatedButton
import com.rohit.balancetheball.presentation.common.AnimatedOutlinedButton
import com.rohit.balancetheball.presentation.common.GlassCard
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

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
    viewModel: GameViewModel = viewModel {
        // Manual dependency wiring — swap in a DI framework when needed
        GameViewModel(
            roomCode = roomCode,
            uid = user.uid,
            tiltSensor = TiltSensor(),
            stepCounter = StepCounter(),
            roomRepository = RoomRepositoryImpl(FirebaseRoomDataSource()),
            historyRepository = HistoryRepositoryImpl(FirebaseHistoryDataSource())
        )
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val ballRadiusPx = with(density) { BALL_RADIUS.toPx() }
    var showExitConfirm by remember { mutableStateOf(false) }
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
                RollingBall(
                    rotation = Quaternion(
                        w = uiState.ballRotationW,
                        x = uiState.ballRotationX,
                        y = uiState.ballRotationY,
                        z = uiState.ballRotationZ
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        uiState.countdownSecondsRemaining?.let { secondsRemaining ->
            Text(
                text = "$secondsRemaining",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center)
            )
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
                        ?: "Total Steps: ${uiState.stepCount} (${uiState.stepCount - uiState.validSteps } invalid steps)",
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
            AnimatedOutlinedButton(onClick = { requestExitGame() }) { Text("Exit Game") }
        }
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

// The 12 vertices of a regular icosahedron, normalized to the unit sphere — used as pentagon
// centers so the drawn pattern reads as a soccer ball. Coordinates use the golden ratio, the
// standard construction (all cyclic permutations of (0, ±1, ±φ)).
private val ICOSAHEDRON_VERTICES: List<Triple<Float, Float, Float>> = run {
    val phi = 1.618034f
    val raw = listOf(
        Triple(0f, 1f, phi), Triple(0f, 1f, -phi), Triple(0f, -1f, phi), Triple(0f, -1f, -phi),
        Triple(1f, phi, 0f), Triple(1f, -phi, 0f), Triple(-1f, phi, 0f), Triple(-1f, -phi, 0f),
        Triple(phi, 0f, 1f), Triple(phi, 0f, -1f), Triple(-phi, 0f, 1f), Triple(-phi, 0f, -1f)
    )
    raw.map { (x, y, z) ->
        val length = sqrt(x * x + y * y + z * z)
        Triple(x / length, y / length, z / length)
    }
}

// Same palette as BallLogo.kt (Twemoji soccer ball, CC-BY 4.0) so the in-game ball and the
// sign-in/logo ball read as the same object.
private val BALL_BASE_COLOR = Color(0xFFF5F8FA)
private val BALL_SHADOW_COLOR = Color(0xFFCCD6DD)
private val BALL_PENTAGON_COLOR = Color(0xFF31373D)

/** How far each pentagon's corners sit from its center, in unit-sphere (tangent-plane) units. */
private const val PENTAGON_CORNER_OFFSET = 0.42f

private typealias Vec3 = Triple<Float, Float, Float>

private data class SpherePentagon(val center: Vec3, val corners: List<Vec3>)

private fun Vec3.normalized(): Vec3 {
    val (x, y, z) = this
    val length = sqrt(x * x + y * y + z * z)
    return Triple(x / length, y / length, z / length)
}

private fun cross(a: Vec3, b: Vec3): Vec3 = Triple(
    a.second * b.third - a.third * b.second,
    a.third * b.first - a.first * b.third,
    a.first * b.second - a.second * b.first
)

// A pentagon "sticker" tangent to the sphere at each icosahedron vertex: its 5 corners are laid
// out in the local tangent plane, then re-normalized onto the sphere. Rotating and projecting
// each corner individually (not just the center, scaled) is what gives real foreshortening —
// pentagons compress into slivers near the rim like an actual projected polygon would, instead of
// just shrinking uniformly in place.
private val SPHERE_PENTAGONS: List<SpherePentagon> = ICOSAHEDRON_VERTICES.map { v ->
    val arbitrary: Vec3 = if (kotlin.math.abs(v.second) < 0.9f) Triple(0f, 1f, 0f) else Triple(1f, 0f, 0f)
    val tangentU = cross(arbitrary, v).normalized()
    val tangentW = cross(v, tangentU)
    val corners = (0 until 5).map { i ->
        val angle = -kotlin.math.PI.toFloat() / 2f + i * (2f * kotlin.math.PI.toFloat() / 5f)
        val cosA = cos(angle) * PENTAGON_CORNER_OFFSET
        val sinA = sin(angle) * PENTAGON_CORNER_OFFSET
        Triple(
            v.first + cosA * tangentU.first + sinA * tangentW.first,
            v.second + cosA * tangentU.second + sinA * tangentW.second,
            v.third + cosA * tangentU.third + sinA * tangentW.third
        ).normalized()
    }
    SpherePentagon(center = v, corners = corners)
}

/**
 * A genuinely rotating sphere, not a warped flat image: every pentagon corner is a real 3D
 * coordinate on a unit sphere, rotated by [rotation] (composed frame-to-frame in GameViewModel
 * from real rolling physics — see Quaternion.kt) and projected to 2D every frame, corner by
 * corner — not just the pentagon's center, scaled — so pentagons genuinely foreshorten (compress
 * toward a sliver) near the rim instead of just shrinking in place. The outer circle itself is
 * never distorted, so the ball never squishes or flips like a flat sprite would.
 */
@Composable
private fun RollingBall(rotation: Quaternion, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, BALL_BASE_COLOR, BALL_SHADOW_COLOR),
                center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
                radius = radius * 1.7f
            ),
            radius = radius,
            center = center
        )

        SPHERE_PENTAGONS.forEach { pentagon ->
            val (_, _, centerZ) = rotation.rotate(pentagon.center.first, pentagon.center.second, pentagon.center.third)
            if (centerZ > 0f) {
                val path = Path()
                pentagon.corners.forEachIndexed { i, corner ->
                    val (x1, y1, _) = rotation.rotate(corner.first, corner.second, corner.third)
                    val screenX = center.x + x1 * radius
                    val screenY = center.y - y1 * radius
                    if (i == 0) path.moveTo(screenX, screenY) else path.lineTo(screenX, screenY)
                }
                path.close()
                drawPath(path = path, color = BALL_PENTAGON_COLOR)
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
