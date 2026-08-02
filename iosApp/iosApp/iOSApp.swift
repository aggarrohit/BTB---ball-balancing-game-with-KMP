import SwiftUI
import GoogleSignIn
import Shared

@main
struct iOSApp: App {
    // All Firebase/config/sign-in setup that used to live here now lives in
    // AppDelegate.application(_:didFinishLaunchingWithOptions:) — see AppDelegate.swift for why.
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}