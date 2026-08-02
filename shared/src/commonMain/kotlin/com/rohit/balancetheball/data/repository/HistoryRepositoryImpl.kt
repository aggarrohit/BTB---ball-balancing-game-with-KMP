package com.rohit.balancetheball.data.repository

import com.rohit.balancetheball.data.remote.FirebaseHistoryDataSource
import com.rohit.balancetheball.domain.model.GameHistoryEntry
import com.rohit.balancetheball.domain.model.OpponentSummary
import com.rohit.balancetheball.domain.model.Room
import com.rohit.balancetheball.domain.repository.HistoryCursor
import com.rohit.balancetheball.domain.repository.HistoryPage
import com.rohit.balancetheball.domain.repository.HistoryRepository

class HistoryRepositoryImpl(
    private val dataSource: FirebaseHistoryDataSource
) : HistoryRepository {

    override suspend fun recordGameHistory(room: Room, uid: String): Result<Unit> = runCatching {
        dataSource.recordGameHistory(room, uid)
    }

    override suspend fun loadGameHistoryPage(
        uid: String,
        pageSize: Int,
        before: HistoryCursor?
    ): Result<HistoryPage<GameHistoryEntry>> = runCatching {
        dataSource.loadGameHistoryPage(uid, pageSize, before)
    }

    override suspend fun loadOpponentsPage(
        uid: String,
        pageSize: Int,
        before: HistoryCursor?
    ): Result<HistoryPage<OpponentSummary>> = runCatching {
        dataSource.loadOpponentsPage(uid, pageSize, before)
    }
}
