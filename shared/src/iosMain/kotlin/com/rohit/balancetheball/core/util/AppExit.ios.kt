package com.rohit.balancetheball.core.util

actual fun exitApp() {
    // No-op — Apple disallows apps self-terminating, and iOS has no hardware back button
    // in the first place, so this path isn't reachable in practice.
}
