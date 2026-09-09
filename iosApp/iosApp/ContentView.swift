// KMP Calculator + Camera
// Author: Danish Hussain

import SwiftUI
import Shared

// Brand palette. These RGB values match the Android `CalculatorScreen` constants exactly —
// the two native UIs are kept in visual sync by hand, not by shared code.
private extension Color {
    static let brandPrimary = Color(red: 46 / 255, green: 107 / 255, blue: 230 / 255)
    static let brandError = Color(red: 210 / 255, green: 47 / 255, blue: 47 / 255)
    static let brandBorder = Color(red: 198 / 255, green: 198 / 255, blue: 200 / 255)
    static let brandLabel = Color(red: 108 / 255, green: 108 / 255, blue: 112 / 255)
    static let brandFieldFill = Color(red: 243 / 255, green: 243 / 255, blue: 247 / 255)
    static let brandTitle = Color(red: 26 / 255, green: 26 / 255, blue: 26 / 255)
}

private let fieldShape = RoundedRectangle(cornerRadius: 12, style: .continuous)
private let fieldHeight: CGFloat = 52

/// The entire iOS UI. Like the Android screen, it is a pure function of `store.state` and
/// only communicates back by dispatching intents — no local calculator state.
///
/// Kotlin intent classes appear here with their flattened Swift names, e.g.
/// `CalculatorIntent.FirstNumberChanged` -> `CalculatorIntentFirstNumberChanged`.
struct ContentView: View {
    // @StateObject: created once and kept for the view's lifetime. It builds and owns the
    // shared CalculatorComponent.
    @StateObject private var store = CalculatorViewStore()

    var body: some View {
        let state = store.state
        ScrollView {   // keyboard can cover the lower fields
            VStack(alignment: .leading, spacing: 18) {
                Text("Calculator")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(.brandTitle)
                    .padding(.top, 8)
                    .padding(.bottom, 2)

                // Each field renders state.<x> and dispatches a *Changed intent on edit.
                LabeledField(label: "First number", error: state.firstNumberError) {
                    TextInput(value: state.firstNumber) {
                        store.dispatch(CalculatorIntentFirstNumberChanged(value: $0))
                    }
                }
                LabeledField(label: "Second number", error: state.secondNumberError) {
                    TextInput(value: state.secondNumber) {
                        store.dispatch(CalculatorIntentSecondNumberChanged(value: $0))
                    }
                }
                LabeledField(label: "Operation", error: state.operationError) {
                    OperationField(selected: state.selectedOperation) {
                        store.dispatch(CalculatorIntentOperationSelected(operation: $0))
                    }
                }

                PrimaryButton(title: "Calculate") {
                    store.dispatch(CalculatorIntentCalculateClicked())
                }

                LabeledField(label: "Result", error: state.resultError) {
                    FieldBox(fill: .brandFieldFill) {
                        Text(state.result ?? "").font(.system(size: 17))
                        Spacer(minLength: 0)
                    }
                }

                SecondaryButton(title: state.hasPhoto ? "Retake photo" : "Open camera") {
                    store.dispatch(CalculatorIntentOpenCameraClicked())
                }
                if let cameraError = state.cameraError {
                    Text(cameraError).font(.system(size: 13)).foregroundColor(.brandError)
                }

                PhotoArea(state: state)
            }
            .padding(20)
        }
        .background(Color.white)
    }
    // Note: unlike Android, there is no "collect effects" code in the view. The shared
    // CalculatorComponent already runs the camera when a LaunchCamera effect is emitted.
}

/// Label + field + optional error — the repeated form row (mirrors Android's LabeledField).
private struct LabeledField<Content: View>: View {
    let label: String
    let error: String?
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label).font(.system(size: 13, weight: .medium)).foregroundColor(.brandLabel)
            content
            if let error {
                Text(error).font(.system(size: 13)).foregroundColor(.brandError)
            }
        }
    }
}

/// Rounded bordered container shared by inputs, dropdown and result box.
private struct FieldBox<Content: View>: View {
    var fill: Color = .white
    @ViewBuilder var content: Content

    var body: some View {
        HStack(spacing: 8) { content }
            .padding(.horizontal, 14)
            .frame(height: fieldHeight)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(fill)
            .clipShape(fieldShape)
            .overlay(fieldShape.stroke(Color.brandBorder, lineWidth: 1))
    }
}

private struct TextInput: View {
    let value: String
    let onChange: (String) -> Void

    var body: some View {
        FieldBox {
            // Binding.get returns the state value, Binding.set forwards to onChange — the
            // text field never stores its own copy of the string.
            TextField("", text: Binding(get: { value }, set: onChange))
                .font(.system(size: 17))
                .keyboardType(.numbersAndPunctuation) // hint only; shared validator still parses
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
        }
    }
}

private struct OperationField: View {
    let selected: MathOperation?
    let onSelect: (MathOperation) -> Void

    var body: some View {
        Menu {
            // MathOperation.entries comes straight from the shared Kotlin enum.
            ForEach(MathOperation.entries, id: \.self) { op in
                Button(op.display()) { onSelect(op) }
            }
        } label: {
            FieldBox {
                Text(selected?.display() ?? "Select operation")
                    .font(.system(size: 17))
                    .foregroundColor(selected == nil ? Color.brandLabel.opacity(0.7) : .black)
                Spacer(minLength: 0)
                Text("▾").font(.system(size: 15)).foregroundColor(.brandLabel)
            }
        }
        .buttonStyle(.plain)
    }
}

private struct PrimaryButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: fieldHeight)
                .background(Color.brandPrimary)
                .clipShape(fieldShape)
        }
        .buttonStyle(.plain)
    }
}

private struct SecondaryButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.brandPrimary)
                .frame(maxWidth: .infinity)
                .frame(height: fieldHeight)
                .overlay(fieldShape.stroke(Color.brandPrimary, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

/// Preview area: photo (current or kept-while-launching), a launching hint, or empty state.
private struct PhotoArea: View {
    let state: CalculatorState

    var body: some View {
        ZStack {
            if let reference = state.displayPhoto, let image = PhotoStore.image(for: reference) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: .infinity)
            } else if state.isCameraLaunching {
                Text("Opening camera…").font(.system(size: 15)).foregroundColor(.brandLabel)
            } else {
                Text("No photo captured").font(.system(size: 15)).foregroundColor(.brandLabel)
            }
        }
        .frame(maxWidth: .infinity, minHeight: 220)
        .clipShape(fieldShape)
        .overlay(fieldShape.stroke(Color.brandBorder, lineWidth: 1))
    }
}

/// Long human label for the dropdown (per-platform, like Android's `display()`); the glyph
/// in parentheses is the shared `symbol`. The `default` case is required because the
/// Obj-C-imported enum isn't seen as exhaustive by Swift.
private extension MathOperation {
    func display() -> String {
        let name: String
        switch self {
        case .add: name = "Addition"
        case .subtract: name = "Subtraction"
        case .multiply: name = "Multiplication"
        case .divide: name = "Division"
        default: name = self.name
        }
        return "\(name)  (\(symbol))"
    }
}
