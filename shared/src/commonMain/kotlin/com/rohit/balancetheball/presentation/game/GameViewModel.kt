package com.rohit.balancetheball.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohit.balancetheball.core.sensor.StepCounter
import com.rohit.balancetheball.core.sensor.TiltReading
import com.rohit.balancetheball.core.sensor.TiltSensor
import com.rohit.balancetheball.core.util.currentTimeMillis
import com.rohit.balancetheball.domain.model.Room
import com.rohit.balancetheball.domain.model.RoomStatus
import com.rohit.balancetheball.domain.repository.RoomRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

private const val PHYSICS_TICK_MS = 16L
private const val MAX_DT_SECONDS = 0.1f // clamp huge gaps (e.g. after backgrounding) so the ball doesn't jump
private const val SENSITIVITY_PX_PER_S2_PER_DEGREE = 30f
private const val DAMPING_RETAINED_PER_SECOND = 0.35f

class GameViewModel(
    private val roomCode: String,
    private val uid: String,
    private val tiltSensor: TiltSensor,
    private val stepCounter: StepCounter,
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var latestTilt = TiltReading(pitchDegrees = 0f, rollDegrees = 0f)
    private var velocityX = 0f
    private var velocityY = 0f
    private var canvasWidthPx: Float? = null
    private var canvasHeightPx: Float? = null
    private var tableWidthPx: Float? = null
    private var tableHeightPx: Float? = null
    private var ballRadiusPx: Float = 0f
    private val stepPermissionGranted = MutableStateFlow<Boolean?>(null)

    private var lastRawStepCount = 0
    private var localValidSteps = 0
    private var localIsEliminated = false
    private var hasSeededFromRoom = false
    private var hasClaimedVictory = false
    private var progressValidDistancePercent = Room.DEFAULT_PROGRESS_VALID_DISTANCE_PERCENT

    init {
        viewModelScope.launch {
            tiltSensor.readings().collect { reading ->
                latestTilt = reading
            }
        }
        viewModelScope.launch {
            // Only register the step-counter listener once permission is settled — registering
            // beforehand can leave the sensor silently not delivering events even after a later grant.
            stepPermissionGranted.filterNotNull().collectLatest { granted ->
                when {
                    !granted -> _uiState.update { it.copy(stepsUnavailableReason = "permission denied") }
                    !stepCounter.isAvailable() -> _uiState.update {
                        it.copy(stepsUnavailableReason = "no step sensor on this device")
                    }
                    else -> {
                        _uiState.update { it.copy(stepsUnavailableReason = null) }
                        stepCounter.steps().collect { count -> onRawStepCount(count) }
                    }
                }
            }
        }
        viewModelScope.launch {
            roomRepository.observeRoom(roomCode).collect { room -> onRoomUpdate(room) }
        }
        viewModelScope.launch {
            var lastTimeMillis = currentTimeMillis()
            while (isActive) {
                delay(PHYSICS_TICK_MS)
                val now = currentTimeMillis()
                val dtSeconds = ((now - lastTimeMillis) / 1000f).coerceAtMost(MAX_DT_SECONDS)
                lastTimeMillis = now
                stepPhysics(dtSeconds)
            }
        }
    }

    fun onCanvasSizeChanged(widthPx: Float, heightPx: Float) {
        val isFirstLayout = canvasWidthPx == null
        canvasWidthPx = widthPx
        canvasHeightPx = heightPx
        if (isFirstLayout) {
            _uiState.update { it.copy(ballX = widthPx / 2f, ballY = heightPx / 2f, ballReady = true) }
        }
    }

    fun onTableSizeChanged(widthPx: Float, heightPx: Float) {
        tableWidthPx = widthPx
        tableHeightPx = heightPx
    }

    fun onBallRadiusPx(radiusPx: Float) {
        ballRadiusPx = radiusPx
    }

    fun onStepPermissionResult(granted: Boolean) {
        stepPermissionGranted.value = granted
    }

    fun onPlayAgain() {
        viewModelScope.launch { roomRepository.playAgain(roomCode) }
    }

    private fun onRawStepCount(rawCount: Int) {
        val delta = rawCount - lastRawStepCount
        lastRawStepCount = rawCount

        val progressValidDistanceFraction = progressValidDistancePercent / 100f
        if (delta > 0 && !localIsEliminated && _uiState.value.distanceFraction <= progressValidDistanceFraction) {
            val targetSteps = _uiState.value.targetSteps
            localValidSteps = if (targetSteps > 0) (localValidSteps + delta).coerceAtMost(targetSteps) else localValidSteps + delta
            viewModelScope.launch { roomRepository.updateValidSteps(roomCode, uid, localValidSteps) }
            if (targetSteps > 0 && localValidSteps >= targetSteps && !hasClaimedVictory) {
                hasClaimedVictory = true
                viewModelScope.launch { roomRepository.claimVictory(roomCode, uid) }
            }
        }

        _uiState.update { it.copy(stepCount = rawCount, validSteps = localValidSteps) }
    }

    private fun onRoomUpdate(room: Room?) {
        if (room == null) return

        progressValidDistancePercent = room.progressValidDistancePercent

        val self = room.players[uid]
        if (self != null && !hasSeededFromRoom) {
            localValidSteps = self.validSteps
            localIsEliminated = self.isEliminated
            hasSeededFromRoom = true
        }

        // A "Play Again" flip back to waiting means a fresh round for everyone — reset our own
        // progress locally and on our own subtree (no one else is allowed to reset it for us).
        if (room.status == RoomStatus.WAITING && (localValidSteps != 0 || localIsEliminated)) {
            localValidSteps = 0
            localIsEliminated = false
            hasClaimedVictory = false
            velocityX = 0f
            velocityY = 0f
            val width = canvasWidthPx
            val height = canvasHeightPx
            _uiState.update {
                it.copy(
                    validSteps = 0,
                    isEliminated = false,
                    ballX = if (width != null) width / 2f else it.ballX,
                    ballY = if (height != null) height / 2f else it.ballY
                )
            }
            viewModelScope.launch { roomRepository.resetOwnProgress(roomCode, uid) }
        }

        val players = room.players.values.map { player ->
            PlayerProgress(
                uid = player.uid,
                username = player.username,
                validSteps = player.validSteps,
                isEliminated = player.isEliminated,
                isSelf = player.uid == uid
            )
        }.sortedByDescending { it.validSteps }

        val winnerUsername = room.winnerUid?.let { winnerUid -> room.players[winnerUid]?.username }

        // Redundant/idempotent on every client — rule guards on the server prevent double-writes.
        if (room.status == RoomStatus.IN_PROGRESS && room.winnerUid == null) {
            val activePlayers = room.players.values.filterNot { it.isEliminated }
            when {
                activePlayers.isEmpty() -> viewModelScope.launch { roomRepository.endWithoutWinner(roomCode) }
                room.players.size > 1 && activePlayers.size == 1 -> {
                    viewModelScope.launch { roomRepository.claimVictory(roomCode, activePlayers.single().uid) }
                }
            }
        }

        _uiState.update {
            it.copy(
                targetSteps = room.targetSteps,
                progressValidDistancePercent = room.progressValidDistancePercent,
                players = players,
                roomStatus = room.status,
                winnerUsername = winnerUsername
            )
        }
    }

    private fun stepPhysics(dtSeconds: Float) {
        val width = canvasWidthPx ?: return
        val height = canvasHeightPx ?: return
        val tableWidth = tableWidthPx ?: return
        val tableHeight = tableHeightPx ?: return
        if (dtSeconds <= 0f) return

        velocityX += latestTilt.rollDegrees * SENSITIVITY_PX_PER_S2_PER_DEGREE * dtSeconds
        // Pitch increases when the top of the phone tilts down, but screen-space Y grows downward
        // toward the *bottom* — so tilting the top down should move the ball down, i.e. this must
        // be inverted relative to the raw pitch sign to feel correct on screen.
        velocityY -= latestTilt.pitchDegrees * SENSITIVITY_PX_PER_S2_PER_DEGREE * dtSeconds

        val damping = DAMPING_RETAINED_PER_SECOND.pow(dtSeconds)
        velocityX *= damping
        velocityY *= damping

        val current = _uiState.value
        var newX = current.ballX + velocityX * dtSeconds
        var newY = current.ballY + velocityY * dtSeconds

        // Outer clamp keeps the ball on-screen even once it's fallen off the table (inner geometry below).
        val minX = ballRadiusPx
        val maxX = width - ballRadiusPx
        val minY = ballRadiusPx
        val maxY = height - ballRadiusPx

        if (newX < minX) {
            newX = minX
            velocityX = 0f
        } else if (newX > maxX) {
            newX = maxX
            velocityX = 0f
        }
        if (newY < minY) {
            newY = minY
            velocityY = 0f
        } else if (newY > maxY) {
            newY = maxY
            velocityY = 0f
        }

        // Table is centered within the full canvas, so its center coincides with the canvas center.
        val centerX = width / 2f
        val centerY = height / 2f
        val distanceFraction = max(
            abs(newX - centerX) / (tableWidth / 2f),
            abs(newY - centerY) / (tableHeight / 2f)
        )

        if (!localIsEliminated && distanceFraction > 1f) {
            localIsEliminated = true
            viewModelScope.launch { roomRepository.markEliminated(roomCode, uid) }
        }

        _uiState.update {
            it.copy(
                ballX = newX,
                ballY = newY,
                distanceFraction = distanceFraction,
                isEliminated = localIsEliminated
            )
        }
    }
}
