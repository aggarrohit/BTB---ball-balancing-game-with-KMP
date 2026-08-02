package com.rohit.balancetheball.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohit.balancetheball.core.sensor.StepCounter
import com.rohit.balancetheball.core.sensor.TiltReading
import com.rohit.balancetheball.core.sensor.TiltSensor
import com.rohit.balancetheball.core.util.currentTimeMillis
import com.rohit.balancetheball.domain.model.Room
import com.rohit.balancetheball.domain.model.RoomStatus
import com.rohit.balancetheball.domain.repository.HistoryRepository
import com.rohit.balancetheball.domain.repository.RoomRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

/** One-shot events GameScreen should react to exactly once (not sticky state). */
sealed interface GameEvent {
    data class PlayerDeclinedPlayAgain(val username: String) : GameEvent
    data object LeftRoomAfterDecline : GameEvent
}

class GameViewModel(
    private val roomCode: String,
    private val uid: String,
    private val tiltSensor: TiltSensor,
    private val stepCounter: StepCounter,
    private val roomRepository: RoomRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

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

    private var lastSeenPlayAgainRequestedBy: String? = null
    private val seenDeclinedUids = mutableSetOf<String>()
    private var lastRecordedHistoryRoundId: String? = null

    // Tilt/step-counter/physics are paused while the app is backgrounded (see GameScreen's
    // LifecycleStartEffect); the room observer stays live so multiplayer state keeps updating.
    private var sensorJobs: List<Job> = emptyList()

    init {
        startSensorsAndPhysics()
        viewModelScope.launch {
            roomRepository.observeRoom(roomCode).collect { room -> onRoomUpdate(room) }
        }
    }

    private fun startSensorsAndPhysics() {
        if (sensorJobs.isNotEmpty()) return
        sensorJobs = listOf(
            viewModelScope.launch {
                tiltSensor.readings().collect { reading -> latestTilt = reading }
            },
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
            },
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
        )
    }

    fun onPauseSensors() {
        sensorJobs.forEach { it.cancel() }
        sensorJobs = emptyList()
    }

    fun onResumeSensors() {
        startSensorsAndPhysics()
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

    fun onProposePlayAgain() {
        viewModelScope.launch {
            if (_uiState.value.players.size <= 1) {
                // Solo room — no one else to send a request to, so skip the consent round-trip
                // and restart immediately.
                roomRepository.playAgain(roomCode)
            } else {
                roomRepository.proposePlayAgain(roomCode, uid)
            }
        }
    }

    fun onAcceptPlayAgain() {
        viewModelScope.launch { roomRepository.acceptPlayAgain(roomCode, uid) }
    }

    fun onDeclinePlayAgain() {
        val username = _uiState.value.players.firstOrNull { it.isSelf }?.username ?: return
        viewModelScope.launch {
            roomRepository.declinePlayAgain(roomCode, uid, username)
            _events.emit(GameEvent.LeftRoomAfterDecline)
        }
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

        // Fires once per (uid, roundId) per client — a fresh roundId only appears after the next
        // Play Again, so this naturally re-arms for each new round. Self-write only, so unlike the
        // other "redundant across every client" writes below, there's no other client to race with.
        if (room.status == RoomStatus.FINISHED && room.roundId != null && room.roundId != lastRecordedHistoryRoundId) {
            lastRecordedHistoryRoundId = room.roundId
            viewModelScope.launch { historyRepository.recordGameHistory(room, uid) }
        }

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

        val request = room.playAgainRequest
        if (request?.requestedBy != lastSeenPlayAgainRequestedBy) {
            // A brand-new request (or the previous one just cleared) — forget which declines we've
            // already toasted for, so a repeat decliner in a later request isn't silently skipped.
            seenDeclinedUids.clear()
            lastSeenPlayAgainRequestedBy = request?.requestedBy
        }
        request?.declinedBy?.forEach { (declinedUid, declinedUsername) ->
            if (declinedUid != uid && seenDeclinedUids.add(declinedUid)) {
                viewModelScope.launch { _events.emit(GameEvent.PlayerDeclinedPlayAgain(declinedUsername)) }
            }
        }
        // Redundant/idempotent across every client — rule guards prevent double-writes. A denier's
        // self-removal shrinks room.players.keys, so this naturally needs one fewer acceptance.
        if (room.status == RoomStatus.FINISHED && request != null &&
            request.acceptedBy.isNotEmpty() && request.acceptedBy == room.players.keys
        ) {
            viewModelScope.launch { roomRepository.playAgain(roomCode) }
        }

        val playAgainUiInfo = request?.let { req ->
            PlayAgainUiInfo(
                requestedByUid = req.requestedBy,
                requestedByUsername = room.players[req.requestedBy]?.username ?: "Someone",
                hasAccepted = uid in req.acceptedBy,
                acceptedCount = req.acceptedBy.size,
                totalNeeded = room.players.size
            )
        }

        _uiState.update {
            it.copy(
                targetSteps = room.targetSteps,
                progressValidDistancePercent = room.progressValidDistancePercent,
                players = players,
                roomStatus = room.status,
                winnerUsername = winnerUsername,
                playAgainRequest = playAgainUiInfo
            )
        }
    }

    private fun stepPhysics(dtSeconds: Float) {
        // Once eliminated the ball is frozen for the rest of the round — it stops responding to
        // tilt entirely rather than drifting back toward the table.
        if (localIsEliminated) return

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
