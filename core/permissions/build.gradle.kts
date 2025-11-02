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

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "CorePermissions"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.base)
                api(libs.kotlin.inject.runtime)
            }
        }

        val mokoImplMain by creating {
            dependsOn(commonMain.get())

            dependencies {
                implementation(libs.moko.permissions.core)
            }
        }

        androidMain {
            dependsOn(mokoImplMain)

            dependencies {
                api(libs.androidx.activity)
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(mokoImplMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

android {
    namespace = "app.tivi.core.permissions"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    kotlin {
        jvmToolchain(17)
    }
}
