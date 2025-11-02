// Copyright 2024, Google LLC, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
}

android {
    namespace = "app.tivi.developer.notifications"
}

kotlin {
    jvmToolchain(17)
    androidTarget()
    jvm()
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.base)
                api(projects.core.notifications.core)
//                api(projects.domain)

                implementation(projects.common.ui.compose)

                implementation(compose.material3)
                implementation(compose.animation)
            }
        }
    }
}


android {
    namespace = "app.tivi.notification"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    kotlin {
        jvmToolchain(17)
    }
}