package com.rohit.balancetheball

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.messaging.FirebaseMessaging
import com.rohit.balancetheball.core.config.AppConfig
import com.rohit.balancetheball.core.push.PendingInvite
import com.rohit.balancetheball.core.push.PendingInviteHolder
import com.rohit.balancetheball.core.push.PushTokenRegistrar
import com.rohit.balancetheball.core.sensor.AndroidSensorContext
import com.rohit.balancetheball.data.auth.FirebaseAuthRepository

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
        registerCurrentPushToken()
        handleInviteIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleInviteIntent(intent)
    }

    /** onNewToken alone only fires on refresh/reinstall, not on every launch — this covers the rest. */
    private fun registerCurrentPushToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val uid = FirebaseAuthRepository().currentUser?.uid ?: return@addOnSuccessListener
            PushTokenRegistrar.register(uid, token)
        }
    }

    private fun handleInviteIntent(intent: Intent) {
        val inviteId = intent.getStringExtra("inviteId") ?: return
        val fromUid = intent.getStringExtra("fromUid") ?: return
        val fromUsername = intent.getStringExtra("fromUsername") ?: return
        val roomCode = intent.getStringExtra("roomCode") ?: return
        PendingInviteHolder.pending.value = PendingInvite(inviteId, fromUid, fromUsername, roomCode)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}