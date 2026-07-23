plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.metro)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    `maven-publish`
}

group = "com.ampairs"
version = "1.0.3"

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.ampairs.data.sync"
        compileSdk { version = release(libs.versions.android.compileSdk.get().toInt()) }
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.data.common)
                // Sync-state persistence (SyncEntity/SyncStateEntity/SyncStateDao) lives in the
                // consolidated database module after DB consolidation.
                implementation(projects.data.database)
                implementation(libs.metro.runtime)
                implementation(libs.metrox.viewmodel.compose)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.dateTime)
                implementation(libs.kermit)
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.animation)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.lifecycle.runtime.compose)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okHttp)
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

// Room persistence (SyncStateEntity/@Database + KSP codegen) moved to :data:database as part of the
// database consolidation — this module no longer runs the Room processor.

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/omprakashsrv/ampairs-app")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
