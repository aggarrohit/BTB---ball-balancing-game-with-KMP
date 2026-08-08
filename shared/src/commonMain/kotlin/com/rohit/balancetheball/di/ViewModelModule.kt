package com.rohit.balancetheball.di

import com.rohit.balancetheball.core.push.PendingInvite
import com.rohit.balancetheball.presentation.auth.AuthViewModel
import com.rohit.balancetheball.presentation.game.GameViewModel
import com.rohit.balancetheball.presentation.history.HistoryViewModel
import com.rohit.balancetheball.presentation.invite.InviteResponseViewModel
import com.rohit.balancetheball.presentation.lobby.LobbyViewModel
import com.rohit.balancetheball.presentation.username.UsernameViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * ViewModels with no runtime arguments resolve entirely from the graph. The rest (Lobby, Game,
 * History, InviteResponse) additionally need per-navigation values — a room code, a uid — that
 * only exist once a screen is actually reached; those come in via Koin's `parametersOf` at the
 * call site (see each screen's `koinViewModel { parametersOf(...) }`) and are destructured here.
 */
val viewModelModule = module {
    viewModel { AuthViewModel(get()) }
    viewModel { UsernameViewModel(get()) }

    viewModel { (uid: String, username: String, initialRoomCode: String?) ->
        LobbyViewModel(
            createRoomUseCase = get(),
            joinRoomUseCase = get(),
            roomRepository = get(),
            uid = uid,
            username = username,
            initialRoomCode = initialRoomCode
        )
    }

    viewModel { (roomCode: String, uid: String) ->
        GameViewModel(
            roomCode = roomCode,
            uid = uid,
            tiltSensor = get(),
            stepCounter = get(),
            roomRepository = get(),
            historyRepository = get()
        )
    }

    viewModel { (uid: String, username: String) ->
        HistoryViewModel(
            uid = uid,
            username = username,
            historyRepository = get(),
            createRoomUseCase = get(),
            inviteRepository = get()
        )
    }

    viewModel { (uid: String, username: String, invite: PendingInvite) ->
        InviteResponseViewModel(
            uid = uid,
            username = username,
            invite = invite,
            joinRoomUseCase = get(),
            inviteRepository = get()
        )
    }
}
