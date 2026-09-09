// KMP Calculator + Camera
// Author: Danish Hussain

import Foundation
import Shared

@MainActor
final class CalculatorViewStore: ObservableObject {

    @Published private(set) var state: CalculatorState

    private let component: CalculatorComponent
    private var subscription: Cancellable?

    init(camera: CameraController = IosCameraController()) {
        component = IosCalculatorFactoryKt.createCalculatorComponent(camera: camera)
        state = component.state.value as! CalculatorState
        subscription = component.watchState { [weak self] newState in
            Task { @MainActor in self?.state = newState }
        }
    }

    func dispatch(_ intent: CalculatorIntent) {
        component.dispatch(intent: intent)
    }

    deinit {
        subscription?.cancel()
        component.close()
    }
}
