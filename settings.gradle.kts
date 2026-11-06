rootProject.name = "my-invoice"

// The Android app needs the Android SDK and Google's Maven repository.
// Pass -PexcludeAndroid=true to build and test only the shared core
// (useful on machines without the Android SDK or access to Google Maven).
val excludeAndroid = providers.gradleProperty("excludeAndroid").orNull == "true"

pluginManagement {
    val skipGoogle = providers.gradleProperty("excludeAndroid").orNull == "true"
    repositories {
        if (!skipGoogle) google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (!excludeAndroid) google()
        mavenCentral()
    }
}

include(":core")
if (!excludeAndroid) {
    include(":androidApp")
}
