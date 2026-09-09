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

/**
 * A tiny "unsubscribe" handle. `fun interface` = it can be created from a lambda, and
 * Kotlin/Native exposes it to Swift as an object with a `cancel()` method (Swift calls it
 * from `deinit`).
 */
fun interface Cancellable {
    fun cancel()
}

/**
 * Wires the pure [CalculatorStore] to the impure outside world (the camera) and gives the
 * iOS side a Swift-friendly surface.
 *
 * Android doesn't strictly need this class — Compose collects `store.effects` directly in
 * `CalculatorScreen`. It exists mainly so Swift has:
 *   - [state] as a plain `StateFlow` getter (Swift reads `.value`)
 *   - [watchState] instead of having to collect a Kotlin `Flow` (awkward from Swift)
 *   - [dispatch] / [close] passthroughs
 *
 * @param camera    the platform camera implementation (injected, never created here)
 * @param scope     coroutine scope the effect/state subscriptions run in
 * @param store     defaults to a fresh store; overridable in tests
 * @param ownsScope if true, [close] also cancels [scope] (iOS creates the scope, so it owns it)
 */
class CalculatorComponent(
    private val camera: CameraController,
    private val scope: CoroutineScope,
    private val store: CalculatorStore = DefaultCalculatorStore(),
    private val ownsScope: Boolean = false,
) {
    val state: StateFlow<CalculatorState> get() = store.state

    init {
        // The one place effects are turned into real work.
        store.effects.onEach { effect ->
            when (effect) {
                // Open the camera; when it finishes, translate the result to an intent and
                // feed it back into the store — closing the MVI loop.
                CalculatorEffect.LaunchCamera -> camera.capture { store.dispatch(it.toIntent()) }
                // A photo was replaced: delete the old file.
                is CalculatorEffect.ReleasePhoto -> camera.release(effect.reference)
            }
        }.launchIn(scope)
    }

    fun dispatch(intent: CalculatorIntent) = store.dispatch(intent)

    /**
     * Swift-facing state subscription. Calls [onChange] on every state emission and returns
     * a [Cancellable] to stop. (SwiftUI's `CalculatorViewStore` uses this to feed a
     * `@Published` property.)
     */
    fun watchState(onChange: (CalculatorState) -> Unit): Cancellable {
        val job = store.state.onEach(onChange).launchIn(scope)
        return Cancellable { job.cancel() }
    }

    fun close() {
        store.close()
        if (ownsScope) scope.cancel()
    }
}
