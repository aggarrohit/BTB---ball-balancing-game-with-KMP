package com.rohit.balancetheball.data.repository

import com.rohit.balancetheball.data.remote.FirebaseRoomDataSource
import com.rohit.balancetheball.domain.model.Room
import com.rohit.balancetheball.domain.model.RoomStatus
import com.rohit.balancetheball.domain.repository.RoomRepository
import kotlinx.coroutines.flow.Flow

class RoomRepositoryImpl(
    private val dataSource: FirebaseRoomDataSource
) : RoomRepository {

    override suspend fun createRoom(
        hostUid: String,
        hostUsername: String,
        maxPlayers: Int,
        targetSteps: Int,
        progressValidDistancePercent: Int
    ): Result<String> = runCatching {
        dataSource.createRoom(hostUid, hostUsername, maxPlayers, targetSteps, progressValidDistancePercent)
    }

    override suspend fun joinRoom(code: String, uid: String, username: String): Result<Unit> = runCatching {
        dataSource.joinRoom(code, uid, username)
    }

    override fun observeRoom(code: String): Flow<Room?> = dataSource.observeRoom(code)

    override suspend fun updateValidSteps(code: String, uid: String, validSteps: Int): Result<Unit> = runCatching {
        dataSource.updateValidSteps(code, uid, validSteps)
    }

    override suspend fun markEliminated(code: String, uid: String): Result<Unit> = runCatching {
        dataSource.markEliminated(code, uid)
    }

    override suspend fun tryStartIfFull(code: String): Result<Unit> = runCatching {
        // Redundant/rejected calls (room already started, or not actually full yet) are expected
        // and harmless — every client may attempt this, only the room Flow reflects the true state.
        try {
            dataSource.setStatus(code, RoomStatus.IN_PROGRESS)
        } catch (_: Exception) {
            // ignored — see above
        }
    }

    override suspend fun claimVictory(code: String, uid: String): Result<Unit> = runCatching {
        dataSource.claimVictory(code, uid)
    }

    override suspend fun endWithoutWinner(code: String): Result<Unit> = runCatching {
        // Redundant/rejected calls are expected and harmless — every client may attempt this.
        try {
            dataSource.setStatus(code, RoomStatus.FINISHED)
        } catch (_: Exception) {
            // ignored — see above
        }
    }

    override suspend fun playAgain(code: String): Result<Unit> = runCatching {
        dataSource.playAgain(code)
    }

    override suspend fun resetOwnProgress(code: String, uid: String): Result<Unit> = runCatching {
        dataSource.resetOwnProgress(code, uid)
    }

    override suspend fun proposePlayAgain(code: String, uid: String): Result<Unit> = runCatching {
        dataSource.proposePlayAgain(code, uid)
    }

    override suspend fun acceptPlayAgain(code: String, uid: String): Result<Unit> = runCatching {
        dataSource.acceptPlayAgain(code, uid)
    }

    override suspend fun declinePlayAgain(code: String, uid: String, username: String): Result<Unit> = runCatching {
        dataSource.declinePlayAgain(code, uid, username)
    }
}
