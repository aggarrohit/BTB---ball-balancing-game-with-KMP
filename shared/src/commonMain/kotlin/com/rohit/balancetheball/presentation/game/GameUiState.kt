package com.rohit.balancetheball.presentation.game

import com.rohit.balancetheball.domain.model.RoomStatus

data class PlayerProgress(
    val uid: String,
    val username: String,
    val validSteps: Int,
    val isEliminated: Boolean,
    val isSelf: Boolean
)

/** All values needed to render the game screen. Ball position is in px, relative to the canvas origin. */
data class GameUiState(
    val ballX: Float = 0f,
    val ballY: Float = 0f,
    val ballReady: Boolean = false,
    /** 0.0 at the table's center, 1.0 exactly at its edge, >1.0 once off the table. */
    val distanceFraction: Float = 0f,
    val stepCount: Int = 0,
    val stepsUnavailableReason: String? = null,
    val validSteps: Int = 0,
    val targetSteps: Int = 0,
    val isEliminated: Boolean = false,
    val players: List<PlayerProgress> = emptyList(),
    val roomStatus: RoomStatus = RoomStatus.IN_PROGRESS,
    val winnerUsername: String? = null
)
