package com.rohit.balancetheball.core.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

actual class StepCounter {
    private val sensorManager: SensorManager by lazy {
        AndroidSensorContext.appContext
            .getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
    }

    // TYPE_STEP_DETECTOR fires one low-latency event per step, unlike TYPE_STEP_COUNTER, which
    // many OEMs batch (delivering several steps at once, sometimes seconds late) to save power.
    private val sensor: Sensor? by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) }

    actual fun isAvailable(): Boolean = sensor != null

    actual fun steps(): Flow<Int> = callbackFlow {
        val stepSensor = sensor
        if (stepSensor == null) {
            close()
            return@callbackFlow
        }

        var stepCount = 0

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Each event is a single detected step (event.values[0] is always 1.0).
                stepCount++
                trySend(stepCount)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)

        awaitClose { sensorManager.unregisterListener(listener) }
    }.conflate()
}
