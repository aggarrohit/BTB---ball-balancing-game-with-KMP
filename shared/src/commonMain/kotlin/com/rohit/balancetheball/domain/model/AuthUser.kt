package com.rohit.balancetheball.domain.model

/** The signed-in identity from Firebase Auth (Google Sign-In), before a game username is chosen. */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?
)
