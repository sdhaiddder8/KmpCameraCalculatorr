// KMP Calculator + Camera
// Author: Danish Hussain

import SwiftUI

/// App entry point. `ContentView` owns the `CalculatorViewStore`, which owns the shared
/// Kotlin `CalculatorComponent`, so there is nothing to set up here.
@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
