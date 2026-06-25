import java.net.URI

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.metro)
    alias(libs.plugins.kover)
}

// whisper.cpp prebuilt xcframework (ggml) for the iOS offline Whisper STT cinterop. Downloaded once
// into build/whisper-xcframework; cinterop binds whisper.h from the per-arch framework Headers. The
// static lib is linked later in the iOS app/framework build (device-side), not at klib generation.
val whisperXcframeworkDir = layout.buildDirectory.dir("whisper-xcframework")
val downloadWhisperXcframework = tasks.register("downloadWhisperXcframework") {
    val outDir = whisperXcframeworkDir.get().asFile
    val version = libs.versions.whisper.cpp.xcframework.get()
    outputs.dir(outDir)
    onlyIf { !outDir.resolve("whisper.xcframework").exists() }
    doLast {
        outDir.mkdirs()
        val zip = outDir.resolve("whisper.zip")
        val url = "https://github.com/ggml-org/whisper.cpp/releases/download/v$version/whisper-v$version-xcframework.zip"
        URI(url).toURL().openStream().use { input -> zip.outputStream().use { input.copyTo(it) } }
        copy {
            from(zipTree(zip))
            into(outDir)
        }
    }
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.ampairs.agent"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
    }
    jvm("desktop")
    iosArm64 {
        compilations.getByName("main").cinterops.create("whisper") {
            defFile(project.file("src/nativeInterop/cinterop/whisper.def"))
            includeDirs(whisperXcframeworkDir.get().asFile.resolve("whisper.xcframework/ios-arm64/whisper.framework/Headers"))
            tasks.named(interopProcessingTaskName).configure { dependsOn(downloadWhisperXcframework) }
        }
    }
    iosSimulatorArm64 {
        compilations.getByName("main").cinterops.create("whisper") {
            defFile(project.file("src/nativeInterop/cinterop/whisper.def"))
            includeDirs(whisperXcframeworkDir.get().asFile.resolve("whisper.xcframework/ios-arm64_x86_64-simulator/whisper.framework/Headers"))
            tasks.named(interopProcessingTaskName).configure { dependsOn(downloadWhisperXcframework) }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.data.common)
                implementation(projects.feature.authApi)
                implementation(projects.feature.customerApi)
                implementation(projects.feature.productApi)
                implementation(libs.metro.runtime)
                implementation(libs.kermit)
                implementation(libs.metrox.viewmodel.compose)
                implementation(libs.lifecycle.viewmodel)
                implementation(libs.lifecycle.viewmodel.navigation3)
                implementation(libs.bundles.ktor.common)
                // Compose
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.animation)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.components.resources)
                // KMP filesystem IO + SHA-256 for the on-device model downloader (ModelManager).
                implementation(libs.kotlinx.io.core)
                implementation(libs.kotlincrypto.sha2)
                // Room — local persistence for the assistant chat transcript (resume-on-reopen).
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
            }
        }
        androidMain {
            dependencies {
                // Grant — KMP runtime-permission library (Android+iOS); used here for the
                // RECORD_AUDIO mic gate (T016). iOS binding follows once Android probe is green.
                implementation(libs.grant.core)
                // LiteRT-LM (Google AI Edge) — on-device LLM runtime for the LiteRtLmEngine
                // adapter (T025); loads the `litert-lm` Gemma models (FunctionGemma-270m,
                // Gemma 3n E2B/E4B). Android-only; iOS/Desktop fall back to llama.cpp (T027).
                implementation(libs.litertlm.android)
                // LiteRT (TFLite runtime, prebuilt AAR with native .so) — runs the Whisper `.tflite`
                // graph for the offline Whisper STT adapter (AndroidWhisperTranscriber). Android-only.
                implementation(libs.litert)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val desktopMain by getting {
            dependencies {
                // whisper.cpp via JNI (native libs embedded for Win/Mac/Linux) — offline Whisper STT
                // engine on Desktop, which has no platform recognizer. Desktop/JVM only.
                implementation(libs.whisper.jni)
                // LiteRT-LM unified Kotlin API (JVM build) — on-device LLM with GPU/NPU acceleration
                // for the desktop LiteRtLmEngine (mirrors the Android engine; same 0.13.1 API). The
                // native GPU plugins (WebGPU/Dawn on Linux/Windows, Metal on macOS) ship as separate
                // prebuilt libs that must be on the JVM native path at runtime. Desktop/JVM only.
                implementation(libs.litertlm.jvm)
            }
        }
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain.get())
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

tasks.withType<com.google.devtools.ksp.gradle.KspAATask>().configureEach {
    dependsOn(tasks.matching { it.name.startsWith("generateComposeResClass") })
    dependsOn(tasks.matching { it.name.startsWith("generateResourceAccessorsFor") })
    dependsOn(tasks.matching { it.name.startsWith("generateActualResourceCollectorsFor") })
    dependsOn(tasks.matching { it.name.startsWith("generateExpectResourceCollectorsFor") })
}
