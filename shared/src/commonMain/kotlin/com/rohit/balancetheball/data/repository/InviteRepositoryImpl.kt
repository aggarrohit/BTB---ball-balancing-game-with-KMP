package com.rohit.balancetheball.data.repository

import com.rohit.balancetheball.data.remote.FirebaseInviteDataSource
import com.rohit.balancetheball.domain.repository.InviteRepository

class InviteRepositoryImpl(
    private val dataSource: FirebaseInviteDataSource
) : InviteRepository {

    override suspend fun sendInvite(
        fromUid: String,
        fromUsername: String,
        toUid: String,
        roomCode: String
    ): Result<Unit> = runCatching {
        dataSource.sendInvite(fromUid, fromUsername, toUid, roomCode)
    }

    override suspend fun declineInvite(toUid: String, inviteId: String): Result<Unit> = runCatching {
        dataSource.declineInvite(toUid, inviteId)
    }

    override suspend fun registerPushToken(uid: String, token: String): Result<Unit> = runCatching {
        dataSource.registerPushToken(uid, token)
    }
}
