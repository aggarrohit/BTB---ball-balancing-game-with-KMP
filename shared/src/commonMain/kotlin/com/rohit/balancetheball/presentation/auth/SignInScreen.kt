package com.rohit.balancetheball.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rohit.balancetheball.data.auth.FirebaseAuthRepository
import com.rohit.balancetheball.domain.model.AuthUser

@Composable
fun SignInScreen(
    onSignedIn: (AuthUser) -> Unit,
    viewModel: AuthViewModel = viewModel {
        // Manual dependency wiring — swap in a DI framework when needed
        AuthViewModel(FirebaseAuthRepository())
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val launchGoogleSignIn = rememberGoogleSignIn { credential, error ->
        viewModel.onGoogleCredential(credential, error)
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is AuthUiState.Success) {
            onSignedIn(state.authUser)
        }
    }

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
                text = "Sign in to play",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = launchGoogleSignIn,
                enabled = uiState !is AuthUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Sign in with Google")
                }
            }
        }
    }

    val state = uiState
    if (state is AuthUiState.Error) {
        AlertDialog(
            onDismissRequest = viewModel::resetState,
            title = { Text("Sign-in failed") },
            text = { Text(state.message) },
            confirmButton = {
                TextButton(onClick = viewModel::resetState) { Text("OK") }
            }
        )
    }
}
