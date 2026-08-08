import FirebaseCore
import FirebaseMessaging
import Shared
import UIKit
import UserNotifications

/// Wires up Firebase Cloud Messaging: registers for remote notifications, forwards the device
/// token to FCM, and — mirroring what BalanceFirebaseMessagingService does on Android — sets
/// PendingInviteHolder when an invite notification is tapped, cold start or warm.
final class AppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate, UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Starts Koin before anything below can possibly resolve a dependency through it — mirrors
        // Android's BalanceApplication.onCreate, which runs before its MainActivity for the same
        // reason. Named doInitKoin, not initKoin, on the Swift side: Kotlin's Objective-C exporter
        // prefixes any top-level function named init* with "do" to avoid colliding with Cocoa's
        // own init-prefixed initializer convention.
        KoinInitKt.doInitKoin()

        // Configure Firebase first — everything below (and IosAppConfigLoader/GoogleSignIn setup
        // that used to live in iOSApp.init()) depends on this having already run, and
        // @UIApplicationDelegateAdaptor doesn't guarantee this runs before the SwiftUI App's own
        // init(), so it's centralized here instead.
        FirebaseApp.configure()
        IosAppConfigLoader().load()
        GoogleSignInBridgeHolder.shared.bridge = SwiftGoogleSignInBridge()

        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self

        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in
            DispatchQueue.main.async {
                application.registerForRemoteNotifications()
            }
        }

        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken, let uid = FirebaseAuthRepository().currentUser?.uid else { return }
        PushTokenRegistrar.shared.register(uid: uid, token: token)
    }

    // Without this, a notification that arrives while the app is already open wouldn't show
    // anything — UNUserNotificationCenter's default is to stay silent in the foreground.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        handleInvitePayload(response.notification.request.content.userInfo)
        completionHandler()
    }

    private func handleInvitePayload(_ userInfo: [AnyHashable: Any]) {
        guard
            userInfo["type"] as? String == "game_invite",
            let inviteId = userInfo["inviteId"] as? String,
            let fromUid = userInfo["fromUid"] as? String,
            let fromUsername = userInfo["fromUsername"] as? String,
            let roomCode = userInfo["roomCode"] as? String
        else { return }

        PendingInviteHolder.shared.pending.value = PendingInvite(
            inviteId: inviteId,
            fromUid: fromUid,
            fromUsername: fromUsername,
            roomCode: roomCode
        )
    }
}
