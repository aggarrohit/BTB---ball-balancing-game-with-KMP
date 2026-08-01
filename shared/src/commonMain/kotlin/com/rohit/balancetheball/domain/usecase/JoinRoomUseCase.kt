package com.rohit.balancetheball.domain.usecase

import com.rohit.balancetheball.domain.repository.RoomRepository

class JoinRoomUseCase(private val repository: RoomRepository) {

    companion object {
        private val CODE_REGEX = Regex("^[0-9]{4}$")
    }

    suspend operator fun invoke(code: String, uid: String, username: String): Result<String> {
        val trimmed = code.trim()
        if (!CODE_REGEX.matches(trimmed)) {
            return Result.failure(IllegalArgumentException("Room code must be exactly 4 digits"))
        }
        return repository.joinRoom(trimmed, uid, username).map { trimmed }
    }
}
