// Copyright 2023, Google LLC, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(17)
    androidTarget()
    jvm()
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.common)
                implementation(libs.kotlin.inject.runtime)
//                api(projects.ui.developer.log)
//                api(projects.ui.developer.notifications)
//                api(projects.ui.developer.settings)
            }
        }

        androidMain {
            dependencies {
//                implementation(libs.chucker.library)
//                implementation(libs.okhttp.loggingInterceptor)
            }
        }
    }
}

android {
    namespace = "app.tivi.shared.qa"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    kotlin {
        jvmToolchain(17)
    }
}

ksp {
    arg("me.tatarka.inject.generateCompanionExtensions", "true")
}