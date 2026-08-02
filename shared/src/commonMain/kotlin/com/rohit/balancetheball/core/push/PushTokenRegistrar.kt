package com.rohit.balancetheball.core.push

import com.rohit.balancetheball.data.remote.FirebaseInviteDataSource
import com.rohit.balancetheball.data.repository.InviteRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fire-and-forget push token registration, callable directly from native platform code (Android's
 * MainActivity/FirebaseMessagingService, iOS's AppDelegate) without needing to bridge a suspend
 * function across the Kotlin/Swift boundary.
 */
object PushTokenRegistrar {
    fun register(uid: String, token: String) {
        // Dispatchers.IO isn't part of the public API on Kotlin/Native — Default is fine for a
        // single lightweight network write like this, on every platform.
        CoroutineScope(Dispatchers.Default).launch {
            InviteRepositoryImpl(FirebaseInviteDataSource()).registerPushToken(uid, token)
        }
    }
}
