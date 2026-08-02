package com.rohit.balancetheball.domain.repository

import com.rohit.balancetheball.domain.model.GameHistoryEntry
import com.rohit.balancetheball.domain.model.OpponentSummary
import com.rohit.balancetheball.domain.model.Room

/** Identifies the last-seen item of a page, so the next (older) page can be fetched. */
data class HistoryCursor(val timestamp: Long, val key: String)

data class HistoryPage<T>(val items: List<T>, val hasMore: Boolean)

interface HistoryRepository {
    /**
     * Records a just-finished round: the caller's own game-history entry, plus an opponents
     * update for every other player in [room]. Self-write only — call once per (uid, room.roundId).
     */
    suspend fun recordGameHistory(room: Room, uid: String): Result<Unit>

    /** Most-recent-first page of [uid]'s game history. Pass the previous page's last cursor for the next page, null for the first. */
    suspend fun loadGameHistoryPage(uid: String, pageSize: Int, before: HistoryCursor?): Result<HistoryPage<GameHistoryEntry>>

    /** Most-recently-played-with-first page of [uid]'s distinct opponents. */
    suspend fun loadOpponentsPage(uid: String, pageSize: Int, before: HistoryCursor?): Result<HistoryPage<OpponentSummary>>
}
