#!/usr/bin/env ruby
# KMP Calculator + Camera
# Author: Danish Hussain
#
# Generates iosApp.xcodeproj from scratch instead of checking a huge, merge-hostile
# .pbxproj into git. Run `ruby project.rb` after `gem install xcodeproj`, then open the
# generated project. Regenerate whenever you add/rename a Swift file below.

require "xcodeproj"
require "fileutils"

root = File.dirname(__FILE__)
project_path = File.join(root, "iosApp.xcodeproj")
FileUtils.rm_rf(project_path)                 # always rebuild fresh
project = Xcodeproj::Project.new(project_path)
project.root_object.attributes["ORGANIZATIONNAME"] = "Danish Hussain"

target = project.new_target(:application, "iosApp", :ios, "16.0")

# The Swift sources of the iOS app. Keep this list in sync with the files on disk.
group = project.new_group("iosApp", "iosApp")
%w[iOSApp.swift ContentView.swift CalculatorViewStore.swift IosCameraController.swift PhotoStore.swift].each do |name|
  file_ref = group.new_reference(name)
  target.add_file_references([file_ref])
end
group.new_reference("Info.plist")

# Build phase that compiles the Kotlin :shared module into Shared.framework and drops it
# where Xcode's linker can find it (see FRAMEWORK_SEARCH_PATHS below). Moving it to index 0
# means it runs BEFORE "Compile Sources", so `import Shared` always resolves.
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
  s["GENERATE_INFOPLIST_FILE"] = "NO"              # we ship our own Info.plist
  s["SWIFT_VERSION"] = "5.0"
  s["IPHONEOS_DEPLOYMENT_TARGET"] = "16.0"
  s["TARGETED_DEVICE_FAMILY"] = "1"                # iPhone only
  s["ASSETCATALOG_COMPILER_APPICON_NAME"] = ""     # no asset catalog / app icon in this sample
  # Where the Gradle build phase places Shared.framework, and the link flag to pull it in.
  s["FRAMEWORK_SEARCH_PATHS"] = ["$(inherited)", "$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)"]
  s["OTHER_LDFLAGS"] = ["$(inherited)", "-framework", "Shared"]

  # Ad-hoc signing: runs on the Simulator with no Apple account. Set a real team to run on device.
  s["CODE_SIGN_STYLE"] = "Automatic"
  s["CODE_SIGN_IDENTITY"] = "-"
  s["CODE_SIGNING_REQUIRED"] = "NO"
  s["CODE_SIGNING_ALLOWED"] = "YES"
end

project.save
puts "Wrote #{project_path}"
