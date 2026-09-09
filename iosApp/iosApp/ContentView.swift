// KMP Calculator + Camera
// Author: Danish Hussain

import SwiftUI
import Shared

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

struct ContentView: View {
    @StateObject private var store = CalculatorViewStore()

    var body: some View {
        let state = store.state
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                Text("Calculator")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(.brandTitle)
                    .padding(.top, 8)
                    .padding(.bottom, 2)

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
}

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
            TextField("", text: Binding(get: { value }, set: onChange))
                .font(.system(size: 17))
                .keyboardType(.numbersAndPunctuation)
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
