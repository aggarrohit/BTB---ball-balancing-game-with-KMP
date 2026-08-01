package com.rohit.balancetheball.presentation.auth

import androidx.compose.runtime.Composable

data class GoogleIdCredential(val idToken: String, val accessToken: String? = null)

/**
 * Returns a launcher for the platform's Google Sign-In flow. Calling the returned lambda
 * triggers the flow; [onResult] is invoked with either a credential or an error message —
 * never both null, never both non-null.
 */
@Composable
expect fun rememberGoogleSignIn(onResult: (credential: GoogleIdCredential?, error: String?) -> Unit): () -> Unit
