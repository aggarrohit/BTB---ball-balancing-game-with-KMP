package com.rohit.balancetheball.data.remote

import com.rohit.balancetheball.core.config.AppConfig
import com.rohit.balancetheball.core.util.currentTimeMillis
import com.rohit.balancetheball.domain.model.InviteStatus
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import kotlin.random.Random

/**
 * Writes game invites to Firebase Realtime Database and registers the push token needed to
 * actually deliver them. The notification itself is sent by a Cloud Function (see functions/index.js)
 * triggered on invite creation — a client can't safely call FCM's send API directly.
 *
 * Database schema:
 *   /invites/{toUid}/{inviteId}: fromUid, fromUsername, roomCode, status ("pending"|"declined"), createdAt
 *   /users/{uid}/fcmTokens/{token}: true
 */
class FirebaseInviteDataSource {

    private val db by lazy {
        val url = AppConfig.firebaseDatabaseUrl
        if (url.isBlank()) Firebase.database
        else Firebase.database(url)
    }

    suspend fun sendInvite(fromUid: String, fromUsername: String, toUid: String, roomCode: String) {
        val inviteId = "$fromUid-${currentTimeMillis()}-${Random.nextInt(1000, 9999)}"
        db.reference("invites/$toUid/$inviteId").setValue(
            mapOf(
                "fromUid" to fromUid,
                "fromUsername" to fromUsername,
                "roomCode" to roomCode,
                "status" to InviteStatus.PENDING.toWireValue(),
                "createdAt" to currentTimeMillis()
            )
        )
    }

    suspend fun declineInvite(toUid: String, inviteId: String) {
        db.reference("invites/$toUid/$inviteId/status").setValue(InviteStatus.DECLINED.toWireValue())
    }

    suspend fun registerPushToken(uid: String, token: String) {
        db.reference("users/$uid/fcmTokens/$token").setValue(true)
    }
}
