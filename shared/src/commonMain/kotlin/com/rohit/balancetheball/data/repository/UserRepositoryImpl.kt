package com.rohit.balancetheball.data.repository

import com.rohit.balancetheball.core.util.currentTimeMillis
import com.rohit.balancetheball.data.remote.FirebaseUserDataSource
import com.rohit.balancetheball.domain.model.User
import com.rohit.balancetheball.domain.repository.UserRepository

class UserRepositoryImpl(
    private val dataSource: FirebaseUserDataSource
) : UserRepository {

    override suspend fun resolveProfile(uid: String): Result<User?> = runCatching {
        val existing = dataSource.getUserByUid(uid) ?: return@runCatching null
        val now = currentTimeMillis()
        dataSource.updateLastLogin(uid, now)
        existing.copy(lastLoginAt = now)
    }

    override suspend fun claimUsername(uid: String, username: String, email: String?): Result<User> = runCatching {
        if (!dataSource.isUsernameAvailable(username)) {
            throw IllegalStateException("Username '$username' is already taken")
        }
        val now = currentTimeMillis()
        val user = User(uid = uid, username = username, email = email, createdAt = now, lastLoginAt = now)
        dataSource.claimUsername(user)
        user
    }
}
