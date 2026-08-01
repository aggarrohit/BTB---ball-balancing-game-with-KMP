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
import com.rohit.balancetheball.domain.repository.AuthRepository
import com.rohit.balancetheball.domain.repository.UserRepository
import com.rohit.balancetheball.presentation.auth.SignInScreen
import com.rohit.balancetheball.presentation.game.GameScreen
import com.rohit.balancetheball.presentation.username.UsernameScreen
import kotlinx.coroutines.launch

private sealed interface AppRoute {
    data object Loading : AppRoute
    data object SignedOut : AppRoute
    data class NeedsUsername(val authUser: AuthUser) : AppRoute
    data class SignedIn(val username: String) : AppRoute
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
            route = if (profile != null) AppRoute.SignedIn(profile.username) else AppRoute.NeedsUsername(authUser)
        }

        // Firebase Auth persists sessions across cold starts, so a returning user skips SignInScreen entirely.
        LaunchedEffect(Unit) {
            val current = authRepository.currentUser
            if (current == null) route = AppRoute.SignedOut else resolveRoute(current)
        }

        when (val currentRoute = route) {
            is AppRoute.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AppRoute.SignedOut -> {
                SignInScreen(
                    onSignedIn = { authUser -> scope.launch { resolveRoute(authUser) } }
                )
            }
            is AppRoute.NeedsUsername -> {
                UsernameScreen(
                    authUser = currentRoute.authUser,
                    onUsernameClaimed = { username -> route = AppRoute.SignedIn(username) }
                )
            }
            is AppRoute.SignedIn -> {
                GameScreen(username = currentRoute.username)
            }
        }
    }
}
