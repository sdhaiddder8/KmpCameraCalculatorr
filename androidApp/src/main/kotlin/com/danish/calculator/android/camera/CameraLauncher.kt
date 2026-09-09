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

@Composable
fun rememberCameraController(): CameraController {
    val context = LocalContext.current.applicationContext
    val pending = remember { PendingCallbacks() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> pending.permission?.invoke(granted); pending.permission = null }

    val captureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved -> pending.capture?.invoke(saved); pending.capture = null }

    return remember {
        object : CameraController {
            override fun capture(onResult: (CameraCaptureResult) -> Unit) {
                if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                    onResult(CameraCaptureResult.Unavailable)
                    return
                }
                pending.permission = { granted ->
                    if (!granted) {
                        onResult(CameraCaptureResult.PermissionDenied)
                    } else {
                        val file = File(context.cacheDir, "photos").apply { mkdirs() }
                            .let { File(it, "capture_${System.currentTimeMillis()}.jpg") }
                        pending.capture = { saved -> onResult(resolve(file, saved)) }
                        captureLauncher.launch(uriFor(context, file))
                    }
                }
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }

            override fun release(reference: PhotoReference) {
                runCatching { File(reference.filePath).delete() }
            }
        }
    }
}

private class PendingCallbacks {
    var permission: ((Boolean) -> Unit)? = null
    var capture: ((Boolean) -> Unit)? = null
}

private fun uriFor(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

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
