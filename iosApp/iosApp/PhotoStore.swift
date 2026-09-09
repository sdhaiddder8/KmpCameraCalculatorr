// KMP Calculator + Camera
// Author: Danish Hussain

import UIKit
import Shared

enum PhotoStore {

    static func save(_ image: UIImage) -> PhotoReference? {
        guard let data = image.jpegData(compressionQuality: 0.9) else { return nil }
        do {
            let dir = try photosDirectory()
            let url = dir.appendingPathComponent("capture_\(Int(Date().timeIntervalSince1970 * 1000)).jpg")
            try data.write(to: url, options: .atomic)
            return PhotoReference(
                filePath: url.path,
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

    private static func photosDirectory() throws -> URL {
        let base = try FileManager.default.url(
            for: .cachesDirectory, in: .userDomainMask, appropriateFor: nil, create: true
        )
        let dir = base.appendingPathComponent("photos", isDirectory: true)
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }
}

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
