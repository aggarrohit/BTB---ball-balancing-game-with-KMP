package com.rohit.balancetheball.presentation.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohit.balancetheball.core.push.PendingInvite
import com.rohit.balancetheball.domain.repository.InviteRepository
import com.rohit.balancetheball.domain.usecase.JoinRoomUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface InviteResponseUiState {
    data object Idle : InviteResponseUiState
    data object Loading : InviteResponseUiState
    data class Joined(val roomCode: String) : InviteResponseUiState
    data object Declined : InviteResponseUiState
    data class Error(val message: String) : InviteResponseUiState
}

class InviteResponseViewModel(
    private val uid: String,
    private val username: String,
    private val invite: PendingInvite,
    private val joinRoomUseCase: JoinRoomUseCase,
    private val inviteRepository: InviteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<InviteResponseUiState>(InviteResponseUiState.Idle)
    val uiState: StateFlow<InviteResponseUiState> = _uiState.asStateFlow()

    fun onAccept() {
        viewModelScope.launch {
            _uiState.value = InviteResponseUiState.Loading
            joinRoomUseCase(invite.roomCode, uid, username)
                .onSuccess { code -> _uiState.value = InviteResponseUiState.Joined(code) }
                .onFailure { error ->
                    _uiState.value = InviteResponseUiState.Error(error.message ?: "Could not join room")
                }
        }
    }

    fun onDecline() {
        viewModelScope.launch {
            _uiState.value = InviteResponseUiState.Loading
            inviteRepository.declineInvite(uid, invite.inviteId)
                .onSuccess { _uiState.value = InviteResponseUiState.Declined }
                .onFailure { error ->
                    _uiState.value = InviteResponseUiState.Error(error.message ?: "Could not decline invite")
                }
        }
    }
}
