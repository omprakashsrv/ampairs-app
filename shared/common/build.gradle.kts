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
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.base)
                api(projects.core.analytics)
                api(projects.core.notifications.core)
                api(projects.core.logging)
                api(projects.core.performance)
                api(projects.core.permissions)
                api(projects.core.powercontroller)
                api(projects.core.preferences)
                api(projects.tasks)
                implementation(libs.kotlin.inject.runtime)

                api(projects.common.imageloading)
                api(projects.common.ui.compose)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okHttp)
                // Developer modules only for Android
                api(projects.ui.developer.log)
                api(projects.ui.developer.notifications)
                api(projects.ui.developer.settings)
            }
        }

        jvmMain {
            dependencies {
                api(libs.ktor.client.okHttp)
                // Developer modules only for JVM/Desktop
                api(projects.ui.developer.log)
                api(projects.ui.developer.notifications)
                api(projects.ui.developer.settings)
            }
        }
    }
}

android {
    namespace = "app.tivi.shared.common"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    kotlin {
        jvmToolchain(17)
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

ksp {
    arg("me.tatarka.inject.generateCompanionExtensions", "true")
}
