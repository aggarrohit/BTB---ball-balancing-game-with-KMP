package com.rohit.balancetheball.domain.usecase

import com.rohit.balancetheball.domain.model.Room
import com.rohit.balancetheball.domain.repository.RoomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateRoomUseCaseTest {

    private class FakeRoomRepository(private val shouldFail: Boolean = false) : RoomRepository {
        override suspend fun createRoom(
            hostUid: String,
            hostUsername: String,
            maxPlayers: Int,
            targetSteps: Int,
            progressValidDistancePercent: Int
        ): Result<String> =
            if (shouldFail) Result.failure(RuntimeException("Network error")) else Result.success("1234")

        override suspend fun joinRoom(code: String, uid: String, username: String): Result<Unit> =
            Result.success(Unit)

        override fun observeRoom(code: String): Flow<Room?> = flowOf(null)

        override suspend fun updateValidSteps(code: String, uid: String, validSteps: Int): Result<Unit> =
            Result.success(Unit)

        override suspend fun markEliminated(code: String, uid: String): Result<Unit> = Result.success(Unit)

        override suspend fun tryStartIfFull(code: String): Result<Unit> = Result.success(Unit)

        override suspend fun claimVictory(code: String, uid: String): Result<Unit> = Result.success(Unit)

        override suspend fun endWithoutWinner(code: String): Result<Unit> = Result.success(Unit)

        override suspend fun playAgain(code: String): Result<Unit> = Result.success(Unit)

        override suspend fun resetOwnProgress(code: String, uid: String): Result<Unit> = Result.success(Unit)

        override suspend fun proposePlayAgain(code: String, uid: String): Result<Unit> = Result.success(Unit)

        override suspend fun acceptPlayAgain(code: String, uid: String): Result<Unit> = Result.success(Unit)

        override suspend fun declinePlayAgain(code: String, uid: String, username: String): Result<Unit> =
            Result.success(Unit)
    }

    @Test
    fun `createRoom succeeds with valid inputs`() = runTest {
        val useCase = CreateRoomUseCase(FakeRoomRepository())
        val result = useCase("uid-1", "Host", maxPlayers = 4, targetSteps = 50)
        assertTrue(result.isSuccess)
        assertEquals("1234", result.getOrThrow())
    }

    @Test
    fun `createRoom fails when maxPlayers is below minimum`() = runTest {
        val useCase = CreateRoomUseCase(FakeRoomRepository())
        val result = useCase("uid-1", "Host", maxPlayers = 0, targetSteps = 50)
        assertTrue(result.isFailure)
    }

    @Test
    fun `createRoom fails when maxPlayers is above maximum`() = runTest {
        val useCase = CreateRoomUseCase(FakeRoomRepository())
        val result = useCase("uid-1", "Host", maxPlayers = 11, targetSteps = 50)
        assertTrue(result.isFailure)
    }

    @Test
    fun `createRoom fails when targetSteps is not positive`() = runTest {
        val useCase = CreateRoomUseCase(FakeRoomRepository())
        val result = useCase("uid-1", "Host", maxPlayers = 4, targetSteps = 0)
        assertTrue(result.isFailure)
    }

    @Test
    fun `createRoom defaults balance threshold to 15 percent`() = runTest {
        val useCase = CreateRoomUseCase(FakeRoomRepository())
        val result = useCase("uid-1", "Host", maxPlayers = 4, targetSteps = 50)
        assertTrue(result.isSuccess)
        assertEquals(15, Room.DEFAULT_PROGRESS_VALID_DISTANCE_PERCENT)
    }

    @Test
    fun `createRoom fails when balance threshold is out of range`() = runTest {
        val useCase = CreateRoomUseCase(FakeRoomRepository())
        val result = useCase("uid-1", "Host", maxPlayers = 4, targetSteps = 50, progressValidDistancePercent = 0)
        assertTrue(result.isFailure)
    }

    @Test
    fun `createRoom propagates repository failure`() = runTest {
        val useCase = CreateRoomUseCase(FakeRoomRepository(shouldFail = true))
        val result = useCase("uid-1", "Host", maxPlayers = 4, targetSteps = 50)
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
}
