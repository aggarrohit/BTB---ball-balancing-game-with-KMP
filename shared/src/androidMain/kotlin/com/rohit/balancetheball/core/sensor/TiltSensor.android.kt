package com.rohit.balancetheball.core.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

actual class TiltSensor {
    actual fun readings(): Flow<TiltReading> = callbackFlow {
        val sensorManager = AndroidSensorContext.appContext
            .getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (sensor == null) {
            close()
            return@callbackFlow
        }

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val pitchDegrees = Math.toDegrees(orientation[1].toDouble()).toFloat()
                val rollDegrees = Math.toDegrees(orientation[2].toDouble()).toFloat()
                trySend(TiltReading(pitchDegrees, rollDegrees))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)

        awaitClose { sensorManager.unregisterListener(listener) }
    }.conflate()
}
