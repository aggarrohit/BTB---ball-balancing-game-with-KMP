package com.rohit.balancetheball.presentation.username

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rohit.balancetheball.data.remote.FirebaseUserDataSource
import com.rohit.balancetheball.data.repository.UserRepositoryImpl
import com.rohit.balancetheball.domain.model.AuthUser
import com.rohit.balancetheball.domain.usecase.ClaimUsernameUseCase

private fun suggestUsername(displayName: String?): String =
    displayName.orEmpty().filter { it.isLetterOrDigit() || it == '_' }.take(20)

@Composable
fun UsernameScreen(
    authUser: AuthUser,
    onUsernameClaimed: (username: String) -> Unit,
    viewModel: UsernameViewModel = viewModel {
        // Manual dependency wiring — swap in a DI framework when needed
        val dataSource = FirebaseUserDataSource()
        val repository = UserRepositoryImpl(dataSource)
        val useCase = ClaimUsernameUseCase(repository)
        UsernameViewModel(useCase)
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is UsernameUiState.Success) {
            onUsernameClaimed(state.username)
        }
    }

    UsernameContent(
        initialUsername = suggestUsername(authUser.displayName),
        uiState = uiState,
        onClaimUsername = { username -> viewModel.onClaimUsername(authUser.uid, username, authUser.email) },
        onDismissError = viewModel::resetState
    )
}

@Composable
private fun UsernameContent(
    initialUsername: String,
    uiState: UsernameUiState,
    onClaimUsername: (String) -> Unit,
    onDismissError: () -> Unit
) {
    var username by remember { mutableStateOf(initialUsername) }
    val keyboard = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Pick a username",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )

            Text(
                text = "This is how other players will see you",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                placeholder = { Text("e.g. BallMaster99") },
                singleLine = true,
                enabled = uiState !is UsernameUiState.Loading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboard?.hide()
                    onClaimUsername(username)
                }),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    keyboard?.hide()
                    onClaimUsername(username)
                },
                enabled = username.isNotBlank() && uiState !is UsernameUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState is UsernameUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Continue")
                }
            }
        }
    }

    if (uiState is UsernameUiState.Error) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("Oops!") },
            text = { Text(uiState.message) },
            confirmButton = {
                TextButton(onClick = onDismissError) { Text("OK") }
            }
        )
    }
}
