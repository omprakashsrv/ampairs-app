// Copyright 2023, Google LLC, Christopher Banes
// SPDX-License-Identifier: Apache-2.0


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    jvmToolchain(17)
    androidTarget()
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.base)
                api(projects.core.preferences)
                implementation(libs.kotlin.inject.runtime)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.core)
            }
        }
    }
}

android {
    namespace = "app.tivi.core.powercontroller"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    kotlin {
        jvmToolchain(17)
    }
}
