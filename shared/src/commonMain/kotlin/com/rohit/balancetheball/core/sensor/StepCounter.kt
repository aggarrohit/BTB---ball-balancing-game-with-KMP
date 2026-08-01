package com.rohit.balancetheball.core.sensor

import kotlinx.coroutines.flow.Flow

/**
 * Streams the number of steps taken since this flow was collected (session steps),
 * not the device's lifetime step count. Emits nothing further if the platform denies
 * permission or lacks step-counting hardware — callers should treat "no emissions" as
 * "steps unavailable" rather than an error.
 */
expect class StepCounter() {
    /** True if this device has the hardware/API to count steps at all (independent of permission). */
    fun isAvailable(): Boolean
    fun steps(): Flow<Int>
}
