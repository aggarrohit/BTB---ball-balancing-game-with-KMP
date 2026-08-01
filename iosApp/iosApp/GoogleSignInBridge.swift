import FirebaseCore
import GoogleSignIn
import Shared
import UIKit

/// Implements the Kotlin `GoogleSignInBridge` interface (see shared/src/iosMain/.../presentation/auth/GoogleSignInBridge.kt).
/// GoogleSignIn-iOS isn't cinterop'd into the KMP `shared` framework (no CocoaPods in this project),
/// so the actual sign-in flow is driven from Swift and the resulting token handed back across the
/// Kotlin/Swift boundary via plain closures.
final class SwiftGoogleSignInBridge: GoogleSignInBridge {

    func signIn(onSuccess: @escaping (GoogleIdCredential) -> Void, onFailure: @escaping (String) -> Void) {
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            onFailure("Firebase is not configured")
            return
        }
        guard let presentingViewController = topViewController() else {
            onFailure("No presenting view controller available")
            return
        }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController) { result, error in
            if let error = error {
                onFailure(error.localizedDescription)
                return
            }
            guard let user = result?.user, let idToken = user.idToken?.tokenString else {
                onFailure("Google sign-in returned no ID token")
                return
            }
            onSuccess(GoogleIdCredential(idToken: idToken, accessToken: user.accessToken.tokenString))
        }
    }

    private func topViewController() -> UIViewController? {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootViewController = windowScene.windows.first?.rootViewController else {
            return nil
        }
        return rootViewController
    }
}
