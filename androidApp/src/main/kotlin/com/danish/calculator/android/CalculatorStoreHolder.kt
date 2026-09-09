// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.android

import androidx.lifecycle.ViewModel
import com.danish.calculator.mvi.CalculatorIntent
import com.danish.calculator.mvi.DefaultCalculatorStore

class CalculatorStoreHolder : ViewModel() {

    private val store = DefaultCalculatorStore()

    val state = store.state
    val effects = store.effects

    fun dispatch(intent: CalculatorIntent) = store.dispatch(intent)

    override fun onCleared() = store.close()
}
