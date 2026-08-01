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
    private val sensor: Sensor? by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }

    actual fun isAvailable(): Boolean = sensor != null

    actual fun steps(): Flow<Int> = callbackFlow {
        val stepSensor = sensor
        if (stepSensor == null) {
            close()
            return@callbackFlow
        }

        var baseline: Float? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val cumulativeSinceBoot = event.values[0]
                val base = baseline ?: cumulativeSinceBoot.also { baseline = it }
                trySend((cumulativeSinceBoot - base).toInt())
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)

        awaitClose { sensorManager.unregisterListener(listener) }
    }.conflate()
}
