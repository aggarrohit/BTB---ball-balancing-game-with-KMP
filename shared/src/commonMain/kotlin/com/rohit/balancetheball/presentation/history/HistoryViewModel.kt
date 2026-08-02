package com.rohit.balancetheball.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohit.balancetheball.domain.model.GameHistoryEntry
import com.rohit.balancetheball.domain.model.OpponentSummary
import com.rohit.balancetheball.domain.repository.HistoryCursor
import com.rohit.balancetheball.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

class HistoryViewModel(
    private val uid: String,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _gameHistoryState = MutableStateFlow(PagedListUiState<GameHistoryEntry>())
    val gameHistoryState: StateFlow<PagedListUiState<GameHistoryEntry>> = _gameHistoryState.asStateFlow()
    private var gameHistoryCursor: HistoryCursor? = null

    private val _opponentsState = MutableStateFlow(PagedListUiState<OpponentSummary>())
    val opponentsState: StateFlow<PagedListUiState<OpponentSummary>> = _opponentsState.asStateFlow()
    private var opponentsCursor: HistoryCursor? = null

    init {
        loadMoreGameHistory()
        loadMoreOpponents()
    }

    fun loadMoreGameHistory() {
        val current = _gameHistoryState.value
        if (current.isLoading || current.isLoadingMore || !current.hasMore) return
        val isFirstPage = current.items.isEmpty()
        _gameHistoryState.update { it.copy(isLoading = isFirstPage, isLoadingMore = !isFirstPage, error = null) }
        viewModelScope.launch {
            historyRepository.loadGameHistoryPage(uid, PAGE_SIZE, gameHistoryCursor)
                .onSuccess { page ->
                    page.items.lastOrNull()?.let { gameHistoryCursor = HistoryCursor(it.finishedAt, it.roundId) }
                    _gameHistoryState.update {
                        it.copy(
                            items = it.items + page.items,
                            isLoading = false,
                            isLoadingMore = false,
                            hasMore = page.hasMore
                        )
                    }
                }
                .onFailure { error ->
                    _gameHistoryState.update {
                        it.copy(isLoading = false, isLoadingMore = false, error = error.message ?: "Could not load game history")
                    }
                }
        }
    }

    fun loadMoreOpponents() {
        val current = _opponentsState.value
        if (current.isLoading || current.isLoadingMore || !current.hasMore) return
        val isFirstPage = current.items.isEmpty()
        _opponentsState.update { it.copy(isLoading = isFirstPage, isLoadingMore = !isFirstPage, error = null) }
        viewModelScope.launch {
            historyRepository.loadOpponentsPage(uid, PAGE_SIZE, opponentsCursor)
                .onSuccess { page ->
                    page.items.lastOrNull()?.let { opponentsCursor = HistoryCursor(it.lastPlayedAt, it.uid) }
                    _opponentsState.update {
                        it.copy(
                            items = it.items + page.items,
                            isLoading = false,
                            isLoadingMore = false,
                            hasMore = page.hasMore
                        )
                    }
                }
                .onFailure { error ->
                    _opponentsState.update {
                        it.copy(isLoading = false, isLoadingMore = false, error = error.message ?: "Could not load opponents")
                    }
                }
        }
    }
}
