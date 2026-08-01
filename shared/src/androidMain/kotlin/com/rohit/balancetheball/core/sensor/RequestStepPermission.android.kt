package com.rohit.balancetheball.core.sensor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun RequestStepPermission(onResult: (granted: Boolean) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onResult(granted) }

    LaunchedEffect(Unit) {
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> onResult(true)
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED -> onResult(true)
            else -> launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }
}
