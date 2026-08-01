package com.rohit.balancetheball.core.sensor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlin.math.PI
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

actual class TiltSensor {
    @OptIn(ExperimentalForeignApi::class)
    actual fun readings(): Flow<TiltReading> = callbackFlow {
        val manager = CMMotionManager()

        if (!manager.isDeviceMotionAvailable()) {
            close()
            return@callbackFlow
        }

        manager.deviceMotionUpdateInterval = 1.0 / 60.0
        manager.startDeviceMotionUpdatesToQueue(NSOperationQueue.mainQueue) { motion, _ ->
            val attitude = motion?.attitude ?: return@startDeviceMotionUpdatesToQueue
            val pitchDegrees = (attitude.pitch * 180.0 / PI).toFloat()
            val rollDegrees = (attitude.roll * 180.0 / PI).toFloat()
            trySend(TiltReading(pitchDegrees, rollDegrees))
        }

        awaitClose { manager.stopDeviceMotionUpdates() }
    }.conflate()
}
