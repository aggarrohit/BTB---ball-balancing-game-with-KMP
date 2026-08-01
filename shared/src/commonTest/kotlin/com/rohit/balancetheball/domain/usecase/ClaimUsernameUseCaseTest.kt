package com.rohit.balancetheball.domain.usecase

import kotlinx.coroutines.test.runTest
import com.rohit.balancetheball.domain.model.User
import com.rohit.balancetheball.domain.repository.UserRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClaimUsernameUseCaseTest {

    // --- Fake repository for testing ---
    private class FakeUserRepository(
        private val existingUsernames: Set<String> = emptySet(),
        private val shouldFail: Boolean = false
    ) : UserRepository {
        override suspend fun resolveProfile(uid: String): Result<User?> =
            Result.success(null)

        override suspend fun claimUsername(uid: String, username: String, email: String?): Result<User> {
            if (shouldFail) return Result.failure(RuntimeException("Network error"))
            if (username in existingUsernames) {
                return Result.failure(IllegalStateException("Username '$username' is already taken"))
            }
            return Result.success(User(uid = uid, username = username, email = email, createdAt = 1234567890L))
        }
    }

    @Test
    fun `claimUsername succeeds with valid username`() = runTest {
        val useCase = ClaimUsernameUseCase(FakeUserRepository())
        val result = useCase("uid-1", "Player1", null)
        assertTrue(result.isSuccess)
        assertEquals("Player1", result.getOrThrow().username)
    }

    @Test
    fun `claimUsername trims whitespace before validation`() = runTest {
        val useCase = ClaimUsernameUseCase(FakeUserRepository())
        val result = useCase("uid-1", "  Player1  ", null)
        assertTrue(result.isSuccess)
        assertEquals("Player1", result.getOrThrow().username)
    }

    @Test
    fun `claimUsername fails when username is blank`() = runTest {
        val useCase = ClaimUsernameUseCase(FakeUserRepository())
        val result = useCase("uid-1", "   ", null)
        assertTrue(result.isFailure)
        assertEquals("Username cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `claimUsername fails when username is too short`() = runTest {
        val useCase = ClaimUsernameUseCase(FakeUserRepository())
        val result = useCase("uid-1", "ab", null)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("at least") == true)
    }

    @Test
    fun `claimUsername fails when username is too long`() = runTest {
        val useCase = ClaimUsernameUseCase(FakeUserRepository())
        val result = useCase("uid-1", "a".repeat(21), null)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("at most") == true)
    }

    @Test
    fun `claimUsername fails when username contains invalid characters`() = runTest {
        val useCase = ClaimUsernameUseCase(FakeUserRepository())
        val result = useCase("uid-1", "bad user!", null)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("letters, numbers") == true)
    }

    @Test
    fun `claimUsername fails when username is already taken`() = runTest {
        val useCase = ClaimUsernameUseCase(FakeUserRepository(existingUsernames = setOf("TakenUser")))
        val result = useCase("uid-1", "TakenUser", null)
        assertTrue(result.isFailure)
    }

    @Test
    fun `claimUsername propagates repository failure`() = runTest {
        val useCase = ClaimUsernameUseCase(FakeUserRepository(shouldFail = true))
        val result = useCase("uid-1", "ValidUser", null)
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
}
