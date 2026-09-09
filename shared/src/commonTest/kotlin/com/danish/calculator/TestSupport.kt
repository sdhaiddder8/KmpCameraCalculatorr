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

// Shared helpers for the commonTest suite. These run on BOTH the JVM and Kotlin/Native,
// so the exact same tests verify the exact same behaviour on Android and iOS.

/** A fresh real store — the tests exercise the production reducer, nothing is mocked. */
fun newStore() = DefaultCalculatorStore()

/** A canned [PhotoReference] for camera tests. */
fun samplePhoto(path: String = "/tmp/photo_1.jpg") =
    PhotoReference(filePath = path, widthPx = 1200, heightPx = 1600, capturedAtEpochMs = 1_000L)

/** Fills the three input fields in one call, so tests read as "enter X, Y, op then ...". */
fun CalculatorStore.enter(first: String, second: String, op: MathOperation?) {
    dispatch(CalculatorIntent.FirstNumberChanged(first))
    dispatch(CalculatorIntent.SecondNumberChanged(second))
    if (op != null) dispatch(CalculatorIntent.OperationSelected(op))
}

/** Collects everything the store emits on its one-shot effect stream. */
class EffectRecorder(val effects: MutableList<CalculatorEffect> = mutableListOf())

/**
 * Starts draining `store.effects` into an [EffectRecorder] on a background coroutine.
 *
 * `backgroundScope` is auto-cancelled when the test ends. `UnconfinedTestDispatcher` runs
 * the collector eagerly, so an effect dispatched right after this call is already recorded
 * by the time the next assertion runs — no manual scheduler advancing needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.recorderFor(store: CalculatorStore): EffectRecorder {
    val recorder = EffectRecorder()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        store.effects.toList(recorder.effects)
    }
    return recorder
}
