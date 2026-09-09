// KMP Calculator + Camera
// Author: Danish Hussain

package com.danish.calculator

import com.danish.calculator.model.PhotoStatus
import com.danish.calculator.mvi.CalculatorEffect
import com.danish.calculator.mvi.CalculatorIntent
import com.danish.calculator.validation.CalculatorErrors
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CameraFlowTest {

    @Test
    fun openCameraEmitsExactlyOneLaunchEffect() = runTest {
        val store = newStore()
        val recorder = recorderFor(store)

        store.dispatch(CalculatorIntent.OpenCameraClicked)

        assertEquals(listOf<CalculatorEffect>(CalculatorEffect.LaunchCamera), recorder.effects)
        assertTrue(store.state.value.isCameraLaunching)
    }

    @Test
    fun capturedResultShowsPhotoAndClearsError() = runTest {
        val store = newStore()
        store.dispatch(CalculatorIntent.OpenCameraClicked)
        store.dispatch(CalculatorIntent.PhotoCaptured(samplePhoto()))

        assertEquals(samplePhoto(), store.state.value.displayPhoto)
        assertNull(store.state.value.cameraError)
    }

    @Test
    fun cancelKeepsPreviousPhotoAndShowsNoError() = runTest {
        val store = newStore()
        store.dispatch(CalculatorIntent.PhotoCaptured(samplePhoto("/tmp/a.jpg")))

        store.dispatch(CalculatorIntent.OpenCameraClicked)
        store.dispatch(CalculatorIntent.PhotoCaptureCancelled)

        assertEquals("/tmp/a.jpg", store.state.value.displayPhoto?.filePath)
        assertNull(store.state.value.cameraError)
    }

    @Test
    fun cancelWithNoPreviousPhotoGoesBackToEmpty() = runTest {
        val store = newStore()
        store.dispatch(CalculatorIntent.OpenCameraClicked)
        store.dispatch(CalculatorIntent.PhotoCaptureCancelled)

        assertEquals(PhotoStatus.Empty, store.state.value.photoStatus)
        assertNull(store.state.value.cameraError)
    }

    @Test
    fun permissionDeniedShowsMessage() = runTest {
        val store = newStore()
        store.dispatch(CalculatorIntent.OpenCameraClicked)
        store.dispatch(CalculatorIntent.CameraPermissionDenied)

        assertEquals(CalculatorErrors.CAMERA_PERMISSION_REQUIRED, store.state.value.cameraError)
        assertEquals(PhotoStatus.Empty, store.state.value.photoStatus)
    }

    @Test
    fun cameraUnavailableShowsMessage() = runTest {
        val store = newStore()
        store.dispatch(CalculatorIntent.OpenCameraClicked)
        store.dispatch(CalculatorIntent.CameraUnavailable)

        assertEquals(CalculatorErrors.CAMERA_NOT_AVAILABLE, store.state.value.cameraError)
    }

    @Test
    fun captureFailedShowsMessage() = runTest {
        val store = newStore()
        store.dispatch(CalculatorIntent.OpenCameraClicked)
        store.dispatch(CalculatorIntent.PhotoCaptureFailed)

        assertEquals(CalculatorErrors.CAPTURE_FAILED, store.state.value.cameraError)
    }

    @Test
    fun replacingPhotoReleasesTheOldFile() = runTest {
        val store = newStore()
        val recorder = recorderFor(store)

        store.dispatch(CalculatorIntent.PhotoCaptured(samplePhoto("/tmp/old.jpg")))
        store.dispatch(CalculatorIntent.PhotoCaptured(samplePhoto("/tmp/new.jpg")))

        assertTrue(recorder.effects.any {
            it is CalculatorEffect.ReleasePhoto && it.reference.filePath == "/tmp/old.jpg"
        })
        assertEquals("/tmp/new.jpg", store.state.value.displayPhoto?.filePath)
    }
}
