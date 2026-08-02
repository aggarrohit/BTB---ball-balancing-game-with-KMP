package com.rohit.balancetheball.core.push

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Set by native platform code (Android's FirebaseMessagingService-launched MainActivity, iOS's
 * UNUserNotificationCenter delegate) the moment an invite notification is tapped — cold start or
 * warm. A plain Kotlin object is directly callable from Swift, same technique as
 * GoogleSignInBridgeHolder, so no extra bridge interface is needed just for this.
 *
 * App.kt observes [pending] and routes to the Accept/Reject screen once a user is signed in,
 * then clears it.
 */
object PendingInviteHolder {
    val pending = MutableStateFlow<PendingInvite?>(null)
}
