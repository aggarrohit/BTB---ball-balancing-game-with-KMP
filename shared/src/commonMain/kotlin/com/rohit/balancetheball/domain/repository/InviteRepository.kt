package com.rohit.balancetheball.domain.repository

interface InviteRepository {
    /** Writes a fresh pending invite into [toUid]'s inbox; a Cloud Function pushes the notification. */
    suspend fun sendInvite(fromUid: String, fromUsername: String, toUid: String, roomCode: String): Result<Unit>

    /** Marks an invite the caller received as declined. */
    suspend fun declineInvite(toUid: String, inviteId: String): Result<Unit>

    /** Registers/refreshes the caller's push token so invites can actually reach this device. */
    suspend fun registerPushToken(uid: String, token: String): Result<Unit>
}
