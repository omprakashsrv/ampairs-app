// Copyright 2023, Christopher Banes
// SPDX-License-Identifier: Apache-2.0


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
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
                api(projects.core.preferences)
                api(projects.common.imageloading)
                api(libs.material.kolor)
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.animation)
                implementation(compose.materialIconsExtended)
                implementation(compose.material3)
                implementation(compose.components.resources)
                implementation(libs.kotlin.inject.runtime)

//                api(projects.common.ui.screens)
//                api(libs.circuit.foundation)

//                implementation(projects.common.ui.resources)
//                api(projects.common.ui.resources)
//                api(libs.lyricist.library)

                api(libs.haze)
                api(libs.coil.compose)

//                implementation(libs.androidx.collection)

//                implementation(compose.foundation)
//                implementation(compose.material)
//                implementation(compose.materialIconsExtended)
//                api(compose.material3)
//                api(libs.compose.material3.windowsizeclass)
//                implementation(compose.animation)

                implementation(libs.uuid)

                api(libs.paging.common)
                api(libs.kotlinx.dateTime)
                api(projects.thirdparty.androidx.paging.compose)
            }
        }

        val jvmCommon by creating {
            dependsOn(commonMain.get())
        }

        jvmMain {
            dependsOn(jvmCommon)
        }

        androidMain {
            dependsOn(jvmCommon)

            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain.get())
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

android {
    namespace = "app.tivi.common.compose"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    kotlin {
        jvmToolchain(17)
    }
}
