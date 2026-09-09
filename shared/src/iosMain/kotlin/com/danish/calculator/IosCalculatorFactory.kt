// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator

import com.danish.calculator.camera.CameraController
import com.danish.calculator.mvi.CalculatorComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * iOS-only factory. This is the entire contents of the `iosMain` source set.
 *
 * Why it exists: building a `CoroutineScope(SupervisorJob() + Dispatchers.Main)` is clumsy
 * to express from Swift, so we do it here and hand Swift a ready-to-use
 * [CalculatorComponent]. Kotlin/Native exports this top-level function to Swift as
 * `IosCalculatorFactoryKt.createCalculatorComponent(camera:)`.
 *
 *  - `Dispatchers.Main` -> state callbacks arrive on the main thread, ready for SwiftUI.
 *  - `SupervisorJob`    -> one failed child coroutine doesn't tear down the whole scope.
 *  - `ownsScope = true` -> the component cancels this scope in `close()`, since it created it.
 */
fun createCalculatorComponent(camera: CameraController): CalculatorComponent =
    CalculatorComponent(
        camera = camera,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
        ownsScope = true,
    )
