// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.camera

import com.danish.calculator.model.PhotoReference
import com.danish.calculator.mvi.CalculatorIntent

/**
 * Everything the camera attempt can end in. Shared code speaks only in these terms; it
 * never sees `UIImagePickerController` or `ACTION_IMAGE_CAPTURE`.
 */
sealed interface CameraCaptureResult {
    data class Captured(val reference: PhotoReference) : CameraCaptureResult
    data object Cancelled : CameraCaptureResult
    data object PermissionDenied : CameraCaptureResult
    data object Unavailable : CameraCaptureResult
    data object Failed : CameraCaptureResult
}

/**
 * The bridge between the platform camera result and the MVI loop: every result maps 1:1
 * to an intent, so the reducer stays the single place that decides what a result *means*.
 */
fun CameraCaptureResult.toIntent(): CalculatorIntent = when (this) {
    is CameraCaptureResult.Captured -> CalculatorIntent.PhotoCaptured(reference)
    CameraCaptureResult.Cancelled -> CalculatorIntent.PhotoCaptureCancelled
    CameraCaptureResult.PermissionDenied -> CalculatorIntent.CameraPermissionDenied
    CameraCaptureResult.Unavailable -> CalculatorIntent.CameraUnavailable
    CameraCaptureResult.Failed -> CalculatorIntent.PhotoCaptureFailed
}

/**
 * The seam between shared logic and native camera code.
 *
 * This project uses a plain interface + dependency injection instead of Kotlin's
 * `expect`/`actual`. Benefits: the shared tests can pass a fake, and each platform builds
 * its implementation with full access to its own SDK:
 *   - Android: `rememberCameraController()` in androidApp (Compose + ActivityResult APIs)
 *   - iOS:     `IosCameraController` in iosApp (UIImagePickerController / AVFoundation)
 *
 * [capture] is callback-based (not `suspend`) because the platform camera APIs are
 * themselves callback/delegate based; [onResult] is invoked exactly once.
 * [release] deletes a file that a newer photo has replaced.
 */
interface CameraController {
    fun capture(onResult: (CameraCaptureResult) -> Unit)
    fun release(reference: PhotoReference)
}
