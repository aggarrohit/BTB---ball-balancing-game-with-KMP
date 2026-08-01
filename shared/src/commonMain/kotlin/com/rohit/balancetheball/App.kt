package com.rohit.balancetheball

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rohit.balancetheball.data.auth.FirebaseAuthRepository
import com.rohit.balancetheball.data.remote.FirebaseUserDataSource
import com.rohit.balancetheball.data.repository.UserRepositoryImpl
import com.rohit.balancetheball.domain.model.AuthUser
import com.rohit.balancetheball.domain.model.User
import com.rohit.balancetheball.domain.repository.AuthRepository
import com.rohit.balancetheball.domain.repository.UserRepository
import com.rohit.balancetheball.presentation.auth.SignInScreen
import com.rohit.balancetheball.presentation.game.GameScreen
import com.rohit.balancetheball.presentation.lobby.LobbyScreen
import com.rohit.balancetheball.presentation.username.UsernameScreen
import com.rohit.balancetheball.core.util.currentTimeMillis
import kotlinx.coroutines.launch

/**
 * navKey fields below exist only to give each *visit* to a re-enterable screen a distinct
 * viewModel() cache key. This app has no Navigation-Compose back stack (just this when block),
 * so every screen's viewModel {} would otherwise share one instance for the whole Activity
 * lifetime, keyed only by class name — meaning returning to a screen (e.g. Lobby, via Exit Game)
 * would hand back the same stale ViewModel from the previous visit instead of a fresh one.
 */
private sealed interface AppRoute {
    data object Loading : AppRoute
    data class SignedOut(val navKey: Long = currentTimeMillis()) : AppRoute
    data class NeedsUsername(val authUser: AuthUser) : AppRoute
    data class InLobby(val user: User, val navKey: Long = currentTimeMillis()) : AppRoute
    data class InRoom(val user: User, val roomCode: String) : AppRoute
}

@Composable
@Preview
fun App(
    authRepository: AuthRepository = remember { FirebaseAuthRepository() },
    userRepository: UserRepository = remember { UserRepositoryImpl(FirebaseUserDataSource()) }
) {
    MaterialTheme {
        var route by remember { mutableStateOf<AppRoute>(AppRoute.Loading) }
        val scope = rememberCoroutineScope()

        suspend fun resolveRoute(authUser: AuthUser) {
            val profile = userRepository.resolveProfile(authUser.uid).getOrNull()
            route = if (profile != null) AppRoute.InLobby(profile) else AppRoute.NeedsUsername(authUser)
        }

        // Firebase Auth persists sessions across cold starts, so a returning user skips SignInScreen entirely.
        LaunchedEffect(Unit) {
            val current = authRepository.currentUser
            if (current == null) route = AppRoute.SignedOut() else resolveRoute(current)
        }

        when (val currentRoute = route) {
            is AppRoute.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AppRoute.SignedOut -> {
                SignInScreen(
                    navKey = currentRoute.navKey,
                    onSignedIn = { authUser -> scope.launch { resolveRoute(authUser) } }
                )
            }
            is AppRoute.NeedsUsername -> {
                UsernameScreen(
                    authUser = currentRoute.authUser,
                    onUsernameClaimed = { user -> route = AppRoute.InLobby(user) }
                )
            }
            is AppRoute.InLobby -> {
                LobbyScreen(
                    user = currentRoute.user,
                    navKey = currentRoute.navKey,
                    onRoomStarted = { roomCode -> route = AppRoute.InRoom(currentRoute.user, roomCode) }
                )
            }
            is AppRoute.InRoom -> {
                GameScreen(
                    user = currentRoute.user,
                    roomCode = currentRoute.roomCode,
                    onExitGame = { route = AppRoute.InLobby(currentRoute.user) },
                    onLoggedOut = { route = AppRoute.SignedOut() }
                )
            }
        }
    }
}
