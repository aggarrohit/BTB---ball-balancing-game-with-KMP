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
        if (existing != null) {
            throw IllegalStateException("Username '$username' is already taken")
        }
        val now = currentTimeMillis()
        val user = User(username = username, createdAt = now)
        dataSource.createUser(user)
        user
    }

    override suspend fun getUser(username: String): Result<User?> = runCatching {
        dataSource.getUser(username)
    }
}
