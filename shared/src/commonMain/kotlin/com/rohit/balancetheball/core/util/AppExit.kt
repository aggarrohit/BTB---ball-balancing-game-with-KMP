package com.rohit.balancetheball.core.util

/**
 * Terminates the app process. Android-only in practice — iOS apps aren't meant to self-terminate
 * (no supported API for it), so the iOS actual is a no-op. Only call this from an explicit,
 * user-confirmed "exit app" action.
 */
expect fun exitApp()
