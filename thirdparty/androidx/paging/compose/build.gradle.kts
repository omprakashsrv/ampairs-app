// Copyright 2024, Christopher Banes
// SPDX-License-Identifier: Apache-2.0


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
}

kotlin {
    jvmToolchain(17)
    jvm()
    iosArm64()
    iosSimulatorArm64()
    android {
        namespace = "androidx.paging.compose"
        compileSdk { version = release(libs.versions.android.compileSdk.get().toInt()) }
    }
    sourceSets {
        commonMain {
            dependencies {
                api(libs.paging.common)
                // Compose Multiplatform library (explicit dependency for 1.10.0+)
                api(libs.compose.runtime)
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

