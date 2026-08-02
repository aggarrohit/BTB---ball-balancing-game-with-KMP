package com.rohit.balancetheball.domain.model

enum class GameResult {
    WON, LOST, NO_WINNER;

    companion object {
        fun fromWireValue(value: String?): GameResult = when (value) {
            "won" -> WON
            "lost" -> LOST
            else -> NO_WINNER
        }
    }

    fun toWireValue(): String = when (this) {
        WON -> "won"
        LOST -> "lost"
        NO_WINNER -> "no_winner"
    }
}

data class HistoryPlayerResult(
    val uid: String,
    val username: String,
    val validSteps: Int,
    val isEliminated: Boolean
)

/** A durable, immutable record of one finished round, from the perspective of the player it's stored under. */
data class GameHistoryEntry(
    val roundId: String,
    val roomCode: String,
    val startedAt: Long,
    val finishedAt: Long,
    val targetSteps: Int,
    val progressValidDistancePercent: Int,
    val result: GameResult,
    val winnerUsername: String?,
    val players: List<HistoryPlayerResult>
)
