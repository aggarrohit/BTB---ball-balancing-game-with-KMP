package com.rohit.balancetheball.presentation.game

/** All values needed to render the game screen. Ball position is in px, relative to the canvas origin. */
data class GameUiState(
    val ballX: Float = 0f,
    val ballY: Float = 0f,
    val ballReady: Boolean = false,
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
    val stepCount: Int = 0,
    /** Null while steps are tracking normally; explains why they aren't otherwise. */
    val stepsUnavailableReason: String? = null
)
