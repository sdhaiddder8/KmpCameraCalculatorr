// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danish.calculator.camera.CameraController
import com.danish.calculator.camera.toIntent
import com.danish.calculator.model.CalculatorState
import com.danish.calculator.model.MathOperation
import com.danish.calculator.mvi.CalculatorEffect
import com.danish.calculator.mvi.CalculatorIntent
import kotlinx.coroutines.flow.Flow

// Palette pulled out as constants so the same blue/grey is reused everywhere. The SwiftUI
// screen defines the identical RGB values — the two UIs are kept visually in sync by hand.
private val Primary = Color(0xFF2E6BE6)
private val ErrorRed = Color(0xFFD22F2F)
private val BorderGray = Color(0xFFC6C6C8)
private val LabelGray = Color(0xFF6C6C70)
private val FieldFill = Color(0xFFF3F3F7)
private val TitleColor = Color(0xFF1A1A1A)
private val FieldShape = RoundedCornerShape(12.dp)
private val FieldHeight = 52.dp

/**
 * The entire Android UI. A pure function of [state]: it only reads state and emits
 * [CalculatorIntent]s via [onIntent] — it never holds calculator state itself.
 *
 * @param effects hot stream of one-shot effects from the store
 * @param camera  platform camera, invoked when a [CalculatorEffect.LaunchCamera] arrives
 * @param onIntent the store's `dispatch`
 */
@Composable
fun CalculatorScreen(
    state: CalculatorState,
    effects: Flow<CalculatorEffect>,
    camera: CameraController,
    onIntent: (CalculatorIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Bridge effects -> platform work. Keyed on effects/camera so it restarts only if
    // those identities change, not on every recomposition. This is the Android equivalent
    // of what CalculatorComponent.init does for iOS.
    LaunchedEffect(effects, camera) {
        effects.collect { effect ->
            when (effect) {
                CalculatorEffect.LaunchCamera -> camera.capture { onIntent(it.toIntent()) }
                is CalculatorEffect.ReleasePhoto -> camera.release(effect.reference)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()) // keyboard can push content off-screen
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Calculator",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TitleColor,
            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
        )

        // Each field: shows the raw string from state, and every keystroke dispatches a
        // *Changed intent. There is no local mutable text state — state is the only truth.
        LabeledField("First number", state.firstNumberError) {
            TextInput(state.firstNumber) { onIntent(CalculatorIntent.FirstNumberChanged(it)) }
        }
        LabeledField("Second number", state.secondNumberError) {
            TextInput(state.secondNumber) { onIntent(CalculatorIntent.SecondNumberChanged(it)) }
        }
        LabeledField("Operation", state.operationError) {
            OperationField(state.selectedOperation) { onIntent(CalculatorIntent.OperationSelected(it)) }
        }

        PrimaryButton("Calculate") { onIntent(CalculatorIntent.CalculateClicked) }

        LabeledField("Result", state.resultError) {
            FieldBox(fill = FieldFill) {
                Text(state.result.orEmpty(), fontSize = 17.sp, color = Color.Black)
            }
        }

        // Button label is derived from state, not tracked separately.
        SecondaryButton(if (state.hasPhoto) "Retake photo" else "Open camera") {
            onIntent(CalculatorIntent.OpenCameraClicked)
        }
        state.cameraError?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }

        PhotoArea(state)
    }
}

/** Label + field + optional error message, the repeated vertical unit of the form. */
@Composable
private fun LabeledField(label: String, error: String?, field: @Composable () -> Unit) {
    Column {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
        Box(Modifier.height(6.dp))
        field()
        if (error != null) {
            Box(Modifier.height(6.dp))
            Text(error, fontSize = 13.sp, color = ErrorRed)
        }
    }
}

/** The rounded, bordered box shared by the text inputs, the dropdown and the result. */
@Composable
private fun FieldBox(
    fill: Color = Color.White,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var m = Modifier
        .fillMaxWidth()
        .height(FieldHeight)
        .background(fill, FieldShape)
        .border(1.dp, BorderGray, FieldShape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m.padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart, content = content)
}

@Composable
private fun TextInput(value: String, onValueChange: (String) -> Unit) {
    FieldBox {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 17.sp, color = Color.Black),
            cursorBrush = SolidColor(Primary),
            // Decimal keypad is only a hint — validation still fully parses the text,
            // because hardware keyboards / paste can produce anything.
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Tap-to-open dropdown of the four operations; iterates the shared `MathOperation.entries`. */
@Composable
private fun OperationField(selected: MathOperation?, onSelect: (MathOperation) -> Unit) {
    var expanded by remember { mutableStateOf(false) } // pure UI state, fine to keep local
    Box {
        FieldBox(onClick = { expanded = true }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selected?.display() ?: "Select operation",
                    fontSize = 17.sp,
                    color = if (selected == null) LabelGray.copy(alpha = 0.7f) else Color.Black,
                    modifier = Modifier.weight(1f),
                )
                Text("▾", fontSize = 15.sp, color = LabelGray)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MathOperation.entries.forEach { op ->
                DropdownMenuItem(
                    text = { Text(op.display()) },
                    onClick = { onSelect(op); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(FieldHeight)
            .background(Primary, FieldShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(FieldHeight)
            .border(1.dp, Primary, FieldShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Primary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

/**
 * The preview area below the button. Shows, in priority order:
 *  - the photo (current or the one being kept while the camera is open) via [displayPhoto]
 *  - a "opening camera" hint while launching
 *  - an empty-state message otherwise
 */
@Composable
private fun PhotoArea(state: CalculatorState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .border(1.dp, BorderGray, FieldShape),
        contentAlignment = Alignment.Center,
    ) {
        val photo = state.displayPhoto
        when {
            photo != null -> PhotoPreview(photo, Modifier.fillMaxWidth().padding(1.dp))
            state.isCameraLaunching -> Text("Opening camera…", color = LabelGray, fontSize = 15.sp)
            else -> Text("No photo captured", color = LabelGray, fontSize = 15.sp)
        }
    }
}

/**
 * The long human label for the dropdown. This mapping is per-platform on purpose — it's
 * the only user-facing string not shared — while the glyph comes from the shared enum.
 */
private fun MathOperation.display(): String {
    val name = when (this) {
        MathOperation.ADD -> "Addition"
        MathOperation.SUBTRACT -> "Subtraction"
        MathOperation.MULTIPLY -> "Multiplication"
        MathOperation.DIVIDE -> "Division"
    }
    return "$name  ($symbol)"
}
