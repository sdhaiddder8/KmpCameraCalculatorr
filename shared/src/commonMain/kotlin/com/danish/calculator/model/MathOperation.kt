// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.model

/**
 * The four operations the calculator supports.
 *
 * Why an enum in `commonMain`:
 *  - Both Android and iOS need the exact same set of operations, so it lives in shared code.
 *  - The glyph ("+", "-", ...) travels WITH the operation via [symbol], so neither UI has to
 *    keep its own mapping of operation -> character. Only the long human labels
 *    ("Addition", ...) are built per platform, because those are UI-facing strings.
 *  - Kotlin/Native exposes this to Swift as `MathOperation` with `.entries` available,
 *    so the SwiftUI dropdown can iterate the same source of truth.
 *
 * Note: SUBTRACT uses U+2212 (the real "minus sign"), not the ASCII hyphen "-", and
 * MULTIPLY/DIVIDE use the proper typographic signs so the UI reads like a real calculator.
 */
enum class MathOperation(val symbol: String) {
    ADD("+"),
    SUBTRACT("−"),
    MULTIPLY("×"),
    DIVIDE("÷"),
}
