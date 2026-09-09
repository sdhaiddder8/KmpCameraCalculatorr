// KMP Calculator + Camera
// Author: Danish Hussain

import AVFoundation
import UIKit
import Shared

/// iOS implementation of the shared `CameraController` interface.
///
/// Note the Swift-side names of the Kotlin sealed subclasses: `CameraCaptureResult.Captured`
/// in Kotlin becomes `CameraCaptureResultCaptured` in Swift (Obj-C interop flattens nested
/// types with a prefix). The shared reducer maps each of these to an intent.
final class IosCameraController: NSObject, CameraController {

    // UIImagePickerController only keeps a weak reference to its delegate, so we must hold
    // the delegate ourselves for the lifetime of the picker.
    private var pickerDelegate: PickerDelegate?

    func capture(onResult: @escaping (any CameraCaptureResult) -> Void) {
        #if targetEnvironment(simulator)
        // The Simulator has no camera. Compile in a fake ONLY for that target so the MVI
        // flow (launch -> captured -> preview) is still demoable.
        captureDebugFake(onResult: onResult)
        #else
        captureReal(onResult: onResult)
        #endif
    }

    func release(reference: PhotoReference) {
        // Delete a photo that a newer capture has replaced (reducer emitted ReleasePhoto).
        try? FileManager.default.removeItem(atPath: reference.filePath)
    }

    // MARK: - Real device path

    private func captureReal(onResult: @escaping (any CameraCaptureResult) -> Void) {
        guard UIImagePickerController.isSourceTypeAvailable(.camera) else {
            onResult(CameraCaptureResultUnavailable())
            return
        }
        // Map the AVFoundation permission states onto our shared result type.
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            presentPicker(onResult: onResult)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async {   // completion is on an arbitrary queue; UI needs main
                    granted ? self.presentPicker(onResult: onResult)
                            : onResult(CameraCaptureResultPermissionDenied())
                }
            }
        case .denied, .restricted:
            onResult(CameraCaptureResultPermissionDenied())
        @unknown default:
            onResult(CameraCaptureResultPermissionDenied())
        }
    }

    private func presentPicker(onResult: @escaping (any CameraCaptureResult) -> Void) {
        guard let top = UIApplication.shared.topViewController else {
            onResult(CameraCaptureResultFailed())
            return
        }
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        let delegate = PickerDelegate { [weak self] image in
            self?.pickerDelegate = nil   // release the retained delegate
            guard let image else {
                onResult(CameraCaptureResultCancelled())
                return
            }
            self?.finish(with: image, onResult: onResult)
        }
        picker.delegate = delegate
        self.pickerDelegate = delegate
        top.present(picker, animated: true)
    }

    private func finish(with image: UIImage, onResult: @escaping (any CameraCaptureResult) -> Void) {
        // PhotoStore writes the JPEG and returns the shared PhotoReference.
        if let reference = PhotoStore.save(image) {
            onResult(CameraCaptureResultCaptured(reference: reference))
        } else {
            onResult(CameraCaptureResultFailed())
        }
    }

    // MARK: - Simulator fake

    private func captureDebugFake(onResult: @escaping (any CameraCaptureResult) -> Void) {
        guard let image = DebugPhoto.generate(),
              let reference = PhotoStore.save(image) else {
            onResult(CameraCaptureResultUnavailable())
            return
        }
        // Small delay so the "Opening camera…" launching state is actually visible.
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            onResult(CameraCaptureResultCaptured(reference: reference))
        }
    }
}

/// Bridges the `UIImagePickerController` delegate callbacks to a single `(UIImage?) -> Void`
/// completion (nil = the user cancelled).
private final class PickerDelegate: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    private let completion: (UIImage?) -> Void

    init(completion: @escaping (UIImage?) -> Void) {
        self.completion = completion
    }

    func imagePickerController(
        _ picker: UIImagePickerController,
        didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
    ) {
        let image = info[.originalImage] as? UIImage
        picker.dismiss(animated: true) { self.completion(image) }
    }

    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true) { self.completion(nil) }
    }
}

/// Finds the currently visible view controller to present the camera from — there's no
/// single "root" once sheets/navigation are involved, so we walk the presentation chain.
private extension UIApplication {
    var topViewController: UIViewController? {
        let scene = connectedScenes.first { $0.activationState == .foregroundActive } as? UIWindowScene
        var top = scene?.keyWindow?.rootViewController
            ?? scene?.windows.first?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}
