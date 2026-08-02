package com.rohit.balancetheball.presentation.lobby

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rohit.balancetheball.data.auth.FirebaseAuthRepository
import com.rohit.balancetheball.data.remote.FirebaseRoomDataSource
import com.rohit.balancetheball.data.repository.RoomRepositoryImpl
import com.rohit.balancetheball.domain.model.Room
import com.rohit.balancetheball.domain.model.RoomStatus
import com.rohit.balancetheball.domain.model.User
import com.rohit.balancetheball.domain.repository.AuthRepository
import com.rohit.balancetheball.domain.usecase.CreateRoomUseCase
import com.rohit.balancetheball.domain.usecase.JoinRoomUseCase
import com.rohit.balancetheball.domain.model.Room.Companion.DEFAULT_PROGRESS_VALID_DISTANCE_PERCENT
import com.rohit.balancetheball.core.util.exitApp
import com.rohit.balancetheball.presentation.common.AnimatedButton
import com.rohit.balancetheball.presentation.common.AnimatedOutlinedButton
import com.rohit.balancetheball.presentation.common.AppBackground
import com.rohit.balancetheball.presentation.common.GlassCard
import kotlinx.coroutines.launch

private enum class LobbyTab { CREATE, JOIN }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LobbyScreen(
    user: User,
    onRoomStarted: (roomCode: String) -> Unit,
    onLoggedOut: () -> Unit,
    navKey: Long = 0L,
    authRepository: AuthRepository = remember { FirebaseAuthRepository() },
    viewModel: LobbyViewModel = viewModel(key = navKey.toString()) {
        // Manual dependency wiring — swap in a DI framework when needed
        val roomRepository = RoomRepositoryImpl(FirebaseRoomDataSource())
        LobbyViewModel(
            createRoomUseCase = CreateRoomUseCase(roomRepository),
            joinRoomUseCase = JoinRoomUseCase(roomRepository),
            roomRepository = roomRepository,
            uid = user.uid,
            username = user.username
        )
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showExitAppConfirm by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // This is the app's "root" screen after signing in — there's nowhere to navigate back to, so
    // the system back button (Android) should ask before quitting rather than silently exiting.
    // No-ops on iOS (no back-gesture source registered outside a real navigation stack).
    BackHandler(enabled = true) { showExitAppConfirm = true }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is LobbyUiState.WaitingInRoom && state.room.status != RoomStatus.WAITING) {
            onRoomStarted(state.room.code)
        }
    }

    val state = uiState
    if (state is LobbyUiState.WaitingInRoom) {
        WaitingRoomContent(
            room = state.room,
            onBack = viewModel::resetState,
            onLogoutClick = { showLogoutConfirm = true }
        )
    } else {
        LobbyForm(
            uiState = state,
            onCreateRoom = viewModel::onCreateRoom,
            onJoinRoom = viewModel::onJoinRoom,
            onDismissError = viewModel::resetState,
            onLogoutClick = { showLogoutConfirm = true }
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log out?") },
            text = { Text("You'll need to sign in again to play.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    coroutineScope.launch {
                        authRepository.signOut()
                        onLoggedOut()
                    }
                }) { Text("Log out") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showExitAppConfirm) {
        AlertDialog(
            onDismissRequest = { showExitAppConfirm = false },
            title = { Text("Exit app?") },
            text = { Text("Are you sure you want to close Balance The Ball?") },
            confirmButton = {
                TextButton(onClick = { exitApp() }) { Text("Exit") }
            },
            dismissButton = {
                TextButton(onClick = { showExitAppConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun LobbyForm(
    uiState: LobbyUiState,
    onCreateRoom: (maxPlayers: Int, targetSteps: Int, progressValidDistancePercent: Int) -> Unit,
    onJoinRoom: (code: String) -> Unit,
    onDismissError: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var tab by remember { mutableStateOf(LobbyTab.CREATE) }
    var maxPlayers by remember { mutableStateOf(2) }
    var targetSteps by remember { mutableStateOf("50") }
    var balanceThresholdPercent by remember { mutableStateOf(DEFAULT_PROGRESS_VALID_DISTANCE_PERCENT.toString()) }
    var joinCode by remember { mutableStateOf("") }
    val isLoading = uiState is LobbyUiState.Loading

    AppBackground {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "Balance The Ball",
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SegmentedTabButton("Create Room", selected = tab == LobbyTab.CREATE) { tab = LobbyTab.CREATE }
                        SegmentedTabButton("Join Room", selected = tab == LobbyTab.JOIN) { tab = LobbyTab.JOIN }
                    }

                    if (tab == LobbyTab.CREATE) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Players", style = MaterialTheme.typography.bodyLarge)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                AnimatedOutlinedButton(
                                    onClick = { maxPlayers = (maxPlayers - 1).coerceAtLeast(CreateRoomUseCase.MIN_PLAYERS) },
                                    enabled = !isLoading
                                ) { Text("−") }
                                Text("$maxPlayers", style = MaterialTheme.typography.headlineSmall)
                                AnimatedOutlinedButton(
                                    onClick = { maxPlayers = (maxPlayers + 1).coerceAtMost(CreateRoomUseCase.MAX_PLAYERS) },
                                    enabled = !isLoading
                                ) { Text("+") }
                            }

                            OutlinedTextField(
                                value = targetSteps,
                                onValueChange = { targetSteps = it.filter(Char::isDigit) },
                                label = { Text("Target steps to win") },
                                singleLine = true,
                                enabled = !isLoading,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = balanceThresholdPercent,
                                onValueChange = { balanceThresholdPercent = it.filter(Char::isDigit).take(3) },
                                label = { Text("Balance threshold %") },
                                supportingText = { Text("Steps only count while the ball stays within this % of center") },
                                singleLine = true,
                                enabled = !isLoading,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth()
                            )

                            val thresholdValue = balanceThresholdPercent.toIntOrNull() ?: 0
                            AnimatedButton(
                                onClick = { onCreateRoom(maxPlayers, targetSteps.toIntOrNull() ?: 0, thresholdValue) },
                                enabled = !isLoading && (targetSteps.toIntOrNull() ?: 0) > 0 &&
                                    thresholdValue in 1..100,
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Create Room")
                                }
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { if (it.length <= 4) joinCode = it.filter(Char::isDigit) },
                                label = { Text("Room code") },
                                placeholder = { Text("e.g. 4821") },
                                singleLine = true,
                                enabled = !isLoading,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth()
                            )

                            AnimatedButton(
                                onClick = { onJoinRoom(joinCode) },
                                enabled = !isLoading && joinCode.length == 4,
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Join Room")
                                }
                            }
                        }
                    }

                    TextButton(onClick = onLogoutClick) { Text("Logout") }
                }
            }
        }
    }

    if (uiState is LobbyUiState.Error) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("Oops!") },
            text = { Text(uiState.message) },
            confirmButton = {
                TextButton(onClick = onDismissError) { Text("OK") }
            }
        )
    }
}

@Composable
private fun SegmentedTabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        AnimatedButton(onClick = onClick) { Text(text) }
    } else {
        AnimatedOutlinedButton(onClick = onClick) { Text(text) }
    }
}

@Composable
private fun WaitingRoomContent(room: Room, onBack: () -> Unit, onLogoutClick: () -> Unit) {
    AppBackground {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "Room Code",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = room.code,
                        style = MaterialTheme.typography.displayLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Share this code with friends",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Waiting for players: ${room.players.size} / ${room.maxPlayers}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Target: ${room.targetSteps} steps to win",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Balance threshold: ${room.progressValidDistancePercent}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = onBack) { Text("Cancel") }
                    TextButton(onClick = onLogoutClick) { Text("Logout") }
                }
            }
        }
    }
}
