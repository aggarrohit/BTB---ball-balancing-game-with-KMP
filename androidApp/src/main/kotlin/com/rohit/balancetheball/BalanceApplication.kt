package com.rohit.balancetheball

import android.app.Application
import com.rohit.balancetheball.core.config.AppConfig
import com.rohit.balancetheball.core.sensor.AndroidSensorContext
import com.rohit.balancetheball.di.initKoin

/**
 * Application.onCreate runs before any Activity or Service in this process, unlike MainActivity's
 * onCreate — the guarantee this app actually needs, since BalanceFirebaseMessagingService can be
 * woken directly by FCM with MainActivity never having run at all.
 */
class BalanceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppConfig.init(
            databaseUrl = BuildConfig.FIREBASE_DATABASE_URL,
            projectId = BuildConfig.FIREBASE_PROJECT_ID,
            googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        )
        AndroidSensorContext.init(applicationContext)
        initKoin()
    }
}
