// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.model

/**
 * The ENTIRE calculator screen, described as one immutable value.
 *
 * This is the "Model" in MVI. Rules that make the pattern work:
 *  - Every field is a `val` and the class is a `data class`, so a state can only be
 *    "changed" by producing a NEW copy (`state.copy(...)`) from the reducer.
 *  - The UI is a pure function of this object. Given the same [CalculatorState], both
 *    the Compose screen and the SwiftUI screen must render identically.
 *  - Nothing platform-specific is allowed here: no Android `Uri`, no `UIImage`, no files.
 *    A photo is represented by [PhotoReference] (a path + dimensions), decoded to a real
 *    image only inside each platform's UI layer.
 *
 * Field groups:
 *  - firstNumber / secondNumber / selectedOperation  -> raw user input, kept as typed
 *  - *Error fields                                   -> per-field validation messages (null = no error)
 *  - result / resultError                            -> outcome of the last "Calculate"
 *  - photoStatus / cameraError                       -> camera sub-state (see [PhotoStatus])
 *
 * Errors are stored as plain `String?` (not error codes) so the shared reducer owns the
 * wording and the tests can assert on it. See `CalculatorErrors`.
 */
data class CalculatorState(
    val firstNumber: String = "",
    val secondNumber: String = "",
    val selectedOperation: MathOperation? = null,
    val firstNumberError: String? = null,
    val secondNumberError: String? = null,
    val operationError: String? = null,
    val result: String? = null,
    val resultError: String? = null,
    val photoStatus: PhotoStatus = PhotoStatus.Empty,
    val cameraError: String? = null,
) {
    /**
     * The photo the UI should currently show, or null for "no photo".
     *
     * While the camera is open we still want the previous picture on screen, so
     * [PhotoStatus.Launching] carries the old reference and we surface it here. This keeps
     * the "camera cancelled -> keep what we had" behaviour out of the view.
     */
    val displayPhoto: PhotoReference?
        get() = when (val s = photoStatus) {
            is PhotoStatus.Captured -> s.reference
            is PhotoStatus.Launching -> s.previous
            PhotoStatus.Empty -> null
        }

    /** Convenience for the UI: does the button say "Open camera" or "Retake photo"? */
    val hasPhoto: Boolean get() = displayPhoto != null

    /** True only in the brief window between tapping the button and the camera returning. */
    val isCameraLaunching: Boolean get() = photoStatus is PhotoStatus.Launching
}
