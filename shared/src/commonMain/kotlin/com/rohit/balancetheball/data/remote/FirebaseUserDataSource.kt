package com.rohit.balancetheball.data.remote

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.flow.first
import com.rohit.balancetheball.core.config.AppConfig
import com.rohit.balancetheball.domain.model.User

/**
 * Reads and writes user data to Firebase Realtime Database.
 *
 * Database schema:
 *   /users
 *     /{uid}
 *       uid: String
 *       username: String
 *       email: String?
 *       createdAt: Long (Unix timestamp ms)
 *       lastLoginAt: Long (Unix timestamp ms)
 *   /usernames
 *     /{username}: <uid>   -- secondary index enforcing username uniqueness
 */
class FirebaseUserDataSource {

    private val db by lazy {
        val url = AppConfig.firebaseDatabaseUrl
        if (url.isBlank()) Firebase.database
        else Firebase.database(url)
    }

    private fun userRef(uid: String) = db.reference("users/$uid")
    private fun usernameRef(username: String) = db.reference("usernames/$username")

    /** Returns the user profile for [uid], or null if they haven't claimed a username yet. */
    suspend fun getUserByUid(uid: String): User? {
        val snapshot = userRef(uid).valueEvents.first()
        @Suppress("UNCHECKED_CAST")
        val map = snapshot.value as? Map<String, Any> ?: return null
        return User(
            uid = map["uid"] as? String ?: uid,
            username = map["username"] as? String ?: return null,
            email = map["email"] as? String,
            createdAt = map["createdAt"] as? Long ?: 0L,
            lastLoginAt = map["lastLoginAt"] as? Long ?: 0L
        )
    }

    /** True if no one has claimed [username] yet. */
    suspend fun isUsernameAvailable(username: String): Boolean {
        val snapshot = usernameRef(username).valueEvents.first()
        return snapshot.value == null
    }

    /**
     * Atomically writes the user's profile and the username index entry together,
     * so the two paths never end up inconsistent from a partial failure.
     * Caller must have already checked [isUsernameAvailable].
     */
    suspend fun claimUsername(user: User) {
        db.reference().updateChildren(
            mapOf(
                "users/${user.uid}/uid" to user.uid,
                "users/${user.uid}/username" to user.username,
                "users/${user.uid}/email" to user.email,
                "users/${user.uid}/createdAt" to user.createdAt,
                "users/${user.uid}/lastLoginAt" to user.lastLoginAt,
                "usernames/${user.username}" to user.uid
            )
        )
    }

    /** Updates only the lastLoginAt field of an existing user record. */
    suspend fun updateLastLogin(uid: String, timestamp: Long) {
        userRef(uid).child("lastLoginAt").setValue(timestamp)
    }
}
