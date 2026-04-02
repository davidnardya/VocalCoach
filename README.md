This is a Kotlin Multiplatform project targeting Android, iOS.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run iOS Application

The iOS shell lives under [/iosApp](./iosApp). The shared Kotlin framework is built by Gradle and vendored through CocoaPods; Xcode must see an up-to-date `ComposeApp.framework` under `composeApp/build/cocoapods/framework/`. **Run the following from the repository root whenever you pull changes, switch branches, or edit shared Kotlin / iOS pod integration**, then build in Xcode (or run the same Gradle link task before a CLI build).

1. **Link the Kotlin framework** for the configuration you use in Xcode (most often Debug on an Apple Silicon simulator):

   ```shell
   ./gradlew :composeApp:linkPodDebugFrameworkIosSimulatorArm64
   ```

   For a **physical device** (Debug):

   ```shell
   ./gradlew :composeApp:linkPodDebugFrameworkIosArm64
   ```

   For **Release** builds, use the matching `linkPodReleaseFramework…` tasks for `iosSimulatorArm64` or `iosArm64` instead.

2. **Install CocoaPods dependencies** (updates `Pods/` and applies Podfile `post_install` patches):

   ```shell
   cd iosApp && pod install
   ```

3. **Open the workspace**, not the project file:

   ```text
   iosApp/iosApp.xcworkspace
   ```

4. In Xcode, **Product → Clean Build Folder** if you still see stale Swift or linker errors after the steps above.

Then run the app from Xcode’s run button or your IDE’s iOS run configuration.

**Note:** TensorFlow Lite Task Audio does not ship an Apple Silicon simulator binary in the CocoaPods release this project uses. On the **simulator**, the app links without that framework and uses a stub classifier (no live model output); use a **device** build for full audio classification.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…