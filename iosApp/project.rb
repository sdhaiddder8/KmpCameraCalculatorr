#!/usr/bin/env ruby
# KMP Calculator + Camera
# Author: Danish Hussain
require "xcodeproj"
require "fileutils"

root = File.dirname(__FILE__)
project_path = File.join(root, "iosApp.xcodeproj")
FileUtils.rm_rf(project_path)
project = Xcodeproj::Project.new(project_path)
project.root_object.attributes["ORGANIZATIONNAME"] = "Danish Hussain"

target = project.new_target(:application, "iosApp", :ios, "16.0")

group = project.new_group("iosApp", "iosApp")
%w[iOSApp.swift ContentView.swift CalculatorViewStore.swift IosCameraController.swift PhotoStore.swift].each do |name|
  file_ref = group.new_reference(name)
  target.add_file_references([file_ref])
end
group.new_reference("Info.plist")

phase = target.new_shell_script_build_phase("Compile Kotlin Framework")
phase.shell_script = <<~SH
  if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
    echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \\"YES\\""
    exit 0
  fi
  cd "$SRCROOT/.."
  ./gradlew :shared:embedAndSignAppleFrameworkForXcode
SH
target.build_phases.move(phase, 0)

target.build_configurations.each do |config|
  s = config.build_settings
  s["PRODUCT_BUNDLE_IDENTIFIER"] = "com.danish.calculator.ios"
  s["PRODUCT_NAME"] = "iosApp"
  s["INFOPLIST_FILE"] = "iosApp/Info.plist"
  s["GENERATE_INFOPLIST_FILE"] = "NO"
  s["SWIFT_VERSION"] = "5.0"
  s["IPHONEOS_DEPLOYMENT_TARGET"] = "16.0"
  s["TARGETED_DEVICE_FAMILY"] = "1"
  s["ASSETCATALOG_COMPILER_APPICON_NAME"] = ""
  s["FRAMEWORK_SEARCH_PATHS"] = ["$(inherited)", "$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)"]
  s["OTHER_LDFLAGS"] = ["$(inherited)", "-framework", "Shared"]

  s["CODE_SIGN_STYLE"] = "Automatic"
  s["CODE_SIGN_IDENTITY"] = "-"
  s["CODE_SIGNING_REQUIRED"] = "NO"
  s["CODE_SIGNING_ALLOWED"] = "YES"
end

project.save
puts "Wrote #{project_path}"
