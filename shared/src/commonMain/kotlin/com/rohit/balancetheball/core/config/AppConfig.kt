package com.rohit.balancetheball.core.config

/**
 * Holds runtime configuration values.
 * Must be initialized before Firebase is first accessed.
 * On Android: call AppConfig.init() from MainActivity.onCreate().
 * On iOS: call AppConfig.init() from iOSApp init.
 */
object AppConfig {
    var firebaseDatabaseUrl: String = ""
    var firebaseProjectId: String = ""

    /** Web OAuth client ID for Google Sign-In (Android only — iOS reads its own from GoogleService-Info.plist). */
    var googleWebClientId: String = ""

    fun init(databaseUrl: String, projectId: String, googleWebClientId: String = "") {
        firebaseDatabaseUrl = databaseUrl
        firebaseProjectId = projectId
        this.googleWebClientId = googleWebClientId
    }
}
