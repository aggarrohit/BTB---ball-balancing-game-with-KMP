package com.rohit.balancetheball.presentation.createaccount

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
import com.rohit.balancetheball.domain.usecase.CreateAccountUseCase

@Composable
fun CreateAccountScreen(
    onAccountCreated: (username: String) -> Unit,
    viewModel: CreateAccountViewModel = viewModel {
        // Manual dependency wiring — swap in a DI framework when needed
        val dataSource = FirebaseUserDataSource()
        val repository = UserRepositoryImpl(dataSource)
        val useCase = CreateAccountUseCase(repository)
        CreateAccountViewModel(useCase)
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is CreateAccountUiState.Success) {
            onAccountCreated((uiState as CreateAccountUiState.Success).username)
        }
    }

    CreateAccountContent(
        uiState = uiState,
        onCreateAccount = viewModel::onCreateAccount,
        onDismissError = viewModel::resetState
    )
}

@Composable
private fun CreateAccountContent(
    uiState: CreateAccountUiState,
    onCreateAccount: (String) -> Unit,
    onDismissError: () -> Unit
) {
    var username by remember { mutableStateOf("") }
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
                text = "Balance The Ball",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Create your player account",
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
                enabled = uiState !is CreateAccountUiState.Loading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboard?.hide()
                    onCreateAccount(username)
                }),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    keyboard?.hide()
                    onCreateAccount(username)
                },
                enabled = username.isNotBlank() && uiState !is CreateAccountUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState is CreateAccountUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Account")
                }
            }
        }
    }

    if (uiState is CreateAccountUiState.Error) {
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
