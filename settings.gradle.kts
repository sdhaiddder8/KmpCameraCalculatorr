// KMP Calculator + Camera
// Author: Danish Hussain

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "KmpCameraCalculator"

// Only two Gradle modules. iosApp is an Xcode project, not a Gradle module — it consumes
// :shared's framework via the "Compile Kotlin Framework" build phase (see iosApp/project.rb).
include(":shared")     // the KMP logic library
include(":androidApp") // the Android (Jetpack Compose) app, depends on :shared
