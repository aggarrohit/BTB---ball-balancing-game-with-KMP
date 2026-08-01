package com.rohit.balancetheball.domain.usecase

import com.rohit.balancetheball.domain.repository.RoomRepository

class CreateRoomUseCase(private val repository: RoomRepository) {

    companion object {
        const val MIN_PLAYERS = 1
        const val MAX_PLAYERS = 10
    }

    suspend operator fun invoke(
        hostUid: String,
        hostUsername: String,
        maxPlayers: Int,
        targetSteps: Int
    ): Result<String> {
        val validationError = validate(maxPlayers, targetSteps)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }
        return repository.createRoom(hostUid, hostUsername, maxPlayers, targetSteps)
    }

    private fun validate(maxPlayers: Int, targetSteps: Int): String? = when {
        maxPlayers < MIN_PLAYERS || maxPlayers > MAX_PLAYERS ->
            "Players must be between $MIN_PLAYERS and $MAX_PLAYERS"
        targetSteps <= 0 -> "Target steps must be greater than zero"
        else -> null
    }
}
