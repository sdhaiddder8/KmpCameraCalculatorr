// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.validation

import com.danish.calculator.model.CalculatorState
import com.danish.calculator.model.MathOperation

sealed interface ValidationOutcome {
    data class Valid(val first: Double, val second: Double, val operation: MathOperation) : ValidationOutcome
    data class Invalid(
        val firstNumberError: String? = null,
        val secondNumberError: String? = null,
        val operationError: String? = null,
    ) : ValidationOutcome
}

internal object InputValidator {

    private val NUMBER = Regex("""[+-]?(\d+\.?\d*|\.\d+)""")

    fun validate(state: CalculatorState): ValidationOutcome {
        val first = parse(state.firstNumber, CalculatorErrors.FIRST_NUMBER_REQUIRED)
        val second = parse(state.secondNumber, CalculatorErrors.SECOND_NUMBER_REQUIRED)
        val operationError =
            if (state.selectedOperation == null) CalculatorErrors.NO_OPERATION_SELECTED else null

        if (first.error != null || second.error != null || operationError != null) {
            return ValidationOutcome.Invalid(first.error, second.error, operationError)
        }
        return ValidationOutcome.Valid(first.value!!, second.value!!, state.selectedOperation!!)
    }

    private class Parsed(val value: Double?, val error: String?)

    private fun parse(raw: String, requiredMessage: String): Parsed {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return Parsed(null, requiredMessage)
        val value = trimmed.toDoubleOrNull()
        if (!NUMBER.matches(trimmed) || value == null || !value.isFinite()) {
            return Parsed(null, CalculatorErrors.NOT_A_VALID_NUMBER)
        }
        return Parsed(value, null)
    }
}
