// KMP Calculator + Camera
// Author: Danish Hussain

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Build script for the :shared module — the Kotlin Multiplatform library that holds all
// the calculator/validation/MVI/camera logic. It compiles to an Android .aar AND an iOS
// .framework from the same source.

plugins {
    alias(libs.plugins.kotlinMultiplatform) // enables the `kotlin { }` multi-target block
    alias(libs.plugins.androidLibrary)      // lets this module also produce an Android library
}

kotlin {
    // --- Android target: plain JVM bytecode, Java 17. ---
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }

    // --- iOS targets: device (arm64), Intel simulator (x64), Apple-silicon simulator. ---
    // Each produces a static framework named "Shared" that `import Shared` pulls into Swift.
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"   // must match `import Shared` and project.rb's framework name
            isStatic = true       // linked into the app binary; no dynamic framework to embed
        }
    }

    // --- Source sets: which code compiles for which target, and its dependencies. ---
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core) // StateFlow / Channel, multiplatform
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)             // runs on JVM and Native
            implementation(libs.kotlinx.coroutines.test) // runTest, TestScope, test dispatchers
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android) // Dispatchers.Main backed by Looper
        }
        // iosMain has no extra deps — just IosCalculatorFactory.kt.
    }
}

// Android-library config for the parts of :shared that compile against the Android SDK.
android {
    namespace = "com.danish.calculator.shared"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
