import SwiftUI
import FirebaseCore
import GoogleSignIn
import Shared

@main
struct iOSApp: App {
    init() {
        // Configure Firebase — requires GoogleService-Info.plist in the Xcode target
        FirebaseApp.configure()

        // Load Firebase config values into the shared KMP AppConfig
        IosAppConfigLoader().load()

        // Wire the Kotlin GoogleSignInBridge interface to this Swift implementation —
        // see GoogleSignInBridge.swift for why this indirection exists.
        GoogleSignInBridgeHolder.shared.bridge = SwiftGoogleSignInBridge()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}