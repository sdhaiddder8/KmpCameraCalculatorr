// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator

import com.danish.calculator.model.MathOperation
import com.danish.calculator.mvi.CalculatorIntent
import com.danish.calculator.validation.CalculatorErrors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CalculatorLogicTest {

    private fun calculate(first: String, second: String, op: MathOperation?) = newStore().apply {
        enter(first, second, op)
        dispatch(CalculatorIntent.CalculateClicked)
    }.state.value

    @Test
    fun addition() {
        val state = calculate("2", "3", MathOperation.ADD)
        assertEquals("5", state.result)
        assertNull(state.firstNumberError)
        assertNull(state.secondNumberError)
        assertNull(state.operationError)
    }

    @Test
    fun subtraction() {
        assertEquals("6", calculate("10", "4", MathOperation.SUBTRACT).result)
        assertEquals("-2", calculate("4", "6", MathOperation.SUBTRACT).result)
    }

    @Test
    fun multiplicationWithNegativeNumber() {
        assertEquals("-6", calculate("3", "-2", MathOperation.MULTIPLY).result)
    }

    @Test
    fun divisionProducingDecimalResult() {
        assertEquals("3.5", calculate("7", "2", MathOperation.DIVIDE).result)
    }

    @Test
    fun trimsWhitespaceAndSupportsDecimals() {
        assertEquals("4", calculate("  1.5 ", "2.5", MathOperation.ADD).result)
    }

    @Test
    fun emptyInputsAreRequired() {
        val state = calculate("", "", MathOperation.ADD)
        assertEquals(CalculatorErrors.FIRST_NUMBER_REQUIRED, state.firstNumberError)
        assertEquals(CalculatorErrors.SECOND_NUMBER_REQUIRED, state.secondNumberError)
        assertNull(state.result)
    }

    @Test
    fun invalidNumericInput() {
        val state = calculate("abc", "2", MathOperation.ADD)
        assertEquals(CalculatorErrors.NOT_A_VALID_NUMBER, state.firstNumberError)
        assertNull(state.result)
    }

    @Test
    fun missingOperation() {
        val state = calculate("1", "2", null)
        assertEquals(CalculatorErrors.NO_OPERATION_SELECTED, state.operationError)
        assertNull(state.result)
    }

    @Test
    fun divisionByZero() {
        assertEquals(CalculatorErrors.DIVISION_BY_ZERO, calculate("10", "0", MathOperation.DIVIDE).secondNumberError)
        assertEquals(CalculatorErrors.DIVISION_BY_ZERO, calculate("10", "-0", MathOperation.DIVIDE).secondNumberError)
        assertNull(calculate("10", "0", MathOperation.DIVIDE).result)
    }

    @Test
    fun allInvalidFieldsReportedTogether() {
        val state = calculate("", "x", null)
        assertEquals(CalculatorErrors.FIRST_NUMBER_REQUIRED, state.firstNumberError)
        assertEquals(CalculatorErrors.NOT_A_VALID_NUMBER, state.secondNumberError)
        assertEquals(CalculatorErrors.NO_OPERATION_SELECTED, state.operationError)
    }

    @Test
    fun resultTooLargeIsRejected() {
        val state = calculate("9999999999", "9999999999", MathOperation.MULTIPLY)
        assertEquals(CalculatorErrors.RESULT_TOO_LARGE, state.resultError)
        assertNull(state.result)
    }
}
