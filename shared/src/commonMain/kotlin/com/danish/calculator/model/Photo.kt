// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.model

/**
 * A platform-neutral handle to a captured photo.
 *
 * The shared code must never touch `Bitmap` / `UIImage` / the filesystem, so once a
 * platform saves the JPEG it hands back just this: where the file is, how big it is,
 * and when it was taken. Each UI layer turns [filePath] back into a real image.
 *
 * [widthPx] / [heightPx] are here so the UI can reserve the right aspect ratio before
 * the (potentially large) file is decoded off the main thread.
 */
data class PhotoReference(
    val filePath: String,
    val widthPx: Int,
    val heightPx: Int,
    val capturedAtEpochMs: Long,
)

/**
 * The camera portion of the state, modelled as a sealed hierarchy so illegal
 * combinations simply can't be represented.
 *
 *  - [Empty]     -> no photo has ever been taken
 *  - [Launching] -> the camera is open right now; [previous] remembers what to fall back
 *                   to if the user cancels or the capture fails
 *  - [Captured]  -> we have a photo at [reference]
 *
 * Using `data object` for the stateless cases means equality "just works" in tests
 * (`assertEquals(PhotoStatus.Empty, ...)`).
 */
sealed interface PhotoStatus {
    data object Empty : PhotoStatus
    data class Launching(val previous: PhotoReference?) : PhotoStatus
    data class Captured(val reference: PhotoReference) : PhotoStatus
}
