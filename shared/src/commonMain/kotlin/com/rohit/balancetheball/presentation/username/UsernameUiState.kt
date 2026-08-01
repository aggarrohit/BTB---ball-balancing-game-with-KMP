package com.rohit.balancetheball.presentation.username

/**
 * All possible states of the username-claim screen (shown after Google sign-in
 * for a uid that hasn't picked a username yet).
 */
sealed interface UsernameUiState {
    data object Idle : UsernameUiState
    data object Loading : UsernameUiState
    data class Success(val username: String) : UsernameUiState
    data class Error(val message: String) : UsernameUiState
}
