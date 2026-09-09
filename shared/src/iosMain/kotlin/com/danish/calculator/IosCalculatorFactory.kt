// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator

import com.danish.calculator.camera.CameraController
import com.danish.calculator.mvi.CalculatorComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun createCalculatorComponent(camera: CameraController): CalculatorComponent =
    CalculatorComponent(
        camera = camera,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
        ownsScope = true,
    )
