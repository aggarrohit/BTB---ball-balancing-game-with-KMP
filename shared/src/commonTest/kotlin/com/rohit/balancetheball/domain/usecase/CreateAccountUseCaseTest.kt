package com.rohit.balancetheball.domain.usecase

import kotlinx.coroutines.test.runTest
import com.rohit.balancetheball.domain.model.User
import com.rohit.balancetheball.domain.repository.UserRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateAccountUseCaseTest {

    // --- Fake repository for testing ---
    private class FakeUserRepository(
        private val existingUsernames: Set<String> = emptySet(),
        private val shouldFail: Boolean = false
    ) : UserRepository {
        override suspend fun createAccount(username: String): Result<User> {
            if (shouldFail) return Result.failure(RuntimeException("Network error"))
            if (username in existingUsernames) {
                return Result.failure(IllegalStateException("Username '$username' is already taken"))
            }
            return Result.success(User(username = username, createdAt = 1234567890L))
        }

        override suspend fun getUser(username: String): Result<User?> =
            Result.success(if (username in existingUsernames) User(username) else null)
    }

    @Test
    fun `createAccount succeeds with valid username`() = runTest {
        val useCase = CreateAccountUseCase(FakeUserRepository())
        val result = useCase("Player1")
        assertTrue(result.isSuccess)
        assertEquals("Player1", result.getOrThrow().username)
    }

    @Test
    fun `createAccount trims whitespace before validation`() = runTest {
        val useCase = CreateAccountUseCase(FakeUserRepository())
        val result = useCase("  Player1  ")
        assertTrue(result.isSuccess)
        assertEquals("Player1", result.getOrThrow().username)
    }

    @Test
    fun `createAccount fails when username is blank`() = runTest {
        val useCase = CreateAccountUseCase(FakeUserRepository())
        val result = useCase("   ")
        assertTrue(result.isFailure)
        assertEquals("Username cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createAccount fails when username is too short`() = runTest {
        val useCase = CreateAccountUseCase(FakeUserRepository())
        val result = useCase("ab")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("at least") == true)
    }

    @Test
    fun `createAccount fails when username is too long`() = runTest {
        val useCase = CreateAccountUseCase(FakeUserRepository())
        val result = useCase("a".repeat(21))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("at most") == true)
    }

    @Test
    fun `createAccount fails when username contains invalid characters`() = runTest {
        val useCase = CreateAccountUseCase(FakeUserRepository())
        val result = useCase("bad user!")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("letters, numbers") == true)
    }

    @Test
    fun `createAccount fails when username is already taken`() = runTest {
        val useCase = CreateAccountUseCase(FakeUserRepository(existingUsernames = setOf("TakenUser")))
        val result = useCase("TakenUser")
        assertTrue(result.isFailure)
    }

    @Test
    fun `createAccount propagates repository failure`() = runTest {
        val useCase = CreateAccountUseCase(FakeUserRepository(shouldFail = true))
        val result = useCase("ValidUser")
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
}
