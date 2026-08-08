package com.rohit.balancetheball.presentation.invite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rohit.balancetheball.core.push.PendingInvite
import com.rohit.balancetheball.domain.model.User
import com.rohit.balancetheball.presentation.common.AnimatedButton
import com.rohit.balancetheball.presentation.common.AnimatedOutlinedButton
import com.rohit.balancetheball.presentation.common.AppBackground
import com.rohit.balancetheball.presentation.common.GlassCard
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun InviteResponseScreen(
    user: User,
    invite: PendingInvite,
    onJoined: (roomCode: String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: InviteResponseViewModel = koinViewModel { parametersOf(user.uid, user.username, invite) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is InviteResponseUiState.Joined -> onJoined(state.roomCode)
            is InviteResponseUiState.Declined -> onDismiss()
            else -> Unit
        }
    }

    AppBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "Game invite",
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${invite.fromUsername} invited you to play — Room ${invite.roomCode}",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val isLoading = uiState is InviteResponseUiState.Loading

                    AnimatedButton(
                        onClick = viewModel::onAccept,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Accept")
                        }
                    }

                    AnimatedOutlinedButton(
                        onClick = viewModel::onDecline,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("Decline")
                    }

                    val errorState = uiState
                    if (errorState is InviteResponseUiState.Error) {
                        Text(
                            text = errorState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
