import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
}

// Read local.properties for environment variables
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.androidx.core.ktx)
    debugImplementation(libs.compose.uiTooling)

    // Firebase Cloud Messaging — needs a real FirebaseMessagingService for background/tap
    // handling, so it's wired natively here rather than through the shared KMP Firebase SDK.
    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-messaging")
}

android {
    namespace = "com.rohit.balancetheball"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.rohit.balancetheball"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        // Inject env variables from local.properties into BuildConfig
        buildConfigField(
            "String",
            "FIREBASE_DATABASE_URL",
            "\"${localProps.getProperty("FIREBASE_DATABASE_URL", "")}\""
        )
        buildConfigField(
            "String",
            "FIREBASE_PROJECT_ID",
            "\"${localProps.getProperty("FIREBASE_PROJECT_ID", "")}\""
        )
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${localProps.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\""
        )
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}