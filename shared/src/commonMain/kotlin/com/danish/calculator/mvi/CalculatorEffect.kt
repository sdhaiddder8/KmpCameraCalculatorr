// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.mvi

import com.danish.calculator.model.PhotoReference

sealed interface CalculatorEffect {
    data object LaunchCamera : CalculatorEffect
    data class ReleasePhoto(val reference: PhotoReference) : CalculatorEffect
}
