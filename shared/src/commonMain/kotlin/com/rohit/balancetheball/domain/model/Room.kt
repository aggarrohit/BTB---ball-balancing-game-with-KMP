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

data class Room(
    val code: String,
    val hostUid: String,
    val maxPlayers: Int,
    val targetSteps: Int,
    val status: RoomStatus,
    val winnerUid: String? = null,
    val createdAt: Long = 0L,
    val players: Map<String, RoomPlayer> = emptyMap()
)
