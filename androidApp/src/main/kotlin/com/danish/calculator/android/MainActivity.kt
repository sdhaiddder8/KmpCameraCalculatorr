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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val store: CalculatorStoreHolder = viewModel()
                val state by store.state.collectAsStateWithLifecycle()

                Scaffold(containerColor = Color.White) { padding ->
                    CalculatorScreen(
                        state = state,
                        effects = store.effects,
                        camera = rememberCameraController(),
                        onIntent = store::dispatch,
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}
