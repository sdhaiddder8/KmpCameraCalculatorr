// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.validation

import com.danish.calculator.model.CalculatorState
import com.danish.calculator.model.MathOperation

/**
 * Result of validating the whole input form at once.
 *
 *  - [Valid]   -> carries the parsed `Double`s and the chosen operation, ready for math.
 *  - [Invalid] -> carries one message per bad field. All three are reported TOGETHER so the
 *                 user sees every problem in a single pass, not one at a time.
 */
sealed interface ValidationOutcome {
    data class Valid(val first: Double, val second: Double, val operation: MathOperation) : ValidationOutcome
    data class Invalid(
        val firstNumberError: String? = null,
        val secondNumberError: String? = null,
        val operationError: String? = null,
    ) : ValidationOutcome
}

/**
 * Turns the raw text fields of a [CalculatorState] into either usable numbers or errors.
 *
 * The UI deliberately does NOT restrict typing (the decimal keyboard is a hint only), so
 * every parse/range check has to happen here, once, in shared code.
 */
internal object InputValidator {

    /**
     * A single, optionally-signed decimal number: "12", "-3.5", ".5", "+7." are all valid;
     * "1e5", "1,000", "1 2" are not. We check this regex AND `toDoubleOrNull` because each
     * catches things the other misses (regex rejects exponents `toDouble` would accept;
     * `toDouble` rejects overflow the regex would pass).
     */
    private val NUMBER = Regex("""[+-]?(\d+\.?\d*|\.\d+)""")

    fun validate(state: CalculatorState): ValidationOutcome {
        val first = parse(state.firstNumber, CalculatorErrors.FIRST_NUMBER_REQUIRED)
        val second = parse(state.secondNumber, CalculatorErrors.SECOND_NUMBER_REQUIRED)
        val operationError =
            if (state.selectedOperation == null) CalculatorErrors.NO_OPERATION_SELECTED else null

        // If anything failed, return every message at once and don't attempt the math.
        if (first.error != null || second.error != null || operationError != null) {
            return ValidationOutcome.Invalid(first.error, second.error, operationError)
        }
        // Safe: the checks above guarantee these are non-null here.
        return ValidationOutcome.Valid(first.value!!, second.value!!, state.selectedOperation!!)
    }

    /** Internal parse outcome: exactly one of [value] / [error] is non-null. */
    private class Parsed(val value: Double?, val error: String?)

    private fun parse(raw: String, requiredMessage: String): Parsed {
        val trimmed = raw.trim() // leading/trailing spaces are tolerated, e.g. "  1.5 "
        if (trimmed.isEmpty()) return Parsed(null, requiredMessage)
        val value = trimmed.toDoubleOrNull()
        if (!NUMBER.matches(trimmed) || value == null || !value.isFinite()) {
            return Parsed(null, CalculatorErrors.NOT_A_VALID_NUMBER)
        }
        return Parsed(value, null)
    }
}
