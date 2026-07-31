import SwiftUI
import FirebaseCore
import Shared

@main
struct iOSApp: App {
    init() {
        // Configure Firebase — requires GoogleService-Info.plist in the Xcode target
        FirebaseApp.configure()

        // Load Firebase config values into the shared KMP AppConfig
        IosAppConfigLoader().load()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}