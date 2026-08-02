package com.rohit.balancetheball.core.util

actual fun exitApp() {
    android.os.Process.killProcess(android.os.Process.myPid())
}
