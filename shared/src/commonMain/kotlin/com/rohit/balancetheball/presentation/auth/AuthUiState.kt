package com.rohit.balancetheball.presentation.auth

import com.rohit.balancetheball.domain.model.AuthUser

/**
 * All possible states of the Sign In screen.
 */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val authUser: AuthUser) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
