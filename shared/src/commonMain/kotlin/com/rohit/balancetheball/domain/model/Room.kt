package com.rohit.balancetheball.domain.model

enum class RoomStatus {
    WAITING, IN_PROGRESS, FINISHED;

    companion object {
        fun fromWireValue(value: String?): RoomStatus = when (value) {
            "in_progress" -> IN_PROGRESS
            "finished" -> FINISHED
            else -> WAITING
        }
    }

    fun toWireValue(): String = when (this) {
        WAITING -> "waiting"
        IN_PROGRESS -> "in_progress"
        FINISHED -> "finished"
    }
}

data class RoomPlayer(
    val uid: String,
    val username: String,
    val validSteps: Int = 0,
    val isEliminated: Boolean = false,
    val joinedAt: Long = 0L
)

/**
 * An in-flight "play again?" proposal. [acceptedBy] always includes [requestedBy] (auto-accepted
 * at creation). [declinedBy] maps uid -> username, since a decliner's [RoomPlayer] entry is
 * removed in the same write, so their username wouldn't be recoverable from [Room.players] by
 * the time other clients render it.
 */
data class PlayAgainRequest(
    val requestedBy: String,
    val acceptedBy: Set<String> = emptySet(),
    val declinedBy: Map<String, String> = emptyMap()
)

data class Room(
    val code: String,
    val hostUid: String,
    val maxPlayers: Int,
    val targetSteps: Int,
    /** Steps only count toward progress while the ball's distance from the table's center stays under this percent of the table's half-extents. */
    val progressValidDistancePercent: Int = DEFAULT_PROGRESS_VALID_DISTANCE_PERCENT,
    val status: RoomStatus,
    val winnerUid: String? = null,
    val createdAt: Long = 0L,
    val players: Map<String, RoomPlayer> = emptyMap(),
    val playAgainRequest: PlayAgainRequest? = null
) {
    companion object {
        const val DEFAULT_PROGRESS_VALID_DISTANCE_PERCENT = 15
        const val MIN_PROGRESS_VALID_DISTANCE_PERCENT = 1
        const val MAX_PROGRESS_VALID_DISTANCE_PERCENT = 100
    }
}
