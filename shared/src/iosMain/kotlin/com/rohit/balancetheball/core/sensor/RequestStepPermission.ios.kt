package com.rohit.balancetheball.core.sensor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun RequestStepPermission(onResult: (granted: Boolean) -> Unit) {
    LaunchedEffect(Unit) { onResult(true) }
}
