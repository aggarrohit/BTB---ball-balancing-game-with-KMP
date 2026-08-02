package com.rohit.balancetheball.data.remote

import com.rohit.balancetheball.core.config.AppConfig
import com.rohit.balancetheball.core.util.currentTimeMillis
import com.rohit.balancetheball.domain.model.GameHistoryEntry
import com.rohit.balancetheball.domain.model.GameResult
import com.rohit.balancetheball.domain.model.HistoryPlayerResult
import com.rohit.balancetheball.domain.model.OpponentSummary
import com.rohit.balancetheball.domain.model.Room
import com.rohit.balancetheball.domain.repository.HistoryCursor
import com.rohit.balancetheball.domain.repository.HistoryPage
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.DataSnapshot
import dev.gitlive.firebase.database.DatabaseReference
import dev.gitlive.firebase.database.ServerValue
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.flow.first

/**
 * Reads and writes durable per-player game history to Firebase Realtime Database.
 *
 * Database schema:
 *   /gameHistory/{uid}/{roundId}
 *     roomCode, startedAt, finishedAt, targetSteps, progressValidDistancePercent,
 *     result ("won"|"lost"|"no_winner"), winnerUid?, winnerUsername?, players/{uid}: {username, validSteps, isEliminated}
 *   /opponents/{uid}/{opponentUid}
 *     username, gamesPlayedTogether, lastPlayedAt
 *
 * Both are keyed under, and written only by, the owning uid's own client — no cross-client races,
 * unlike shared room state. See database.rules.json.
 */
class FirebaseHistoryDataSource {

    private val db by lazy {
        val url = AppConfig.firebaseDatabaseUrl
        if (url.isBlank()) Firebase.database
        else Firebase.database(url)
    }

    suspend fun recordGameHistory(room: Room, uid: String) {
        val roundId = room.roundId ?: return
        val finishedAt = currentTimeMillis()
        val result = when {
            room.winnerUid == uid -> GameResult.WON
            room.winnerUid != null -> GameResult.LOST
            else -> GameResult.NO_WINNER
        }
        val winnerUsername = room.winnerUid?.let { winnerUid -> room.players[winnerUid]?.username }

        val updates = mutableMapOf<String, Any?>(
            "gameHistory/$uid/$roundId" to mapOf(
                "roomCode" to room.code,
                "startedAt" to (room.roundStartedAt ?: room.createdAt),
                "finishedAt" to finishedAt,
                "targetSteps" to room.targetSteps,
                "progressValidDistancePercent" to room.progressValidDistancePercent,
                "result" to result.toWireValue(),
                "winnerUid" to room.winnerUid,
                "winnerUsername" to winnerUsername,
                "players" to room.players.mapValues { (_, player) ->
                    mapOf(
                        "username" to player.username,
                        "validSteps" to player.validSteps,
                        "isEliminated" to player.isEliminated
                    )
                }
            )
        )

        room.players.values.filter { it.uid != uid }.forEach { opponent ->
            updates["opponents/$uid/${opponent.uid}/username"] = opponent.username
            updates["opponents/$uid/${opponent.uid}/lastPlayedAt"] = finishedAt
            updates["opponents/$uid/${opponent.uid}/gamesPlayedTogether"] = ServerValue.increment(1.0)
        }

        db.reference().updateChildren(updates)
    }

    suspend fun loadGameHistoryPage(uid: String, pageSize: Int, before: HistoryCursor?): HistoryPage<GameHistoryEntry> =
        loadPage(db.reference("gameHistory/$uid"), "finishedAt", pageSize, before) { toGameHistoryEntry() }

    suspend fun loadOpponentsPage(uid: String, pageSize: Int, before: HistoryCursor?): HistoryPage<OpponentSummary> =
        loadPage(db.reference("opponents/$uid"), "lastPlayedAt", pageSize, before) { toOpponentSummary() }

    /**
     * Realtime Database has no endBefore/startAfter (Firestore-only names) — cursoring uses
     * endAt(cursorValue, cursorKey).limitToLast(pageSize + 1) and drops the duplicate boundary
     * item (the cursor item itself, already shown on the previous page). limitToLast always
     * returns ascending order, so results are reversed to most-recent-first.
     */
    private suspend fun <T> loadPage(
        ref: DatabaseReference,
        orderByField: String,
        pageSize: Int,
        before: HistoryCursor?,
        parse: DataSnapshot.() -> T?
    ): HistoryPage<T> {
        val query = if (before != null) {
            ref.orderByChild(orderByField).endAt(before.timestamp.toDouble(), before.key).limitToLast(pageSize + 1)
        } else {
            ref.orderByChild(orderByField).limitToLast(pageSize)
        }
        val snapshot = query.valueEvents.first()
        val rawChildren = snapshot.children.toList()
        val trimmed = if (before != null && rawChildren.isNotEmpty()) rawChildren.dropLast(1) else rawChildren
        val items = trimmed.mapNotNull { it.parse() }.asReversed()
        val hasMore = if (before != null) rawChildren.size == pageSize + 1 else rawChildren.size == pageSize
        return HistoryPage(items, hasMore)
    }

    @Suppress("UNCHECKED_CAST")
    private fun DataSnapshot.toGameHistoryEntry(): GameHistoryEntry? {
        val roundId = key ?: return null
        val map = value as? Map<String, Any?> ?: return null
        val playersMap = map["players"] as? Map<String, Any?> ?: emptyMap()
        val players = playersMap.mapNotNull { (playerUid, raw) ->
            val playerMap = raw as? Map<String, Any?> ?: return@mapNotNull null
            val username = playerMap["username"] as? String ?: return@mapNotNull null
            HistoryPlayerResult(
                uid = playerUid,
                username = username,
                validSteps = (playerMap["validSteps"] as? Long)?.toInt() ?: 0,
                isEliminated = playerMap["isEliminated"] as? Boolean ?: false
            )
        }

        return GameHistoryEntry(
            roundId = roundId,
            roomCode = map["roomCode"] as? String ?: return null,
            startedAt = map["startedAt"] as? Long ?: 0L,
            finishedAt = map["finishedAt"] as? Long ?: return null,
            targetSteps = (map["targetSteps"] as? Long)?.toInt() ?: 0,
            progressValidDistancePercent = (map["progressValidDistancePercent"] as? Long)?.toInt()
                ?: Room.DEFAULT_PROGRESS_VALID_DISTANCE_PERCENT,
            result = GameResult.fromWireValue(map["result"] as? String),
            winnerUsername = map["winnerUsername"] as? String,
            players = players
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun DataSnapshot.toOpponentSummary(): OpponentSummary? {
        val opponentUid = key ?: return null
        val map = value as? Map<String, Any?> ?: return null
        return OpponentSummary(
            uid = opponentUid,
            username = map["username"] as? String ?: return null,
            gamesPlayedTogether = (map["gamesPlayedTogether"] as? Long)?.toInt() ?: 0,
            lastPlayedAt = map["lastPlayedAt"] as? Long ?: return null
        )
    }
}
