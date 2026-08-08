package com.rohit.balancetheball

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.messaging.FirebaseMessaging
import com.rohit.balancetheball.core.push.PendingInvite
import com.rohit.balancetheball.core.push.PendingInviteHolder
import com.rohit.balancetheball.core.push.PushTokenRegistrar
import com.rohit.balancetheball.domain.repository.AuthRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {
    private val authRepository: AuthRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

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
            val uid = authRepository.currentUser?.uid ?: return@addOnSuccessListener
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