package com.rohit.balancetheball.presentation.username

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.rohit.balancetheball.domain.usecase.ClaimUsernameUseCase

class UsernameViewModel(
    private val claimUsernameUseCase: ClaimUsernameUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UsernameUiState>(UsernameUiState.Idle)
    val uiState: StateFlow<UsernameUiState> = _uiState.asStateFlow()

    fun onClaimUsername(uid: String, username: String, email: String?) {
        viewModelScope.launch {
            _uiState.value = UsernameUiState.Loading
            claimUsernameUseCase(uid, username, email)
                .onSuccess { user ->
                    _uiState.value = UsernameUiState.Success(user.username)
                }
                .onFailure { error ->
                    _uiState.value = UsernameUiState.Error(
                        error.message ?: "Something went wrong"
                    )
                }
        }
    }

    fun resetState() {
        _uiState.value = UsernameUiState.Idle
    }
}
