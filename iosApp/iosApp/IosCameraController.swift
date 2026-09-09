// KMP Calculator + Camera
// Author: Danish Hussain

import AVFoundation
import UIKit
import Shared

final class IosCameraController: NSObject, CameraController {

    private var pickerDelegate: PickerDelegate?

    func capture(onResult: @escaping (any CameraCaptureResult) -> Void) {
        #if targetEnvironment(simulator)
        captureDebugFake(onResult: onResult)
        #else
        captureReal(onResult: onResult)
        #endif
    }

    func release(reference: PhotoReference) {
        try? FileManager.default.removeItem(atPath: reference.filePath)
    }

    private func captureReal(onResult: @escaping (any CameraCaptureResult) -> Void) {
        guard UIImagePickerController.isSourceTypeAvailable(.camera) else {
            onResult(CameraCaptureResultUnavailable())
            return
        }
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            presentPicker(onResult: onResult)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async {
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
            self?.pickerDelegate = nil
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
        if let reference = PhotoStore.save(image) {
            onResult(CameraCaptureResultCaptured(reference: reference))
        } else {
            onResult(CameraCaptureResultFailed())
        }
    }

    private func captureDebugFake(onResult: @escaping (any CameraCaptureResult) -> Void) {
        guard let image = DebugPhoto.generate(),
              let reference = PhotoStore.save(image) else {
            onResult(CameraCaptureResultUnavailable())
            return
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            onResult(CameraCaptureResultCaptured(reference: reference))
        }
    }
}

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
