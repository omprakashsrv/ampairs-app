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
                api(projects.core.base)
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.dateTime)
                api(libs.kermit)
                implementation(libs.datastore)
                implementation(libs.datastore.preferences)

                implementation(libs.kotlin.inject.runtime)
            }
        }

        val mobileMain by creating {
            dependsOn(commonMain.get())

            dependencies {
                implementation(libs.crashkios.crashlytics)
            }
        }

        androidMain {
            dependsOn(mobileMain)

            dependencies {
                implementation(libs.google.firebase.crashlytics)
            }
        }

        iosMain {
            dependsOn(mobileMain)
        }
    }
}

android {
    namespace = "app.tivi.core.logging"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    kotlin {
        jvmToolchain(17)
    }
}
