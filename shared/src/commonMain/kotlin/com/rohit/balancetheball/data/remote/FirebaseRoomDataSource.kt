package com.rohit.balancetheball.data.remote

import com.rohit.balancetheball.core.config.AppConfig
import com.rohit.balancetheball.core.util.currentTimeMillis
import com.rohit.balancetheball.domain.model.Room
import com.rohit.balancetheball.domain.model.RoomPlayer
import com.rohit.balancetheball.domain.model.RoomStatus
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.DataSnapshot
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.random.Random

private const val MAX_CREATE_ATTEMPTS = 5

/**
 * Reads and writes multiplayer room data to Firebase Realtime Database.
 *
 * Database schema:
 *   /rooms
 *     /{code}
 *       code, hostUid, maxPlayers, targetSteps, status, winnerUid?, createdAt
 *       players/{uid}: username, validSteps, isEliminated, joinedAt
 *
 * `createRoom` and `claimVictory`/`playAgain` write via a single root-level updateChildren call
 * so the whole nested value lands as one write, checked against one security rule — see
 * database.rules.json for why this matters (flattening these into per-field keys would make
 * each field get checked against a *different*, more specific rule that can't pass at creation time).
 */
class FirebaseRoomDataSource {

    private val db by lazy {
        val url = AppConfig.firebaseDatabaseUrl
        if (url.isBlank()) Firebase.database
        else Firebase.database(url)
    }

    private fun roomRef(code: String) = db.reference("rooms/$code")
    private fun playerRef(code: String, uid: String) = roomRef(code).child("players").child(uid)

    /** Generates and claims a free 4-digit code, retrying on collision. Returns the claimed code. */
    suspend fun createRoom(hostUid: String, hostUsername: String, maxPlayers: Int, targetSteps: Int): String {
        var lastError: Throwable? = null
        repeat(MAX_CREATE_ATTEMPTS) {
            val code = randomCode()
            val now = currentTimeMillis()
            try {
                db.reference().updateChildren(
                    mapOf(
                        "rooms/$code" to mapOf(
                            "code" to code,
                            "hostUid" to hostUid,
                            "maxPlayers" to maxPlayers,
                            "targetSteps" to targetSteps,
                            "status" to RoomStatus.WAITING.toWireValue(),
                            "createdAt" to now,
                            "players" to mapOf(
                                hostUid to mapOf(
                                    "username" to hostUsername,
                                    "validSteps" to 0,
                                    "isEliminated" to false,
                                    "joinedAt" to now
                                )
                            )
                        )
                    )
                )
                return code
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Could not generate a free room code")
    }

    /**
     * Room capacity isn't enforced by the security rules (Firebase's rules linter rejects
     * numChildren() in this project for reasons that couldn't be pinned down further), so it's
     * checked here instead — a client-side, not perfectly race-safe check, acceptable for a
     * casual game that already trusts room members for other shared state.
     */
    suspend fun joinRoom(code: String, uid: String, username: String) {
        val room = roomRef(code).valueEvents.first().toRoom(code)
            ?: throw IllegalStateException("Room not found")
        if (room.status != RoomStatus.WAITING) {
            throw IllegalStateException("This room has already started")
        }
        if (uid !in room.players && room.players.size >= room.maxPlayers) {
            throw IllegalStateException("This room is full")
        }

        playerRef(code, uid).setValue(
            mapOf(
                "username" to username,
                "validSteps" to 0,
                "isEliminated" to false,
                "joinedAt" to currentTimeMillis()
            )
        )
    }

    fun observeRoom(code: String): Flow<Room?> =
        roomRef(code).valueEvents.map { snapshot -> snapshot.toRoom(code) }

    suspend fun updateValidSteps(code: String, uid: String, validSteps: Int) {
        playerRef(code, uid).child("validSteps").setValue(validSteps)
    }

    suspend fun markEliminated(code: String, uid: String) {
        playerRef(code, uid).child("isEliminated").setValue(true)
    }

    suspend fun setStatus(code: String, status: RoomStatus) {
        roomRef(code).child("status").setValue(status.toWireValue())
    }

    suspend fun claimVictory(code: String, uid: String) {
        db.reference().updateChildren(
            mapOf(
                "rooms/$code/winnerUid" to uid,
                "rooms/$code/status" to RoomStatus.FINISHED.toWireValue()
            )
        )
    }

    suspend fun playAgain(code: String) {
        db.reference().updateChildren(
            mapOf(
                "rooms/$code/status" to RoomStatus.WAITING.toWireValue(),
                "rooms/$code/winnerUid" to null
            )
        )
    }

    suspend fun resetOwnProgress(code: String, uid: String) {
        playerRef(code, uid).updateChildren(
            mapOf(
                "validSteps" to 0,
                "isEliminated" to false
            )
        )
    }

    private fun randomCode(): String = Random.nextInt(0, 10000).toString().padStart(4, '0')

    @Suppress("UNCHECKED_CAST")
    private fun DataSnapshot.toRoom(code: String): Room? {
        val map = value as? Map<String, Any?> ?: return null
        val playersMap = map["players"] as? Map<String, Any?> ?: emptyMap()
        val players = playersMap.mapNotNull { (uid, raw) ->
            val playerMap = raw as? Map<String, Any?> ?: return@mapNotNull null
            val username = playerMap["username"] as? String ?: return@mapNotNull null
            uid to RoomPlayer(
                uid = uid,
                username = username,
                validSteps = (playerMap["validSteps"] as? Long)?.toInt() ?: 0,
                isEliminated = playerMap["isEliminated"] as? Boolean ?: false,
                joinedAt = playerMap["joinedAt"] as? Long ?: 0L
            )
        }.toMap()

        return Room(
            code = map["code"] as? String ?: code,
            hostUid = map["hostUid"] as? String ?: return null,
            maxPlayers = (map["maxPlayers"] as? Long)?.toInt() ?: return null,
            targetSteps = (map["targetSteps"] as? Long)?.toInt() ?: return null,
            status = RoomStatus.fromWireValue(map["status"] as? String),
            winnerUid = map["winnerUid"] as? String,
            createdAt = map["createdAt"] as? Long ?: 0L,
            players = players
        )
    }
}
