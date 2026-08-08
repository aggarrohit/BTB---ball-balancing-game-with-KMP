package com.rohit.balancetheball.di

import com.rohit.balancetheball.core.sensor.StepCounter
import com.rohit.balancetheball.core.sensor.TiltSensor
import org.koin.dsl.module

/** Fresh sensor wrapper per injection — GameViewModel owns one for exactly one game session. */
val sensorModule = module {
    factory { TiltSensor() }
    factory { StepCounter() }
}
