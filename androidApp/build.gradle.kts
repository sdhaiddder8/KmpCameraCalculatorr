// KMP Calculator + Camera
// Author: Danish Hussain

// Build script for the Android app. This module is UI only — it depends on :shared for
// everything else.
plugins {
    alias(libs.plugins.androidApplication) // installable APK
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)    // required for Jetpack Compose with Kotlin 2.x
}

android {
    namespace = "com.danish.calculator.android"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.danish.calculator.android"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":shared")) // the KMP logic library — the whole point of the app

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)             // read photo orientation in PhotoPreview
    implementation(libs.androidx.activity.compose)          // setContent, ActivityResult launchers
    implementation(libs.androidx.lifecycle.runtime.compose) // collectAsStateWithLifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose) // viewModel() in composition

    // Compose BOM pins all the Compose artifact versions together.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)            // @Preview support, debug builds only
}
