package com.rohit.balancetheball.domain.repository

import com.rohit.balancetheball.domain.model.Room
import kotlinx.coroutines.flow.Flow

interface RoomRepository {
    /** Generates and claims a fresh 4-digit room code, retrying on collision. */
    suspend fun createRoom(
        hostUid: String,
        hostUsername: String,
        maxPlayers: Int,
        targetSteps: Int,
        progressValidDistancePercent: Int = Room.DEFAULT_PROGRESS_VALID_DISTANCE_PERCENT
    ): Result<String>

    /** Joins an existing, non-full, still-waiting room. */
    suspend fun joinRoom(code: String, uid: String, username: String): Result<Unit>

    /** Emits the room's state on every change (including immediately on subscription), or null if it doesn't exist. */
    fun observeRoom(code: String): Flow<Room?>

    /** Updates the caller's own valid-step progress within [code]. */
    suspend fun updateValidSteps(code: String, uid: String, validSteps: Int): Result<Unit>

    /** Marks the caller as permanently eliminated for this round. */
    suspend fun markEliminated(code: String, uid: String): Result<Unit>

    /** Idempotent: flips a full, still-waiting room to in_progress. Safe to call redundantly from any client. */
    suspend fun tryStartIfFull(code: String): Result<Unit>

    /** Atomically claims the win for [uid]. A rejected write just means someone else already won. */
    suspend fun claimVictory(code: String, uid: String): Result<Unit>

    /** Idempotent: ends a still-in-progress round with no winner (e.g. every player fell off the table). */
    suspend fun endWithoutWinner(code: String): Result<Unit>

    /** Atomically resets the room back to waiting so a new round can start with the same roster. */
    suspend fun playAgain(code: String): Result<Unit>

    /** Resets the caller's own progress for a new round (call after [playAgain] flips the room back to waiting). */
    suspend fun resetOwnProgress(code: String, uid: String): Result<Unit>
}
