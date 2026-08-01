package com.rohit.balancetheball.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohit.balancetheball.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onGoogleCredential(credential: GoogleIdCredential?, error: String?) {
        if (credential == null) {
            _uiState.value = AuthUiState.Error(error ?: "Google sign-in failed")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signInWithGoogle(credential.idToken, credential.accessToken)
                .onSuccess { authUser -> _uiState.value = AuthUiState.Success(authUser) }
                .onFailure { error2 ->
                    _uiState.value = AuthUiState.Error(error2.message ?: "Sign-in failed")
                }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
