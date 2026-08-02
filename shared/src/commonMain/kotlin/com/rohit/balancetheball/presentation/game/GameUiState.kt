package com.rohit.balancetheball.presentation.game

import com.rohit.balancetheball.domain.model.Room
import com.rohit.balancetheball.domain.model.RoomStatus

data class PlayerProgress(
    val uid: String,
    val username: String,
    val validSteps: Int,
    val isEliminated: Boolean,
    val isSelf: Boolean
)

/** An in-flight play-again request as seen by this client. Null once resolved/cleared or if none is active. */
data class PlayAgainUiInfo(
    val requestedByUid: String,
    val requestedByUsername: String,
    val hasAccepted: Boolean,
    val acceptedCount: Int,
    val totalNeeded: Int
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
    val progressValidDistancePercent: Int = Room.DEFAULT_PROGRESS_VALID_DISTANCE_PERCENT,
    val isEliminated: Boolean = false,
    val players: List<PlayerProgress> = emptyList(),
    val roomStatus: RoomStatus = RoomStatus.IN_PROGRESS,
    val winnerUsername: String? = null,
    val playAgainRequest: PlayAgainUiInfo? = null,
    /** Non-null while the post-restart countdown is showing; ticks down to null once it hits zero. */
    val countdownSecondsRemaining: Int? = null
)
