// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.android

import androidx.lifecycle.ViewModel
import com.danish.calculator.mvi.CalculatorIntent
import com.danish.calculator.mvi.DefaultCalculatorStore

/**
 * The only reason this class exists: an Android `ViewModel` survives configuration changes
 * (rotation, dark-mode toggle, ...), so the shared [DefaultCalculatorStore] — and with it
 * the typed numbers, result and captured photo — isn't recreated when the Activity is.
 *
 * It holds NO logic and NO state of its own. It just exposes the store's `state` / `effects`
 * and forwards `dispatch`. All behaviour lives in the shared reducer.
 *
 * (The iOS counterpart is `CalculatorViewStore`, an `ObservableObject`.)
 */
class CalculatorStoreHolder : ViewModel() {

    private val store = DefaultCalculatorStore()

    val state = store.state
    val effects = store.effects

    fun dispatch(intent: CalculatorIntent) = store.dispatch(intent)

    /** Activity finished for good -> close the effect channel. */
    override fun onCleared() = store.close()
}
