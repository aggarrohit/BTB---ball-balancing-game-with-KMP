package com.rohit.balancetheball.domain.repository

import com.rohit.balancetheball.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Synchronous snapshot of the signed-in user, if any — for a startup check before collecting [authState]. */
    val currentUser: AuthUser?

    /** Emits the signed-in user (or null) immediately on collection, then again on every change. */
    val authState: Flow<AuthUser?>

    suspend fun signInWithGoogle(idToken: String, accessToken: String?): Result<AuthUser>

    suspend fun signOut()
}
