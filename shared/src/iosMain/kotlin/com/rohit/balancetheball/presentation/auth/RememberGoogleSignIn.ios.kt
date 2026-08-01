package com.rohit.balancetheball.presentation.auth

import androidx.compose.runtime.Composable

@Composable
actual fun rememberGoogleSignIn(onResult: (credential: GoogleIdCredential?, error: String?) -> Unit): () -> Unit {
    return {
        val bridge = GoogleSignInBridgeHolder.bridge
        if (bridge == null) {
            onResult(null, "Google Sign-In not configured — add the GoogleSignIn-iOS package in Xcode")
        } else {
            bridge.signIn(
                onSuccess = { credential -> onResult(credential, null) },
                onFailure = { message -> onResult(null, message) }
            )
        }
    }
}
