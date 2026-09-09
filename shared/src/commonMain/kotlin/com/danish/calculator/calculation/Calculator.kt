// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.calculation

import com.danish.calculator.model.MathOperation
import kotlin.math.abs

sealed interface CalculationResult {
    data class Success(val formatted: String) : CalculationResult
    data object DivisionByZero : CalculationResult
    data object ResultTooLarge : CalculationResult
}

internal object Calculator {

    private const val MAX_MAGNITUDE = 1e15

    fun calculate(first: Double, second: Double, operation: MathOperation): CalculationResult {
        val raw = when (operation) {
            MathOperation.ADD -> first + second
            MathOperation.SUBTRACT -> first - second
            MathOperation.MULTIPLY -> first * second
            MathOperation.DIVIDE -> {
                if (second == 0.0) return CalculationResult.DivisionByZero
                first / second
            }
        }
        if (!raw.isFinite() || abs(raw) > MAX_MAGNITUDE) return CalculationResult.ResultTooLarge
        return CalculationResult.Success(format(raw))
    }

    private fun format(value: Double): String {
        val v = if (value == 0.0) 0.0 else value
        return if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
    }
}
