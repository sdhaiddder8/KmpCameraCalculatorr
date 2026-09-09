// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.mvi

import com.danish.calculator.model.MathOperation
import com.danish.calculator.model.PhotoReference

sealed interface CalculatorIntent {

    data class FirstNumberChanged(val value: String) : CalculatorIntent
    data class SecondNumberChanged(val value: String) : CalculatorIntent
    data class OperationSelected(val operation: MathOperation) : CalculatorIntent
    data object CalculateClicked : CalculatorIntent
    data object OpenCameraClicked : CalculatorIntent

    data class PhotoCaptured(val reference: PhotoReference) : CalculatorIntent
    data object PhotoCaptureCancelled : CalculatorIntent
    data object CameraPermissionDenied : CalculatorIntent
    data object CameraUnavailable : CalculatorIntent
    data object PhotoCaptureFailed : CalculatorIntent
}
