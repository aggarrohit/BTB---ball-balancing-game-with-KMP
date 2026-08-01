package com.rohit.balancetheball.domain.model

/**
 * Domain model representing a player account.
 * Intentionally simple — only a username is required to create an account.
 */
data class User(
    val username: String,
    val createdAt: Long = 0L,
    val lastLoginAt: Long = 0L
)
