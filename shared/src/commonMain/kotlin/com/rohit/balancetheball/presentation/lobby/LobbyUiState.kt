package com.rohit.balancetheball.presentation.lobby

import com.rohit.balancetheball.domain.model.Room

/**
 * All possible states of the Lobby screen (create/join a room, then wait for it to fill).
 */
sealed interface LobbyUiState {
    data object Idle : LobbyUiState
    data object Loading : LobbyUiState
    data class WaitingInRoom(val room: Room) : LobbyUiState
    data class Error(val message: String) : LobbyUiState
}
