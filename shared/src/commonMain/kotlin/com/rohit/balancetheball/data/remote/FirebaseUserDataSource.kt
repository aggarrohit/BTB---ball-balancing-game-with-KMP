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
 *     /{username}
 *       username: String
 *       createdAt: Long (Unix timestamp ms)
 */
class FirebaseUserDataSource {

    private val db by lazy {
        val url = AppConfig.firebaseDatabaseUrl
        if (url.isBlank()) Firebase.database
        else Firebase.database(url)
    }

    private fun userRef(username: String) = db.reference("users/$username")

    /**
     * Returns the user with [username] from the DB, or null if not found.
     * Uses valueEvents Flow and takes the first emission.
     */
    suspend fun getUser(username: String): User? {
        val snapshot = userRef(username).valueEvents.first()
        @Suppress("UNCHECKED_CAST")
        val map = snapshot.value as? Map<String, Any> ?: return null
        return User(
            username = map["username"] as? String ?: username,
            createdAt = map["createdAt"] as? Long ?: 0L
        )
    }

    /**
     * Writes a new user record under /users/{username}.
     * Caller must ensure the username is not already taken.
     */
    suspend fun createUser(user: User) {
        userRef(user.username).setValue(
            mapOf(
                "username" to user.username,
                "createdAt" to user.createdAt
            )
        )
    }
}
