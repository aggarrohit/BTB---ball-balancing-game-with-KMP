package com.rohit.balancetheball.presentation.createaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.rohit.balancetheball.domain.usecase.CreateAccountUseCase

class CreateAccountViewModel(
    private val createAccountUseCase: CreateAccountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateAccountUiState>(CreateAccountUiState.Idle)
    val uiState: StateFlow<CreateAccountUiState> = _uiState.asStateFlow()

    fun onCreateAccount(username: String) {
        viewModelScope.launch {
            _uiState.value = CreateAccountUiState.Loading
            createAccountUseCase(username)
                .onSuccess { user ->
                    _uiState.value = CreateAccountUiState.Success(user.username)
                }
                .onFailure { error ->
                    _uiState.value = CreateAccountUiState.Error(
                        error.message ?: "Something went wrong"
                    )
                }
        }
    }

    fun resetState() {
        _uiState.value = CreateAccountUiState.Idle
    }
}
