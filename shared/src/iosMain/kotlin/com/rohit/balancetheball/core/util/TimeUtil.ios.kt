package com.rohit.balancetheball.core.util

actual fun currentTimeMillis(): Long =
    kotlin.time.Clock.System.now().toEpochMilliseconds()
