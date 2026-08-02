package com.rohit.balancetheball.core.push

import androidx.compose.runtime.Composable

/**
 * Requests whatever platform permission is needed to show invite notifications.
 *
 * On Android, [onResult] reflects the actual runtime POST_NOTIFICATIONS grant (API 33+; true
 * immediately below that, where no runtime prompt exists).
 * On iOS, [onResult] is invoked with true immediately — the system authorization prompt is
 * requested natively from AppDelegate at launch instead, since it's tied to APNs registration.
 * Either way, a denial should be treated as graceful degradation, not a blocking error.
 */
@Composable
expect fun RequestNotificationPermission(onResult: (granted: Boolean) -> Unit)
