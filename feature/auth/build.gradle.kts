plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.ampairs.auth"
        compileSdk { version = release(libs.versions.android.compileSdk.get().toInt()) }
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.data.common)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.bundles.ktor.common)
                // Compose
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.animation)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.components.resources)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
                implementation(libs.store5)
                implementation(libs.kotlinx.dateTime)
                // FileKit for cross-platform file picking
                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                // Coil image loading (CoilImageLoader lives here, needs TokenRepository)
                implementation(libs.coil.core)
                implementation(libs.coil.compose)
                implementation(libs.coil.network)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.koin.android)
                implementation(libs.ktor.client.okHttp)
                // Firebase Auth (OTP/Phone sign-in)
                implementation(libs.firebase.auth)
                implementation(libs.google.firebase.crashlytics)
                implementation(libs.google.firebase.analytics)
                implementation(libs.google.firebase.perf)
                implementation(libs.google.firebase.messaging)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.okHttp)
            }
        }
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain.get())
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspCommonMainMetadata", libs.room.compiler)
    add("kspAndroid", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

tasks.withType<com.google.devtools.ksp.gradle.KspAATask>().configureEach {
    dependsOn(tasks.matching { it.name.startsWith("generateComposeResClass") })
    dependsOn(tasks.matching { it.name.startsWith("generateResourceAccessorsFor") })
    dependsOn(tasks.matching { it.name.startsWith("generateActualResourceCollectorsFor") })
    dependsOn(tasks.matching { it.name.startsWith("generateExpectResourceCollectorsFor") })
}
