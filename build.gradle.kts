// KMP Calculator + Camera
// Author: Danish Hussain

// Root build script. `apply false` means: make each plugin's version available to the
// sub-modules, but don't apply any of them to the root project itself (it has no code).
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.composeCompiler) apply false
}
