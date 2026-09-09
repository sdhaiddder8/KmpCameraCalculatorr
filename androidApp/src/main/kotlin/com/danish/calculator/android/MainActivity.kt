// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danish.calculator.android.camera.rememberCameraController
import com.danish.calculator.android.ui.CalculatorScreen

/**
 * The whole Android app is one Activity hosting one Compose screen.
 *
 * Responsibilities here are deliberately minimal:
 *  1. Obtain the [CalculatorStoreHolder] `ViewModel` (survives rotation, owns the shared store).
 *  2. Observe its state as Compose state.
 *  3. Hand state + effects + a camera implementation + the dispatch function to
 *     [CalculatorScreen]. The screen never sees the ViewModel directly.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // viewModel() returns the same instance across configuration changes.
                val store: CalculatorStoreHolder = viewModel()
                // collectAsStateWithLifecycle: stops collecting when the app is backgrounded.
                val state by store.state.collectAsStateWithLifecycle()

                Scaffold(containerColor = Color.White) { padding ->
                    CalculatorScreen(
                        state = state,
                        effects = store.effects,
                        // rememberCameraController wires the Compose ActivityResult launchers.
                        camera = rememberCameraController(),
                        onIntent = store::dispatch,
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}
