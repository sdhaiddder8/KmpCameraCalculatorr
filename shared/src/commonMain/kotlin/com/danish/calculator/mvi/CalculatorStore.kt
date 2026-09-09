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

/**
 * The store: holds the current state and is the ONLY entry point for changing it.
 *
 *  - [state]    -> a `StateFlow`: always has a current value, replays it to new collectors.
 *                  This is what the UI observes.
 *  - [effects]  -> a `Flow` of one-shot effects; each item is delivered once (see below).
 *  - [dispatch] -> feed an intent in; the reducer runs synchronously and state updates.
 *  - [close]    -> release the effect channel when the screen goes away.
 *
 * It's an interface so tests (and the Android/iOS holders) depend on the abstraction.
 */
interface CalculatorStore {
    val state: StateFlow<CalculatorState>
    val effects: Flow<CalculatorEffect>
    fun dispatch(intent: CalculatorIntent)
    fun close()
}

/**
 * The real implementation. Pure Kotlin + coroutines, so the identical object runs on the
 * JVM (Android) and Kotlin/Native (iOS).
 */
class DefaultCalculatorStore(
    initialState: CalculatorState = CalculatorState(),
) : CalculatorStore {

    // Private mutable / public read-only is the standard StateFlow pattern: nobody outside
    // can assign state; they must go through dispatch().
    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<CalculatorState> = _state.asStateFlow()

    // A Channel (not a SharedFlow) because effects must be delivered exactly once to one
    // consumer. Capacity 16 + SUSPEND: if effects somehow pile up, the producer waits
    // rather than dropping one.
    private val _effects = Channel<CalculatorEffect>(16, BufferOverflow.SUSPEND)
    override val effects: Flow<CalculatorEffect> = _effects.receiveAsFlow()

    override fun dispatch(intent: CalculatorIntent) {
        // 1. Run the pure reducer on the current state.
        val (newState, effects) = CalculatorReducer.reduce(_state.value, intent)
        // 2. Publish the new state (StateFlow notifies the UI if it actually changed).
        _state.value = newState
        // 3. Hand off any effects. trySend is safe here because the buffer is large enough
        //    for this app's effect volume and we never want dispatch() to suspend.
        effects.forEach { _effects.trySend(it) }
    }

    override fun close() {
        _effects.close()
    }
}
