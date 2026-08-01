package com.rohit.balancetheball.domain.usecase

import com.rohit.balancetheball.domain.model.Room
import com.rohit.balancetheball.domain.repository.RoomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JoinRoomUseCaseTest {

    private class FakeRoomRepository(
        private val fullCodes: Set<String> = emptySet(),
        private val shouldFail: Boolean = false
    ) : RoomRepository {
        override suspend fun createRoom(
            hostUid: String,
            hostUsername: String,
            maxPlayers: Int,
            targetSteps: Int
        ): Result<String> = Result.success("1234")

        override suspend fun joinRoom(code: String, uid: String, username: String): Result<Unit> = when {
            shouldFail -> Result.failure(RuntimeException("Network error"))
            code in fullCodes -> Result.failure(IllegalStateException("Room is full"))
            else -> Result.success(Unit)
        }

        override fun observeRoom(code: String): Flow<Room?> = flowOf(null)

        override suspend fun updateValidSteps(code: String, uid: String, validSteps: Int): Result<Unit> =
            Result.success(Unit)

        override suspend fun markEliminated(code: String, uid: String): Result<Unit> = Result.success(Unit)

        override suspend fun tryStartIfFull(code: String): Result<Unit> = Result.success(Unit)

        override suspend fun claimVictory(code: String, uid: String): Result<Unit> = Result.success(Unit)

        override suspend fun playAgain(code: String): Result<Unit> = Result.success(Unit)

        override suspend fun resetOwnProgress(code: String, uid: String): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun `joinRoom succeeds with a valid 4-digit code`() = runTest {
        val useCase = JoinRoomUseCase(FakeRoomRepository())
        val result = useCase("1234", "uid-1", "Player1")
        assertTrue(result.isSuccess)
        assertEquals("1234", result.getOrThrow())
    }

    @Test
    fun `joinRoom trims whitespace before validation`() = runTest {
        val useCase = JoinRoomUseCase(FakeRoomRepository())
        val result = useCase("  1234  ", "uid-1", "Player1")
        assertTrue(result.isSuccess)
        assertEquals("1234", result.getOrThrow())
    }

    @Test
    fun `joinRoom fails when code is not 4 digits`() = runTest {
        val useCase = JoinRoomUseCase(FakeRoomRepository())
        val result = useCase("123", "uid-1", "Player1")
        assertTrue(result.isFailure)
    }

    @Test
    fun `joinRoom fails when code contains non-digits`() = runTest {
        val useCase = JoinRoomUseCase(FakeRoomRepository())
        val result = useCase("12a4", "uid-1", "Player1")
        assertTrue(result.isFailure)
    }

    @Test
    fun `joinRoom propagates repository failure when room is full`() = runTest {
        val useCase = JoinRoomUseCase(FakeRoomRepository(fullCodes = setOf("1234")))
        val result = useCase("1234", "uid-1", "Player1")
        assertTrue(result.isFailure)
    }
}
