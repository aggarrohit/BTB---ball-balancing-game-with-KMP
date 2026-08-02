package com.rohit.balancetheball.presentation.history

/** Shared shape for either paginated list (game history, opponents) — see [HistoryViewModel]. */
data class PagedListUiState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null
)
