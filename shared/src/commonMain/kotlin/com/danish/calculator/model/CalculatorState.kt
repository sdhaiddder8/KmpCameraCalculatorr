// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.model

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
    val displayPhoto: PhotoReference?
        get() = when (val s = photoStatus) {
            is PhotoStatus.Captured -> s.reference
            is PhotoStatus.Launching -> s.previous
            PhotoStatus.Empty -> null
        }

    val hasPhoto: Boolean get() = displayPhoto != null

    val isCameraLaunching: Boolean get() = photoStatus is PhotoStatus.Launching
}
