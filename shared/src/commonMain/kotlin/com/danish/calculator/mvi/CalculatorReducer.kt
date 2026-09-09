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

/**
 * What the reducer returns: the next state, plus any effects to run.
 * Defaulting [effects] to empty keeps the common "just a new state" case terse.
 */
data class Reduction(
    val state: CalculatorState,
    val effects: List<CalculatorEffect> = emptyList(),
)

/**
 * The heart of the app: `(state, intent) -> (newState, effects)`, and nothing else.
 *
 * This function is PURE — no coroutines, no clock, no camera, no IO. That is what lets the
 * shared tests run the real reducer with no mocks. Every behaviour rule of the app is
 * expressed here as a branch of the `when`.
 */
internal object CalculatorReducer {

    fun reduce(state: CalculatorState, intent: CalculatorIntent): Reduction = when (intent) {

        // --- Typing / selecting: store the raw input, drop that field's stale error, and
        //     clear any old result (it no longer matches the inputs). ---
        is CalculatorIntent.FirstNumberChanged ->
            Reduction(state.copy(firstNumber = intent.value, firstNumberError = null).clearResult())

        is CalculatorIntent.SecondNumberChanged ->
            Reduction(state.copy(secondNumber = intent.value, secondNumberError = null).clearResult())

        is CalculatorIntent.OperationSelected ->
            Reduction(state.copy(selectedOperation = intent.operation, operationError = null).clearResult())

        // --- The only intent that actually does arithmetic. ---
        CalculatorIntent.CalculateClicked -> calculate(state)

        // --- Camera: move to "Launching" (keeps the current photo visible via displayPhoto)
        //     and ask the platform to open the camera through a one-shot effect. ---
        CalculatorIntent.OpenCameraClicked -> Reduction(
            state.copy(photoStatus = PhotoStatus.Launching(state.displayPhoto), cameraError = null),
            listOf(CalculatorEffect.LaunchCamera),
        )

        // --- A photo came back. Show it, and if it replaced a DIFFERENT file, emit
        //     ReleasePhoto so the platform deletes the old one. ---
        is CalculatorIntent.PhotoCaptured -> {
            val old = state.displayPhoto
            val effects = if (old != null && old.filePath != intent.reference.filePath) {
                listOf(CalculatorEffect.ReleasePhoto(old))
            } else {
                emptyList()
            }
            Reduction(state.copy(photoStatus = PhotoStatus.Captured(intent.reference), cameraError = null), effects)
        }

        // --- User backed out of the camera: go back to whatever photo we had, no error. ---
        CalculatorIntent.PhotoCaptureCancelled ->
            Reduction(state.copy(photoStatus = resting(state), cameraError = null))

        // --- The three camera failure modes all: drop back to the resting photo state and
        //     surface a shared error string. ---
        CalculatorIntent.CameraPermissionDenied ->
            cameraError(state, CalculatorErrors.CAMERA_PERMISSION_REQUIRED)

        CalculatorIntent.CameraUnavailable ->
            cameraError(state, CalculatorErrors.CAMERA_NOT_AVAILABLE)

        CalculatorIntent.PhotoCaptureFailed ->
            cameraError(state, CalculatorErrors.CAPTURE_FAILED)
    }

    /**
     * Validate, then (only if valid) compute. On invalid input, write every field message
     * and clear the result. On valid input, first wipe the error fields, then map the
     * [CalculationResult] onto either `result` or the appropriate error slot.
     *
     * Note: divide-by-zero is reported on `secondNumberError` (it's really an input
     * problem), while overflow is reported on `resultError` (the inputs were fine).
     */
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

    /** A result is only valid for the inputs that produced it, so any input edit clears it. */
    private fun CalculatorState.clearResult() = copy(result = null, resultError = null)

    /**
     * The photo state to return to when the camera closes without a new picture:
     * keep the existing photo if there is one, otherwise go back to Empty.
     */
    private fun resting(state: CalculatorState): PhotoStatus =
        state.displayPhoto?.let(PhotoStatus::Captured) ?: PhotoStatus.Empty
}
