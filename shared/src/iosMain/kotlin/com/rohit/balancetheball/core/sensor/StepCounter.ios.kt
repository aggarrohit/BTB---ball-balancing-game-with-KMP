package com.rohit.balancetheball.core.sensor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSDate

actual class StepCounter {
    @OptIn(ExperimentalForeignApi::class)
    actual fun isAvailable(): Boolean = CMPedometer.isStepCountingAvailable()

    @OptIn(ExperimentalForeignApi::class)
    actual fun steps(): Flow<Int> = callbackFlow {
        if (!CMPedometer.isStepCountingAvailable()) {
            close()
            return@callbackFlow
        }

        val pedometer = CMPedometer()
        pedometer.startPedometerUpdatesFromDate(NSDate()) { data, error ->
            if (error != null || data == null) return@startPedometerUpdatesFromDate
            trySend(data.numberOfSteps.intValue)
        }

        awaitClose { pedometer.stopPedometerUpdates() }
    }.conflate()
}
