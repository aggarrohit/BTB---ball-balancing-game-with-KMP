package com.rohit.balancetheball.core.push

import com.rohit.balancetheball.domain.repository.InviteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Fire-and-forget push token registration, callable directly from native platform code (Android's
 * MainActivity/FirebaseMessagingService, iOS's AppDelegate) without needing to bridge a suspend
 * function across the Kotlin/Swift boundary. Not a Composable, so it resolves its dependency via
 * KoinComponent rather than koinInject() — Koin must already be started by then (see KoinInit.kt).
 */
object PushTokenRegistrar : KoinComponent {
    private val inviteRepository: InviteRepository by inject()

    fun register(uid: String, token: String) {
        // Dispatchers.IO isn't part of the public API on Kotlin/Native — Default is fine for a
        // single lightweight network write like this, on every platform.
        CoroutineScope(Dispatchers.Default).launch {
            inviteRepository.registerPushToken(uid, token)
        }
    }
}
