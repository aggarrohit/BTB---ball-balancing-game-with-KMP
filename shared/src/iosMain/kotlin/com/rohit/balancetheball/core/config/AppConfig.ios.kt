package com.rohit.balancetheball.core.config

import platform.Foundation.NSBundle

// AppConfig is defined in commonMain as a plain object.
// Call AppConfig.init(...) from iOSApp.init to populate values from Info.plist.
object IosAppConfigLoader {
    fun load() {
        val bundle = NSBundle.mainBundle
        val dbUrl = bundle.objectForInfoDictionaryKey("FIREBASE_DATABASE_URL") as? String ?: ""
        val projectId = bundle.objectForInfoDictionaryKey("FIREBASE_PROJECT_ID") as? String ?: ""
        AppConfig.init(dbUrl, projectId)
    }
}
