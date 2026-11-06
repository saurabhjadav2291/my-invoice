// The Kotlin Android plugin must see the Android Gradle Plugin's classes, so AGP
// goes on the root build classpath. Core-only builds (-PexcludeAndroid=true) skip
// it, because Google's Maven repository may not be reachable there.
buildscript {
    if (providers.gradleProperty("excludeAndroid").orNull != "true") {
        repositories {
            google()
            mavenCentral()
        }
        dependencies { classpath(libs.android.gradle.plugin) }
    }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.sqldelight) apply false
}
