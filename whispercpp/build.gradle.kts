plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}

// Standalone Android (com.android.library) module wrapping whisper.cpp's JNI bridge for the offline
// Whisper STT adapter on Android. It lives outside :feature:agent because that module uses the KMP
// Android library plugin, which cannot run externalNativeBuild (C/C++). Consumed only by the agent
// module's androidMain. whisper.cpp itself is fetched + compiled by CMake (src/main/cpp/CMakeLists.txt)
// at the version pinned in the catalog (same as the iOS xcframework).
android {
    namespace = "com.whispercpp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
        externalNativeBuild {
            cmake {
                // Pin the whisper.cpp release the native build clones (single source of truth: the
                // version catalog, shared with the iOS cinterop).
                arguments += "-DWHISPER_CPP_TAG=v${libs.versions.whisper.cpp.xcframework.get()}"
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // r27 LTS — Android Studio auto-installs it on first local build. The native build only runs for
    // assemble/install tasks; a plain Kotlin compile (CI) does not invoke the NDK.
    ndkVersion = "27.0.12077973"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
