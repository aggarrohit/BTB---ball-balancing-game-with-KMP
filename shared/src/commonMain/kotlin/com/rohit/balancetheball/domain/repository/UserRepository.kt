package com.rohit.balancetheball.domain.repository

import com.rohit.balancetheball.domain.model.User

interface UserRepository {
    /** Returns existing user or creates a new one if the username is not taken. */
    suspend fun createAccount(username: String): Result<User>

    /** Returns null if no user with the given username exists. */
    suspend fun getUser(username: String): Result<User?>
}
