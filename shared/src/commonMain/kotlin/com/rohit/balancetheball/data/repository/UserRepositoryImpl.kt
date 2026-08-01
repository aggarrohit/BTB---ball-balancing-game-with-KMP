package com.rohit.balancetheball.data.repository

import com.rohit.balancetheball.core.util.currentTimeMillis
import com.rohit.balancetheball.data.remote.FirebaseUserDataSource
import com.rohit.balancetheball.domain.model.User
import com.rohit.balancetheball.domain.repository.UserRepository

class UserRepositoryImpl(
    private val dataSource: FirebaseUserDataSource
) : UserRepository {

    override suspend fun createAccount(username: String): Result<User> = runCatching {
        val existing = dataSource.getUser(username)
        val now = currentTimeMillis()
        if (existing != null) {
            dataSource.updateLastLogin(username, now)
            existing.copy(lastLoginAt = now)
        } else {
            val user = User(username = username, createdAt = now, lastLoginAt = now)
            dataSource.createUser(user)
            user
        }
    }

    override suspend fun getUser(username: String): Result<User?> = runCatching {
        dataSource.getUser(username)
    }
}
