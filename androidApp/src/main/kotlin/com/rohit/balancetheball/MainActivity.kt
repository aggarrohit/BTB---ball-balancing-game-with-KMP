package com.rohit.balancetheball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rohit.balancetheball.core.config.AppConfig
import com.rohit.balancetheball.core.sensor.AndroidSensorContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize config from BuildConfig (values injected from local.properties)
        AppConfig.init(
            databaseUrl = BuildConfig.FIREBASE_DATABASE_URL,
            projectId = BuildConfig.FIREBASE_PROJECT_ID,
            googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        )
        AndroidSensorContext.init(applicationContext)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}