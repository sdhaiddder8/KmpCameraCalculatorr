// KMP Calculator + Camera
// Author: Danish Hussain

import Foundation
import Shared   // the Kotlin/Native framework produced by the :shared module

/// SwiftUI-facing wrapper around the shared Kotlin `CalculatorComponent`.
///
/// It is the iOS counterpart of Android's `CalculatorStoreHolder`: it holds no logic,
/// it just mirrors the shared `state` into a `@Published` property and forwards
/// `dispatch`. Everything the calculator *does* happens in shared Kotlin.
///
/// `@MainActor` because SwiftUI state must be touched on the main thread, and the shared
/// component is created with `Dispatchers.Main` so its callbacks already land there.
@MainActor
final class CalculatorViewStore: ObservableObject {

    /// The current shared state. `private(set)` — views change it only by dispatching intents.
    @Published private(set) var state: CalculatorState

    private let component: CalculatorComponent
    private var subscription: Cancellable?   // Kotlin `fun interface`, cancelled in deinit

    /// - Parameter camera: injected so tests / previews can pass a fake. Defaults to the
    ///   real `IosCameraController`.
    init(camera: CameraController = IosCameraController()) {
        // Kotlin top-level function `createCalculatorComponent` is exported to Swift on the
        // synthetic `IosCalculatorFactoryKt` class.
        component = IosCalculatorFactoryKt.createCalculatorComponent(camera: camera)

        // Kotlin `StateFlow.value` is exposed as a plain property. The force-cast is
        // needed because the generic type comes across as `Any`.
        state = component.state.value as! CalculatorState

        // Subscribe for later changes. The closure is called on Dispatchers.Main; the
        // extra `Task { @MainActor }` just satisfies Swift's concurrency checker.
        subscription = component.watchState { [weak self] newState in
            Task { @MainActor in self?.state = newState }
        }
    }

    func dispatch(_ intent: CalculatorIntent) {
        component.dispatch(intent: intent)
    }

    deinit {
        subscription?.cancel()   // stop observing the flow
        component.close()        // close the effect channel / cancel the coroutine scope
    }
}
