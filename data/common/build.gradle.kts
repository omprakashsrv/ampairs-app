plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.metro)
    `maven-publish`
}

group = "com.ampairs"
version = "1.0.0"

// Override artifact name so the published ID is "data-common" rather than just "common"
base.archivesName.set("data-common")

// Pin resource accessor package so setting group="com.ampairs" for publishing
// doesn't shift the auto-derived package away from the existing source imports.
compose.resources {
    packageOfResClass = "ampairsapp.data.common.generated.resources"
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.ampairs.data.common"
        compileSdk { version = release(libs.versions.android.compileSdk.get().toInt()) }
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
    }
    jvm("desktop")   // must match shared — named target
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                // Compose
                implementation(libs.compose.runtime)
                implementation(libs.lifecycle.runtime.compose)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.animation)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.components.resources)

                // Metro
                implementation(libs.metro.runtime)

                // Ktor HTTP client
                implementation(libs.bundles.ktor.common)

                // Room runtime (infrastructure only — no @Database here)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)

                // DataStore preferences
                implementation(libs.datastore)
                implementation(libs.datastore.preferences)

                // Coroutines & datetime
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.dateTime)

                // Coil image loading
                implementation(libs.coil.core)
                implementation(libs.coil.compose)
                implementation(libs.coil.network)

                // UUID generation
                implementation(libs.uuid)

                // Sentry KMP
                implementation(libs.sentry.kmp)

                // Kermit logging
                implementation(libs.kermit)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okHttp)
                // In-app update (InAppUpdateManager)
                implementation(libs.app.update)
                implementation(libs.play.services.coroutines)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core)
            }
        }

        // Desktop — jvm("desktop") creates desktopMain
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.okHttp)
            }
        }

        // iOS — manual wiring required (applyDefaultHierarchyTemplate=false)
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
