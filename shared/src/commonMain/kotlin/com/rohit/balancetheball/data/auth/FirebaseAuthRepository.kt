package com.rohit.balancetheball.data.auth

import com.rohit.balancetheball.domain.model.AuthUser
import com.rohit.balancetheball.domain.repository.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseAuthRepository : AuthRepository {

    private val auth get() = Firebase.auth

    override val currentUser: AuthUser?
        get() = auth.currentUser?.toAuthUser()

    override val authState: Flow<AuthUser?> =
        auth.authStateChanged.map { it?.toAuthUser() }

    override suspend fun signInWithGoogle(idToken: String, accessToken: String?): Result<AuthUser> = runCatching {
        val credential = GoogleAuthProvider.credential(idToken, accessToken)
        val user = auth.signInWithCredential(credential).user
            ?: throw IllegalStateException("Google sign-in succeeded but returned no user")
        user.toAuthUser()
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(uid = uid, email = email, displayName = displayName)
}
