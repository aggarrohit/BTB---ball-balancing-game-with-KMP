package com.rohit.balancetheball.domain.model

/**
 * Domain model representing a player account.
 * Keyed by [uid] (the Firebase Auth UID) — [username] is a separately claimed, unique display name.
 */
data class User(
    val uid: String,
    val username: String,
    val email: String? = null,
    val createdAt: Long = 0L,
    val lastLoginAt: Long = 0L
)
