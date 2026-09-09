// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.mvi

import com.danish.calculator.camera.CameraController
import com.danish.calculator.camera.toIntent
import com.danish.calculator.model.CalculatorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

fun interface Cancellable {
    fun cancel()
}

class CalculatorComponent(
    private val camera: CameraController,
    private val scope: CoroutineScope,
    private val store: CalculatorStore = DefaultCalculatorStore(),
    private val ownsScope: Boolean = false,
) {
    val state: StateFlow<CalculatorState> get() = store.state

    init {
        store.effects.onEach { effect ->
            when (effect) {
                CalculatorEffect.LaunchCamera -> camera.capture { store.dispatch(it.toIntent()) }
                is CalculatorEffect.ReleasePhoto -> camera.release(effect.reference)
            }
        }.launchIn(scope)
    }

    fun dispatch(intent: CalculatorIntent) = store.dispatch(intent)

    fun watchState(onChange: (CalculatorState) -> Unit): Cancellable {
        val job = store.state.onEach(onChange).launchIn(scope)
        return Cancellable { job.cancel() }
    }

    fun close() {
        store.close()
        if (ownsScope) scope.cancel()
    }
}
