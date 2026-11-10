import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Version comes from the root buildscript classpath.
    id("com.android.application")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.revishingstudio.invoice"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.myinvoice.app"
        minSdk = 26
        targetSdk = 36
        // Each GitHub build gets a higher number, so a new APK installs as an update.
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = "0.1.0"
    }

    // One permanent signing key, provided by CI from GitHub secrets. Android only
    // installs an update over the existing app when both are signed with the same
    // key. Without the key (e.g. local builds) the usual debug key is used.
    val keystorePath = System.getenv("SIGNING_KEYSTORE")
    val keystorePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
    val stableSigning = if (!keystorePath.isNullOrBlank() && !keystorePassword.isNullOrBlank()) {
        signingConfigs.create("stable") {
            storeFile = file(keystorePath)
            storePassword = keystorePassword
            keyAlias = "myinvoice"
            keyPassword = keystorePassword
        }
    } else {
        null
    }

    buildTypes {
        debug {
            stableSigning?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = false
            stableSigning?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.sqldelight.android.driver)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.core)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.haze)
    debugImplementation(libs.compose.ui.tooling)
}
