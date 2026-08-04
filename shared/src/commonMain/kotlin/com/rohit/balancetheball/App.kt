package com.rohit.balancetheball

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rohit.balancetheball.core.push.PendingInvite
import com.rohit.balancetheball.core.push.PendingInviteHolder
import com.rohit.balancetheball.core.theme.AppTheme
import com.rohit.balancetheball.core.theme.ThemePreferences
import com.rohit.balancetheball.data.auth.FirebaseAuthRepository
import com.rohit.balancetheball.data.remote.FirebaseUserDataSource
import com.rohit.balancetheball.data.repository.UserRepositoryImpl
import com.rohit.balancetheball.domain.model.AuthUser
import com.rohit.balancetheball.domain.model.User
import com.rohit.balancetheball.domain.repository.AuthRepository
import com.rohit.balancetheball.domain.repository.UserRepository
import com.rohit.balancetheball.presentation.auth.SignInScreen
import com.rohit.balancetheball.presentation.common.ThemeMenu
import com.rohit.balancetheball.presentation.game.GameScreen
import com.rohit.balancetheball.presentation.history.HistoryScreen
import com.rohit.balancetheball.presentation.invite.InviteResponseScreen
import com.rohit.balancetheball.presentation.lobby.LobbyScreen
import com.rohit.balancetheball.presentation.username.UsernameScreen
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// Type-safe Navigation-Compose routes. Kept as plain data — screens still receive a real User/
// AuthUser/PendingInvite reconstructed from these primitives at the call site below, so no
// screen's own signature needs to change. Routes intentionally carry only IDs (uid/username),
// not whole serialized domain objects, per Navigation's own type-safety guidance.
@Serializable private object LoadingRoute
@Serializable private object SignInRoute
@Serializable private data class NeedsUsernameRoute(val authUid: String, val authEmail: String?, val authDisplayName: String?)
@Serializable private data class LobbyRoute(val uid: String, val username: String, val pendingRoomCode: String? = null)
@Serializable private data class GameRoute(val uid: String, val username: String, val roomCode: String)
@Serializable private data class HistoryRoute(val uid: String, val username: String)
@Serializable private data class InviteResponseRoute(
    val uid: String,
    val username: String,
    val inviteId: String,
    val fromUid: String,
    val fromUsername: String,
    val roomCode: String
)

@Composable
@Preview
fun App(
    authRepository: AuthRepository = remember { FirebaseAuthRepository() },
    userRepository: UserRepository = remember { UserRepositoryImpl(FirebaseUserDataSource()) }
) {
    var themeMode by remember { mutableStateOf(ThemePreferences.getThemeMode()) }

    AppTheme(themeMode) {
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()
        var currentUser by remember { mutableStateOf<User?>(null) }

        // Every transition in this app fully replaces the visible screen (there's no drill-down
        // navigation to preserve) — clearing the whole back stack on each hop is what makes real
        // Navigation-Compose ViewModel scoping actually kick in (the old entry, and its
        // ViewModel, gets torn down when popped) instead of just adding unused history depth.
        // Popping up to the *graph's own* id (not the start destination's) is what actually
        // guarantees this clears everything: the start destination (LoadingRoute) only exists on
        // the back stack for the very first hop, so anchoring to it stops working correctly the
        // moment it's gone — the graph itself is always present regardless of current stack contents.
        fun goTo(route: Any) {
            navController.navigate(route) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }

        suspend fun resolveRoute(authUser: AuthUser) {
            val profile = userRepository.resolveProfile(authUser.uid).getOrNull()
            if (profile != null) {
                currentUser = profile
                goTo(LobbyRoute(profile.uid, profile.username))
            } else {
                goTo(NeedsUsernameRoute(authUser.uid, authUser.email, authUser.displayName))
            }
        }

        // Firebase Auth persists sessions across cold starts, so a returning user skips SignInScreen entirely.
        LaunchedEffect(Unit) {
            val current = authRepository.currentUser
            if (current == null) goTo(SignInRoute) else resolveRoute(current)
        }

        // Handles both a cold start (app launched by tapping an invite notification, waiting for
        // sign-in to resolve) and a warm one (invite arrives while already signed in) — reacts to
        // either the invite or the signed-in user changing, and only consumes it once both exist.
        LaunchedEffect(Unit) {
            combine(snapshotFlow { currentUser }, PendingInviteHolder.pending) { user, invite -> user to invite }
                .collect { (user, invite) ->
                    if (user == null || invite == null) return@collect
                    PendingInviteHolder.pending.value = null
                    goTo(
                        InviteResponseRoute(
                            uid = user.uid,
                            username = user.username,
                            inviteId = invite.inviteId,
                            fromUid = invite.fromUid,
                            fromUsername = invite.fromUsername,
                            roomCode = invite.roomCode
                        )
                    )
                }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = LoadingRoute) {
                composable<LoadingRoute> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                composable<SignInRoute> {
                    SignInScreen(onSignedIn = { authUser -> scope.launch { resolveRoute(authUser) } })
                }
                composable<NeedsUsernameRoute> { entry ->
                    val route = entry.toRoute<NeedsUsernameRoute>()
                    UsernameScreen(
                        authUser = AuthUser(route.authUid, route.authEmail, route.authDisplayName),
                        onUsernameClaimed = { user ->
                            currentUser = user
                            goTo(LobbyRoute(user.uid, user.username))
                        }
                    )
                }
                composable<LobbyRoute> { entry ->
                    val route = entry.toRoute<LobbyRoute>()
                    LobbyScreen(
                        user = User(uid = route.uid, username = route.username),
                        initialRoomCode = route.pendingRoomCode,
                        onRoomStarted = { roomCode -> goTo(GameRoute(route.uid, route.username, roomCode)) },
                        onLoggedOut = {
                            currentUser = null
                            goTo(SignInRoute)
                        },
                        onHistoryClick = { goTo(HistoryRoute(route.uid, route.username)) }
                    )
                }
                composable<GameRoute> { entry ->
                    val route = entry.toRoute<GameRoute>()
                    GameScreen(
                        user = User(uid = route.uid, username = route.username),
                        roomCode = route.roomCode,
                        onExitGame = { goTo(LobbyRoute(route.uid, route.username)) }
                    )
                }
                composable<HistoryRoute> { entry ->
                    val route = entry.toRoute<HistoryRoute>()
                    HistoryScreen(
                        user = User(uid = route.uid, username = route.username),
                        onBack = { goTo(LobbyRoute(route.uid, route.username)) },
                        onInviteSent = { roomCode ->
                            goTo(LobbyRoute(route.uid, route.username, pendingRoomCode = roomCode))
                        }
                    )
                }
                composable<InviteResponseRoute> { entry ->
                    val route = entry.toRoute<InviteResponseRoute>()
                    InviteResponseScreen(
                        user = User(uid = route.uid, username = route.username),
                        invite = PendingInvite(
                            inviteId = route.inviteId,
                            fromUid = route.fromUid,
                            fromUsername = route.fromUsername,
                            roomCode = route.roomCode
                        ),
                        onJoined = { roomCode -> goTo(GameRoute(route.uid, route.username, roomCode)) },
                        onDismiss = { goTo(LobbyRoute(route.uid, route.username)) }
                    )
                }
            }

            ThemeMenu(
                currentMode = themeMode,
                onModeSelected = { mode ->
                    themeMode = mode
                    ThemePreferences.setThemeMode(mode)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(8.dp)
            )
        }
    }
}
