// KMP Calculator + Camera
// Author: Danish Hussain

import UIKit
import Shared

/// The iOS side of photo persistence — the platform-specific work the shared code avoids.
///
/// `save` writes a `UIImage` to a JPEG in the app's Caches directory and returns the
/// shared `PhotoReference` describing it. `image(for:)` is the reverse, used by the UI.
enum PhotoStore {

    static func save(_ image: UIImage) -> PhotoReference? {
        guard let data = image.jpegData(compressionQuality: 0.9) else { return nil }
        do {
            let dir = try photosDirectory()
            // Millisecond timestamp keeps each capture's filename unique.
            let url = dir.appendingPathComponent("capture_\(Int(Date().timeIntervalSince1970 * 1000)).jpg")
            try data.write(to: url, options: .atomic)
            return PhotoReference(
                filePath: url.path,
                // `size` is in points; multiply by `scale` for real pixels. Int32 because
                // that's what the Kotlin `Int` maps to in Swift.
                widthPx: Int32(image.size.width * image.scale),
                heightPx: Int32(image.size.height * image.scale),
                capturedAtEpochMs: Int64(Date().timeIntervalSince1970 * 1000)
            )
        } catch {
            return nil
        }
    }

    static func image(for reference: PhotoReference) -> UIImage? {
        UIImage(contentsOfFile: reference.filePath)
    }

    /// `Caches/photos`, created on demand. Caches (not Documents) because these photos are
    /// disposable — the OS may reclaim the space and the reducer deletes replaced files.
    private static func photosDirectory() throws -> URL {
        let base = try FileManager.default.url(
            for: .cachesDirectory, in: .userDomainMask, appropriateFor: nil, create: true
        )
        let dir = base.appendingPathComponent("photos", isDirectory: true)
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }
}

/// Generates a placeholder image for the Simulator (which has no camera): a teal field
/// with "DEBUG PHOTO" and a timestamp, so captures are visually distinct.
enum DebugPhoto {
    static func generate() -> UIImage? {
        let size = CGSize(width: 900, height: 1200)
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { ctx in
            UIColor.systemTeal.setFill()
            ctx.fill(CGRect(origin: .zero, size: size))

            let text = "DEBUG PHOTO\n\(Date().formatted(date: .abbreviated, time: .standard))"
            let style = NSMutableParagraphStyle()
            style.alignment = .center
            let attrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.boldSystemFont(ofSize: 48),
                .foregroundColor: UIColor.white,
                .paragraphStyle: style,
            ]
            let rect = CGRect(x: 40, y: size.height / 2 - 80, width: size.width - 80, height: 200)
            (text as NSString).draw(in: rect, withAttributes: attrs)
        }
    }
}
