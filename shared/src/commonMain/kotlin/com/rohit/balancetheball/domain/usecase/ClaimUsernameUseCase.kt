package com.rohit.balancetheball.domain.usecase

import com.rohit.balancetheball.domain.model.User
import com.rohit.balancetheball.domain.repository.UserRepository

/**
 * Validates a chosen username and delegates claiming it (for an already-signed-in uid) to [UserRepository].
 * All business rules for username validity live here.
 */
class ClaimUsernameUseCase(private val repository: UserRepository) {

    companion object {
        const val MIN_USERNAME_LENGTH = 3
        const val MAX_USERNAME_LENGTH = 20
        private val VALID_USERNAME_REGEX = Regex("^[a-zA-Z0-9_]+$")
    }

    suspend operator fun invoke(uid: String, username: String, email: String?): Result<User> {
        val trimmed = username.trim()
        val validationError = validate(trimmed)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }
        return repository.claimUsername(uid, trimmed, email)
    }

    private fun validate(username: String): String? = when {
        username.isBlank() -> "Username cannot be empty"
        username.length < MIN_USERNAME_LENGTH -> "Username must be at least $MIN_USERNAME_LENGTH characters"
        username.length > MAX_USERNAME_LENGTH -> "Username must be at most $MAX_USERNAME_LENGTH characters"
        !VALID_USERNAME_REGEX.matches(username) -> "Username can only contain letters, numbers, and underscores"
        else -> null
    }
}
