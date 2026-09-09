// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator

import com.danish.calculator.model.MathOperation
import com.danish.calculator.model.PhotoReference
import com.danish.calculator.mvi.CalculatorEffect
import com.danish.calculator.mvi.CalculatorIntent
import com.danish.calculator.mvi.CalculatorStore
import com.danish.calculator.mvi.DefaultCalculatorStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

fun newStore() = DefaultCalculatorStore()

fun samplePhoto(path: String = "/tmp/photo_1.jpg") =
    PhotoReference(filePath = path, widthPx = 1200, heightPx = 1600, capturedAtEpochMs = 1_000L)

fun CalculatorStore.enter(first: String, second: String, op: MathOperation?) {
    dispatch(CalculatorIntent.FirstNumberChanged(first))
    dispatch(CalculatorIntent.SecondNumberChanged(second))
    if (op != null) dispatch(CalculatorIntent.OperationSelected(op))
}

class EffectRecorder(val effects: MutableList<CalculatorEffect> = mutableListOf())

@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.recorderFor(store: CalculatorStore): EffectRecorder {
    val recorder = EffectRecorder()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        store.effects.toList(recorder.effects)
    }
    return recorder
}
