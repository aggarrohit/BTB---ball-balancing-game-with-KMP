package com.rohit.balancetheball.core.sensor

import androidx.compose.runtime.Composable

/**
 * Requests whatever platform permission is needed for [StepCounter] to work.
 *
 * On Android, [onResult] reflects the actual runtime permission grant.
 * On iOS, [onResult] is invoked with true immediately — CoreMotion shows its own
 * system prompt automatically on first use, so this only means "proceed," not "granted."
 * Either way, a denial should be treated as graceful degradation (steps unavailable),
 * not a blocking error.
 */
@Composable
expect fun RequestStepPermission(onResult: (granted: Boolean) -> Unit)
