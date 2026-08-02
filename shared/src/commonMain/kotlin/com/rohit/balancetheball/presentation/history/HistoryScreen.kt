package com.rohit.balancetheball.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rohit.balancetheball.data.remote.FirebaseHistoryDataSource
import com.rohit.balancetheball.data.repository.HistoryRepositoryImpl
import com.rohit.balancetheball.domain.model.GameHistoryEntry
import com.rohit.balancetheball.domain.model.GameResult
import com.rohit.balancetheball.domain.model.OpponentSummary
import com.rohit.balancetheball.domain.model.User
import com.rohit.balancetheball.presentation.common.AnimatedButton
import com.rohit.balancetheball.presentation.common.AnimatedOutlinedButton
import com.rohit.balancetheball.presentation.common.AppBackground
import com.rohit.balancetheball.presentation.common.GlassCard
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private enum class HistoryTab { GAMES, OPPONENTS }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HistoryScreen(
    user: User,
    onBack: () -> Unit,
    navKey: Long = 0L,
    viewModel: HistoryViewModel = viewModel(key = navKey.toString()) {
        // Manual dependency wiring — swap in a DI framework when needed
        HistoryViewModel(uid = user.uid, historyRepository = HistoryRepositoryImpl(FirebaseHistoryDataSource()))
    }
) {
    var tab by remember { mutableStateOf(HistoryTab.GAMES) }
    val gameHistoryState by viewModel.gameHistoryState.collectAsStateWithLifecycle()
    val opponentsState by viewModel.opponentsState.collectAsStateWithLifecycle()

    BackHandler(enabled = true) { onBack() }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedOutlinedButton(onClick = onBack) { Text("Back") }
                Text("History", style = MaterialTheme.typography.headlineSmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryTabButton("Games", selected = tab == HistoryTab.GAMES) { tab = HistoryTab.GAMES }
                HistoryTabButton("Opponents", selected = tab == HistoryTab.OPPONENTS) { tab = HistoryTab.OPPONENTS }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (tab) {
                HistoryTab.GAMES -> PagedList(
                    state = gameHistoryState,
                    onLoadMore = viewModel::loadMoreGameHistory,
                    emptyMessage = "No games played yet",
                    itemKey = { it.roundId }
                ) { entry -> GameHistoryRow(entry, modifier = Modifier.fillMaxWidth()) }
                HistoryTab.OPPONENTS -> PagedList(
                    state = opponentsState,
                    onLoadMore = viewModel::loadMoreOpponents,
                    emptyMessage = "No opponents yet",
                    itemKey = { it.uid }
                ) { opponent -> OpponentRow(opponent, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
private fun HistoryTabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        AnimatedButton(onClick = onClick) { Text(text) }
    } else {
        AnimatedOutlinedButton(onClick = onClick) { Text(text) }
    }
}

@Composable
private fun <T> PagedList(
    state: PagedListUiState<T>,
    onLoadMore: () -> Unit,
    emptyMessage: String,
    itemKey: (T) -> Any,
    itemContent: @Composable (T) -> Unit
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            layoutInfo.totalItemsCount > 0 && lastVisible >= layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadMore, state.hasMore, state.isLoadingMore) {
        if (shouldLoadMore && state.hasMore && !state.isLoadingMore) onLoadMore()
    }

    when {
        state.isLoading -> Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.items.isEmpty() -> Text(
            text = state.error ?: emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 32.dp)
        )
        else -> LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(state.items, key = itemKey) { item -> itemContent(item) }
            if (state.isLoadingMore) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
            if (state.error != null) {
                item {
                    Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun GameHistoryRow(entry: GameHistoryEntry, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    GlassCard(modifier = modifier.clickable { expanded = !expanded }, contentPadding = 16.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(formatDate(entry.finishedAt), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Room ${entry.roomCode} · Target ${entry.targetSteps}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ResultBadge(entry.result)
            }
            if (expanded) {
                Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    entry.players.sortedByDescending { it.validSteps }.forEach { player ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(player.username, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "${player.validSteps} steps" + if (player.isEliminated) " · out" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultBadge(result: GameResult) {
    val (label, color) = when (result) {
        GameResult.WON -> "Won" to MaterialTheme.colorScheme.primary
        GameResult.LOST -> "Lost" to MaterialTheme.colorScheme.error
        GameResult.NO_WINNER -> "No winner" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(label, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
}

@Composable
private fun OpponentRow(opponent: OpponentSummary, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, contentPadding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(opponent.username, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Last played ${formatDate(opponent.lastPlayedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("${opponent.gamesPlayedTogether} games", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatDate(epochMillis: Long): String {
    val dateTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    val month = dateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "$month ${dateTime.dayOfMonth}, ${dateTime.year}"
}
