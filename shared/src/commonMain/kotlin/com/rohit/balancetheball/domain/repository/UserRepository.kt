package com.rohit.balancetheball.domain.repository

import com.rohit.balancetheball.domain.model.User

interface UserRepository {
    /**
     * Looks up the profile already claimed for [uid], bumping lastLoginAt as a side effect if found.
     * Null means this uid hasn't claimed a username yet.
     */
    suspend fun resolveProfile(uid: String): Result<User?>

    /** Claims [username] for [uid]. Fails if the username is already taken by someone else. */
    suspend fun claimUsername(uid: String, username: String, email: String?): Result<User>
}
