// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator

import com.danish.calculator.model.MathOperation
import com.danish.calculator.model.PhotoStatus
import com.danish.calculator.mvi.CalculatorIntent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * State-machine rules that aren't arithmetic: each input intent lands in state, editing an
 * input clears the stale result and that field's error, changing the operation clears the
 * result, and none of the calculator inputs disturb a captured photo.
 */
class MviStateTest {

    /** Every input intent is reflected in the corresponding state field. */
    @Test
    fun stateReflectsEachInputIntent() {
        val store = newStore()

        store.dispatch(CalculatorIntent.FirstNumberChanged("12"))
        assertEquals("12", store.state.value.firstNumber)

        store.dispatch(CalculatorIntent.SecondNumberChanged("3.5"))
        assertEquals("3.5", store.state.value.secondNumber)

        store.dispatch(CalculatorIntent.OperationSelected(MathOperation.MULTIPLY))
        assertEquals(MathOperation.MULTIPLY, store.state.value.selectedOperation)
    }

    /** Editing an input clears its old error AND the now-stale result. */
    @Test
    fun changingInputClearsResultAndOldError() {
        val store = newStore()
        store.enter("10", "0", MathOperation.DIVIDE)
        store.dispatch(CalculatorIntent.CalculateClicked)
        assertEquals("Cannot divide by zero", store.state.value.secondNumberError)

        store.dispatch(CalculatorIntent.SecondNumberChanged("2"))
        assertNull(store.state.value.secondNumberError)
        assertNull(store.state.value.result)

        store.dispatch(CalculatorIntent.CalculateClicked)
        assertEquals("5", store.state.value.result)

        store.dispatch(CalculatorIntent.FirstNumberChanged("11"))
        assertNull(store.state.value.result) // stale result gone after another edit
    }

    /** Changing the operation also invalidates the previous result. */
    @Test
    fun changingOperationClearsResult() {
        val store = newStore()
        store.enter("2", "3", MathOperation.ADD)
        store.dispatch(CalculatorIntent.CalculateClicked)
        assertEquals("5", store.state.value.result)

        store.dispatch(CalculatorIntent.OperationSelected(MathOperation.MULTIPLY))
        assertNull(store.state.value.result)
    }

    /** The photo is independent of the calculator: editing inputs / calculating keeps it. */
    @Test
    fun changingInputsDoesNotClearThePhoto() {
        val store = newStore()
        store.dispatch(CalculatorIntent.PhotoCaptured(samplePhoto()))
        assertTrue(store.state.value.photoStatus is PhotoStatus.Captured)

        store.dispatch(CalculatorIntent.FirstNumberChanged("7"))
        store.dispatch(CalculatorIntent.SecondNumberChanged("8"))
        store.dispatch(CalculatorIntent.OperationSelected(MathOperation.ADD))
        store.dispatch(CalculatorIntent.CalculateClicked)

        assertEquals("15", store.state.value.result)
        assertEquals(samplePhoto(), store.state.value.displayPhoto)
    }
}
