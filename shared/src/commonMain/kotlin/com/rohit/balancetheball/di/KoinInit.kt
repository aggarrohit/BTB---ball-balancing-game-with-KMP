package com.rohit.balancetheball.di

import org.koin.core.context.startKoin

/**
 * Starts the app's single Koin container. Must run before anything tries to resolve a dependency
 * — including a cold background entry point that never goes through a screen at all (Android's
 * push-notification service can be woken by FCM without MainActivity ever running first), so each
 * platform calls this from its earliest possible hook (Android: Application.onCreate; iOS: the
 * AppDelegate), not from a screen or Activity.
 */
fun initKoin() {
    startKoin {
        modules(dataModule, domainModule, sensorModule, viewModelModule)
    }
}
