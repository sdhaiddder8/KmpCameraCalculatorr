// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.camera

import com.danish.calculator.model.PhotoReference
import com.danish.calculator.mvi.CalculatorIntent

sealed interface CameraCaptureResult {
    data class Captured(val reference: PhotoReference) : CameraCaptureResult
    data object Cancelled : CameraCaptureResult
    data object PermissionDenied : CameraCaptureResult
    data object Unavailable : CameraCaptureResult
    data object Failed : CameraCaptureResult
}

fun CameraCaptureResult.toIntent(): CalculatorIntent = when (this) {
    is CameraCaptureResult.Captured -> CalculatorIntent.PhotoCaptured(reference)
    CameraCaptureResult.Cancelled -> CalculatorIntent.PhotoCaptureCancelled
    CameraCaptureResult.PermissionDenied -> CalculatorIntent.CameraPermissionDenied
    CameraCaptureResult.Unavailable -> CalculatorIntent.CameraUnavailable
    CameraCaptureResult.Failed -> CalculatorIntent.PhotoCaptureFailed
}

interface CameraController {
    fun capture(onResult: (CameraCaptureResult) -> Unit)
    fun release(reference: PhotoReference)
}
