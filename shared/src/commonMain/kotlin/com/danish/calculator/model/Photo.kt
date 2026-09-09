// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.model

data class PhotoReference(
    val filePath: String,
    val widthPx: Int,
    val heightPx: Int,
    val capturedAtEpochMs: Long,
)

sealed interface PhotoStatus {
    data object Empty : PhotoStatus
    data class Launching(val previous: PhotoReference?) : PhotoStatus
    data class Captured(val reference: PhotoReference) : PhotoStatus
}
