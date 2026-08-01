package com.rohit.balancetheball.presentation.username

import com.rohit.balancetheball.domain.model.User

/**
 * All possible states of the username-claim screen (shown after Google sign-in
 * for a uid that hasn't picked a username yet).
 */
sealed interface UsernameUiState {
    data object Idle : UsernameUiState
    data object Loading : UsernameUiState
    data class Success(val user: User) : UsernameUiState
    data class Error(val message: String) : UsernameUiState
}
