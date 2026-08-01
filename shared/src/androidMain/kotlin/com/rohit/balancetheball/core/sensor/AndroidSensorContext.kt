package com.rohit.balancetheball.core.sensor

import android.content.Context

/**
 * Holds the application Context so Android sensor actuals (SensorManager) can be
 * obtained without threading a Context through commonMain code.
 * Initialized once from MainActivity.onCreate, next to AppConfig.init.
 */
object AndroidSensorContext {
    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
