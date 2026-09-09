// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.mvi

import com.danish.calculator.model.PhotoReference

/**
 * One-shot side effects the reducer asks the outside world to perform.
 *
 * Why these are NOT part of the state:
 *  - State is re-read on every recomposition and after a rotation. If "open the camera"
 *    lived in the state, rotating the phone would reopen the camera.
 *  - An effect must happen exactly once. The store delivers these over a `Channel`
 *    (single consumer, consumed once), not a `StateFlow`.
 *
 *  - [LaunchCamera]  -> "start the platform camera now"
 *  - [ReleasePhoto]  -> "delete this now-replaced file"; emitted by the reducer when a new
 *                       photo supersedes an old one, so we don't leak files in the cache dir.
 */
sealed interface CalculatorEffect {
    data object LaunchCamera : CalculatorEffect
    data class ReleasePhoto(val reference: PhotoReference) : CalculatorEffect
}
