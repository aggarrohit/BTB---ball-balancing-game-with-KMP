package com.rohit.balancetheball.core.sensor

import kotlinx.coroutines.flow.Flow

/** A single tilt reading, in degrees. Positive/negative sign and magnitude are platform-consistent. */
data class TiltReading(
    val pitchDegrees: Float,
    val rollDegrees: Float
)

/** Streams device tilt (pitch/roll in degrees) derived from the platform's attitude/orientation sensor. */
expect class TiltSensor() {
    fun readings(): Flow<TiltReading>
}
