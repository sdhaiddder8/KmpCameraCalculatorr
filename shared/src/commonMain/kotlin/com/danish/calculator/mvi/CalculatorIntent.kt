// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.mvi

import com.danish.calculator.model.MathOperation
import com.danish.calculator.model.PhotoReference

/**
 * The "I" in MVI: every single thing that can drive a state change.
 *
 * Nothing else may mutate the calculator. The UI reacts to state and, in response to the
 * user, sends one of these back in via `dispatch`. A sealed interface means the reducer's
 * `when` is exhaustive — add a case here and the compiler forces you to handle it.
 *
 * Two sources of intents:
 *  - User actions:   FirstNumberChanged, OperationSelected, CalculateClicked, OpenCameraClicked
 *  - Camera results: PhotoCaptured / PhotoCaptureCancelled / CameraPermissionDenied /
 *                    CameraUnavailable / PhotoCaptureFailed  — the platform camera code
 *                    reports what happened by dispatching one of these (see CameraController.toIntent).
 */
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
