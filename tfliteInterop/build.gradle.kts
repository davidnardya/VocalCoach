plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    // TensorFlowLiteTaskAudio CocoaPods only ships device arm64 + Intel-simulator slices — no
    // arm64 for Apple Silicon simulator. TF cinterop is iosArm64-only; vcaudio is used on both.
    iosArm64()
    iosSimulatorArm64()

    iosArm64().compilations.getByName("main") {
        cinterops {
            val TensorFlowLiteTaskAudio by creating {
                val frameworksDir =
                    rootProject.layout.projectDirectory.dir("iosApp/Pods/TensorFlowLiteTaskAudio/Frameworks")
                definitionFile.set(layout.projectDirectory.file("src/nativeInterop/tflpod/TensorFlowLiteTaskAudio.def"))
                packageName("cocoapods.TensorFlowLiteTaskAudio")
                compilerOpts("-F", frameworksDir.asFile.absolutePath)
            }
            val vcaudio by creating {
                definitionFile.set(layout.projectDirectory.file("src/nativeInterop/vcaudio/vcaudio.def"))
                includeDirs(layout.projectDirectory.dir("src/nativeInterop/vcaudio"))
            }
        }
    }

    iosSimulatorArm64().compilations.getByName("main") {
        cinterops {
            val vcaudio by creating {
                definitionFile.set(layout.projectDirectory.file("src/nativeInterop/vcaudio/vcaudio.def"))
                includeDirs(layout.projectDirectory.dir("src/nativeInterop/vcaudio"))
            }
        }
    }
}
