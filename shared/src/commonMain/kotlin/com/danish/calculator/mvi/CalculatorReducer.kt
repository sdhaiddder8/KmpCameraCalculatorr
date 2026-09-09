// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.mvi

import com.danish.calculator.calculation.CalculationResult
import com.danish.calculator.calculation.Calculator
import com.danish.calculator.model.CalculatorState
import com.danish.calculator.model.PhotoStatus
import com.danish.calculator.validation.CalculatorErrors
import com.danish.calculator.validation.InputValidator
import com.danish.calculator.validation.ValidationOutcome

data class Reduction(
    val state: CalculatorState,
    val effects: List<CalculatorEffect> = emptyList(),
)

internal object CalculatorReducer {

    fun reduce(state: CalculatorState, intent: CalculatorIntent): Reduction = when (intent) {

        is CalculatorIntent.FirstNumberChanged ->
            Reduction(state.copy(firstNumber = intent.value, firstNumberError = null).clearResult())

        is CalculatorIntent.SecondNumberChanged ->
            Reduction(state.copy(secondNumber = intent.value, secondNumberError = null).clearResult())

        is CalculatorIntent.OperationSelected ->
            Reduction(state.copy(selectedOperation = intent.operation, operationError = null).clearResult())

        CalculatorIntent.CalculateClicked -> calculate(state)

        CalculatorIntent.OpenCameraClicked -> Reduction(
            state.copy(photoStatus = PhotoStatus.Launching(state.displayPhoto), cameraError = null),
            listOf(CalculatorEffect.LaunchCamera),
        )

        is CalculatorIntent.PhotoCaptured -> {
            val old = state.displayPhoto
            val effects = if (old != null && old.filePath != intent.reference.filePath) {
                listOf(CalculatorEffect.ReleasePhoto(old))
            } else {
                emptyList()
            }
            Reduction(state.copy(photoStatus = PhotoStatus.Captured(intent.reference), cameraError = null), effects)
        }

        CalculatorIntent.PhotoCaptureCancelled ->
            Reduction(state.copy(photoStatus = resting(state), cameraError = null))

        CalculatorIntent.CameraPermissionDenied ->
            cameraError(state, CalculatorErrors.CAMERA_PERMISSION_REQUIRED)

        CalculatorIntent.CameraUnavailable ->
            cameraError(state, CalculatorErrors.CAMERA_NOT_AVAILABLE)

        CalculatorIntent.PhotoCaptureFailed ->
            cameraError(state, CalculatorErrors.CAPTURE_FAILED)
    }

    private fun calculate(state: CalculatorState): Reduction =
        when (val outcome = InputValidator.validate(state)) {
            is ValidationOutcome.Invalid -> Reduction(
                state.copy(
                    firstNumberError = outcome.firstNumberError,
                    secondNumberError = outcome.secondNumberError,
                    operationError = outcome.operationError,
                    result = null,
                    resultError = null,
                ),
            )

            is ValidationOutcome.Valid -> {
                val clean = state.copy(firstNumberError = null, secondNumberError = null, operationError = null)
                when (val calc = Calculator.calculate(outcome.first, outcome.second, outcome.operation)) {
                    is CalculationResult.Success -> Reduction(clean.copy(result = calc.formatted, resultError = null))
                    CalculationResult.DivisionByZero ->
                        Reduction(clean.copy(result = null, secondNumberError = CalculatorErrors.DIVISION_BY_ZERO))
                    CalculationResult.ResultTooLarge ->
                        Reduction(clean.copy(result = null, resultError = CalculatorErrors.RESULT_TOO_LARGE))
                }
            }
        }

    private fun cameraError(state: CalculatorState, message: String) =
        Reduction(state.copy(photoStatus = resting(state), cameraError = message))

    private fun CalculatorState.clearResult() = copy(result = null, resultError = null)

    private fun resting(state: CalculatorState): PhotoStatus =
        state.displayPhoto?.let(PhotoStatus::Captured) ?: PhotoStatus.Empty
}
