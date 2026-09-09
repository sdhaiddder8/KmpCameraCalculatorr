// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.mvi

import com.danish.calculator.model.CalculatorState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

interface CalculatorStore {
    val state: StateFlow<CalculatorState>
    val effects: Flow<CalculatorEffect>
    fun dispatch(intent: CalculatorIntent)
    fun close()
}

class DefaultCalculatorStore(
    initialState: CalculatorState = CalculatorState(),
) : CalculatorStore {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<CalculatorState> = _state.asStateFlow()

    private val _effects = Channel<CalculatorEffect>(16, BufferOverflow.SUSPEND)
    override val effects: Flow<CalculatorEffect> = _effects.receiveAsFlow()

    override fun dispatch(intent: CalculatorIntent) {
        val (newState, effects) = CalculatorReducer.reduce(_state.value, intent)
        _state.value = newState
        effects.forEach { _effects.trySend(it) }
    }

    override fun close() {
        _effects.close()
    }
}
