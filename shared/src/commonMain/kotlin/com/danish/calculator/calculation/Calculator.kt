// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.calculation

import com.danish.calculator.model.MathOperation
import kotlin.math.abs

/**
 * Outcome of one arithmetic attempt. A sealed type (rather than throwing) forces the
 * reducer to handle every case explicitly and keeps the failure reasons testable.
 */
sealed interface CalculationResult {
    /** [formatted] is display-ready ("5", "3.5", "-2"), never "5.0". */
    data class Success(val formatted: String) : CalculationResult
    data object DivisionByZero : CalculationResult
    data object ResultTooLarge : CalculationResult
}

/**
 * The pure arithmetic core. No state, no IO — just numbers in, [CalculationResult] out.
 *
 * `internal` because only the reducer should call it; the UI never does math directly.
 * It receives already-parsed `Double`s (parsing/validation is `InputValidator`'s job).
 */
internal object Calculator {

    /**
     * Results are capped at 1e15. Past this, `Double` starts losing integer precision and
     * the formatted output becomes misleading, so we reject it as "too large" instead.
     */
    private const val MAX_MAGNITUDE = 1e15

    fun calculate(first: Double, second: Double, operation: MathOperation): CalculationResult {
        val raw = when (operation) {
            MathOperation.ADD -> first + second
            MathOperation.SUBTRACT -> first - second
            MathOperation.MULTIPLY -> first * second
            MathOperation.DIVIDE -> {
                // Guard before dividing: `x / 0.0` would produce Infinity/NaN, not an error.
                if (second == 0.0) return CalculationResult.DivisionByZero
                first / second
            }
        }
        // Catches Infinity/NaN (e.g. huge * huge) and anything over the precision cap.
        if (!raw.isFinite() || abs(raw) > MAX_MAGNITUDE) return CalculationResult.ResultTooLarge
        return CalculationResult.Success(format(raw))
    }

    /**
     * Formats so whole numbers print without a trailing ".0" ("5", not "5.0") while real
     * decimals are left alone ("3.5").
     */
    private fun format(value: Double): String {
        val v = if (value == 0.0) 0.0 else value // normalise -0.0 to 0.0 so it prints as "0"
        return if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
    }
}
