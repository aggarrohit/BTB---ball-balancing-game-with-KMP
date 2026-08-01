package com.rohit.balancetheball.presentation.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rohit.balancetheball.core.config.AppConfig
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
actual fun rememberGoogleSignIn(onResult: (credential: GoogleIdCredential?, error: String?) -> Unit): () -> Unit {
    // LocalContext here is the hosting Activity (set by ComponentActivity.setContent), not just
    // Application Context — Credential Manager needs an Activity to present its picker UI.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    return {
        scope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(AppConfig.googleWebClientId)
                    .setNonce(UUID.randomUUID().toString())
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val response = CredentialManager.create(context).getCredential(context, request)
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
                onResult(GoogleIdCredential(idToken = googleIdTokenCredential.idToken), null)
            } catch (e: Exception) {
                onResult(null, e.message ?: "Google sign-in failed")
            }
        }
    }
}
