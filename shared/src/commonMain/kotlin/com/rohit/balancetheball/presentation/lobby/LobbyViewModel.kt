package com.rohit.balancetheball.presentation.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohit.balancetheball.domain.model.RoomStatus
import com.rohit.balancetheball.domain.repository.RoomRepository
import com.rohit.balancetheball.domain.usecase.CreateRoomUseCase
import com.rohit.balancetheball.domain.usecase.JoinRoomUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LobbyViewModel(
    private val createRoomUseCase: CreateRoomUseCase,
    private val joinRoomUseCase: JoinRoomUseCase,
    private val roomRepository: RoomRepository,
    private val uid: String,
    private val username: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<LobbyUiState>(LobbyUiState.Idle)
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    fun onCreateRoom(maxPlayers: Int, targetSteps: Int, progressValidDistancePercent: Int) {
        viewModelScope.launch {
            _uiState.value = LobbyUiState.Loading
            createRoomUseCase(uid, username, maxPlayers, targetSteps, progressValidDistancePercent)
                .onSuccess { code -> observeRoom(code) }
                .onFailure { error ->
                    _uiState.value = LobbyUiState.Error(error.message ?: "Could not create room")
                }
        }
    }

    fun onJoinRoom(code: String) {
        viewModelScope.launch {
            _uiState.value = LobbyUiState.Loading
            joinRoomUseCase(code, uid, username)
                .onSuccess { joinedCode -> observeRoom(joinedCode) }
                .onFailure { error ->
                    _uiState.value = LobbyUiState.Error(error.message ?: "Could not join room")
                }
        }
    }

    fun resetState() {
        observeJob?.cancel()
        observeJob = null
        _uiState.value = LobbyUiState.Idle
    }

    private fun observeRoom(code: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            roomRepository.observeRoom(code).collect { room ->
                if (room == null) {
                    _uiState.value = LobbyUiState.Error("This room no longer exists")
                    return@collect
                }
                _uiState.value = LobbyUiState.WaitingInRoom(room)
                if (room.status == RoomStatus.WAITING && room.players.size >= room.maxPlayers) {
                    // Safe to call redundantly from every client in the room — see RoomRepository docs.
                    roomRepository.tryStartIfFull(code)
                }
            }
        }
    }
}
