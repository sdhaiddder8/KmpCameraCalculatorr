// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator.android.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Text
import androidx.exifinterface.media.ExifInterface
import com.danish.calculator.model.PhotoReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Turns a shared [PhotoReference] (just a file path) into an actual on-screen image.
 * This decode step is exactly the platform-specific work the shared code refuses to do.
 *
 * The bitmap is decoded off the main thread and stored in local Compose state; until it's
 * ready a "Loading photo…" placeholder shows. Keying `remember`/`LaunchedEffect` on
 * `reference.filePath` means a new photo triggers a fresh decode and the old bitmap is
 * dropped.
 */
@Composable
fun PhotoPreview(reference: PhotoReference, modifier: Modifier = Modifier) {
    var bitmap by remember(reference.filePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(reference.filePath) {
        bitmap = withContext(Dispatchers.IO) { decode(reference.filePath) }
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Captured photo",
            contentScale = ContentScale.Fit,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(it.width.toFloat() / it.height),
        )
    } ?: Text("Loading photo…")
}

/**
 * Decode a JPEG to a right-side-up, memory-safe [Bitmap].
 *
 *  - Downsample: reads the header first (`inJustDecodeBounds`) and picks a power-of-two
 *    `inSampleSize` so the loaded bitmap never exceeds [maxSize] on either edge — a
 *    full-res camera photo would otherwise risk an OutOfMemoryError.
 *  - Rotate: camera photos are often stored landscape with an EXIF "orientation" tag
 *    instead of physically rotated pixels. We read that tag and rotate the bitmap so it
 *    displays the way it was shot.
 */
private fun decode(path: String, maxSize: Int = 1600): Bitmap? {
    val file = File(path)
    if (!file.exists() || file.length() == 0L) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds) // fills bounds.outWidth/outHeight, allocates nothing
    if (bounds.outWidth <= 0) return null

    var sample = 1
    while (bounds.outWidth / sample > maxSize || bounds.outHeight / sample > maxSize) sample *= 2

    val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return null

    val degrees = when (runCatching {
        ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> return bitmap // already upright (or unknown) — nothing to do
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
}
