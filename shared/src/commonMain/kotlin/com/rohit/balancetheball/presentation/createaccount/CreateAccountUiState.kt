package com.rohit.balancetheball.presentation.createaccount

/**
 * All possible states of the Create Account screen.
 */
sealed interface CreateAccountUiState {
    data object Idle : CreateAccountUiState
    data object Loading : CreateAccountUiState
    data class Success(val username: String) : CreateAccountUiState
    data class Error(val message: String) : CreateAccountUiState
}
