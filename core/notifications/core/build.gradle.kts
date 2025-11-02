// Copyright 2024, Christopher Banes
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
                api(projects.core.base)
                api(projects.core.permissions)
//        implementation(projects.common.ui.resources)
                implementation(libs.kotlin.inject.runtime)
                implementation(libs.datastore)
                api(libs.kotlinx.dateTime)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.core)
                implementation(projects.core.notifications.protos)
            }
        }
    }
}

android {
    namespace = "app.tivi.core.notifications"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    kotlin {
        jvmToolchain(17)
    }
}
