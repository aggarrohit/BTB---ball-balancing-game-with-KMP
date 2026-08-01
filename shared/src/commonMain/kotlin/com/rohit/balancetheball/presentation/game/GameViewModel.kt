package com.rohit.balancetheball.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohit.balancetheball.core.sensor.StepCounter
import com.rohit.balancetheball.core.sensor.TiltReading
import com.rohit.balancetheball.core.sensor.TiltSensor
import com.rohit.balancetheball.core.util.currentTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow

private const val PHYSICS_TICK_MS = 16L
private const val MAX_DT_SECONDS = 0.1f // clamp huge gaps (e.g. after backgrounding) so the ball doesn't jump
private const val SENSITIVITY_PX_PER_S2_PER_DEGREE = 30f
private const val DAMPING_RETAINED_PER_SECOND = 0.35f

class GameViewModel(
    private val tiltSensor: TiltSensor,
    private val stepCounter: StepCounter
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var latestTilt = TiltReading(pitchDegrees = 0f, rollDegrees = 0f)
    private var velocityX = 0f
    private var velocityY = 0f
    private var canvasWidthPx: Float? = null
    private var canvasHeightPx: Float? = null
    private var ballRadiusPx: Float = 0f
    private val stepPermissionGranted = MutableStateFlow<Boolean?>(null)

    init {
        viewModelScope.launch {
            tiltSensor.readings().collect { reading ->
                latestTilt = reading
                _uiState.update { it.copy(pitchDegrees = reading.pitchDegrees, rollDegrees = reading.rollDegrees) }
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
                        stepCounter.steps().collect { count ->
                            _uiState.update { it.copy(stepCount = count) }
                        }
                    }
                }
            }
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

    fun onBallRadiusPx(radiusPx: Float) {
        ballRadiusPx = radiusPx
    }

    fun onStepPermissionResult(granted: Boolean) {
        stepPermissionGranted.value = granted
    }

    private fun stepPhysics(dtSeconds: Float) {
        val width = canvasWidthPx ?: return
        val height = canvasHeightPx ?: return
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

        _uiState.update { it.copy(ballX = newX, ballY = newY) }
    }
}
