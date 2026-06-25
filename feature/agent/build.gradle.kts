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

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.ampairs.agent"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

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
                // Cactus — on-device Whisper STT engine (selectable speech adapter). Android+iOS only
                // (no JVM target), so it's added per-platform, not in commonMain.
                implementation(libs.cactus)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val desktopMain by getting { }
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
