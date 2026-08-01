package com.rohit.balancetheball.presentation.auth

/**
 * Implemented in Swift (see iosApp/iosApp/GoogleSignInBridge.swift) using the GoogleSignIn-iOS SDK,
 * which isn't cinterop'd into this Kotlin/Native framework (no CocoaPods setup in this project).
 * Two plain callbacks instead of a single Result-typed one: kotlin.Result doesn't bridge cleanly
 * to Swift across the generated Objective-C framework header.
 */
interface GoogleSignInBridge {
    fun signIn(onSuccess: (GoogleIdCredential) -> Unit, onFailure: (message: String) -> Unit)
}

/** Set once from iOSApp.swift's init(), after FirebaseApp.configure(). */
object GoogleSignInBridgeHolder {
    var bridge: GoogleSignInBridge? = null
}
