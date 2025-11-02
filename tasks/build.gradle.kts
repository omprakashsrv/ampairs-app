// Copyright 2023, Google LLC, Christopher Banes and the Tivi project contributors
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
                implementation(projects.core.notifications.core)
                implementation(libs.kotlin.inject.runtime)
            }
        }

        androidMain {
            dependencies {
                api(libs.androidx.work.runtime)
            }
        }
    }
}

android {
    namespace = "app.tivi.tasks"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        manifestPlaceholders += mapOf("appAuthRedirectScheme" to "empty")
    }
    kotlin {
        jvmToolchain(17)
    }
}
