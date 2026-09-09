// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.android.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.danish.calculator.camera.CameraController
import com.danish.calculator.camera.CameraCaptureResult
import com.danish.calculator.model.PhotoReference
import java.io.File

/**
 * Android implementation of the shared [CameraController] interface.
 *
 * It has to be a @Composable factory because `rememberLauncherForActivityResult` can only
 * be called during composition — that's how Compose registers the permission dialog and
 * the `ACTION_IMAGE_CAPTURE` result handlers with the Activity.
 *
 * Flow when `capture()` is called:
 *   1. Is there any camera hardware? If not -> Unavailable.
 *   2. Ask for CAMERA permission. Denied -> PermissionDenied.
 *   3. Create an empty target file in cacheDir/photos and launch TakePicture into it.
 *   4. On return: file has bytes -> Captured(reference); nothing saved -> treat as Cancelled.
 */
@Composable
fun rememberCameraController(): CameraController {
    val context = LocalContext.current.applicationContext
    // The ActivityResult callbacks fire later and separately, so we stash the
    // "what to do next" lambdas here and invoke them when each result arrives.
    val pending = remember { PendingCallbacks() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> pending.permission?.invoke(granted); pending.permission = null }

    val captureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(), // writes the full-res photo to the Uri we pass
    ) { saved -> pending.capture?.invoke(saved); pending.capture = null }

    // remember { } so the same CameraController instance is reused across recompositions.
    return remember {
        object : CameraController {
            override fun capture(onResult: (CameraCaptureResult) -> Unit) {
                if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                    onResult(CameraCaptureResult.Unavailable)
                    return
                }
                // Step 2: request permission; the rest runs in this callback.
                pending.permission = { granted ->
                    if (!granted) {
                        onResult(CameraCaptureResult.PermissionDenied)
                    } else {
                        // Step 3: unique file under the app cache, then launch the camera.
                        val file = File(context.cacheDir, "photos").apply { mkdirs() }
                            .let { File(it, "capture_${System.currentTimeMillis()}.jpg") }
                        pending.capture = { saved -> onResult(resolve(file, saved)) }
                        captureLauncher.launch(uriFor(context, file))
                    }
                }
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }

            override fun release(reference: PhotoReference) {
                // Best-effort delete of a replaced photo; failure is harmless (cache dir).
                runCatching { File(reference.filePath).delete() }
            }
        }
    }
}

/** Holds the deferred continuations for the two async launcher results. */
private class PendingCallbacks {
    var permission: ((Boolean) -> Unit)? = null
    var capture: ((Boolean) -> Unit)? = null
}

/**
 * `ACTION_IMAGE_CAPTURE` needs a content:// URI it can write to; a raw file path throws
 * `FileUriExposedException` on modern Android. FileProvider (declared in the manifest +
 * res/xml/file_paths.xml) grants the camera app temporary write access to our file.
 */
private fun uriFor(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

/**
 * Turn the launcher's boolean + the file on disk into a result.
 *
 * `ACTION_IMAGE_CAPTURE` can't distinguish "user cancelled" from a rare "capture failed",
 * so anything that leaves no file (or an empty one) is reported as [Cancelled]. When a
 * real photo exists we read just its dimensions (`inJustDecodeBounds`, no full decode)
 * for the [PhotoReference].
 */
private fun resolve(file: File, saved: Boolean): CameraCaptureResult {
    if (!saved || !file.exists() || file.length() == 0L) {
        file.delete()
        return CameraCaptureResult.Cancelled
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching { BitmapFactory.decodeFile(file.absolutePath, bounds) }
    return CameraCaptureResult.Captured(
        PhotoReference(
            filePath = file.absolutePath,
            widthPx = bounds.outWidth.coerceAtLeast(0),
            heightPx = bounds.outHeight.coerceAtLeast(0),
            capturedAtEpochMs = System.currentTimeMillis(),
        ),
    )
}
