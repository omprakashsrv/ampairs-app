plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.metro)
    `maven-publish`
}

group = "com.ampairs"
version = "1.0.0"

// Pin resource accessor package so group="com.ampairs" for publishing
// doesn't shift the auto-derived package away from existing source imports.
compose.resources {
    packageOfResClass = "ampairsapp.feature.auth.generated.resources"
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.ampairs.auth"
        compileSdk { version = release(libs.versions.android.compileSdk.get().toInt()) }
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
        // Without this, Kotlin derives the module name as "$group:$name" ("com.ampairs:auth")
        // since group is set for publishing above. R8's release/minify pass re-embeds that
        // literal (colon-containing) name as a META-INF/*.kotlin_module entry, which then fails
        // androidApp:bundleRelease with "Entry name contains invalid characters".
        compilerOptions {
            moduleName.set("auth")
        }
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.feature.authApi)
                implementation(projects.data.common)
                implementation(projects.data.database)
                implementation(libs.metro.runtime)
                implementation(libs.metrox.viewmodel.compose)
                implementation(libs.bundles.ktor.common)
                implementation(libs.kermit)
                // Compose
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.animation)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.components.resources)
                implementation(libs.lifecycle.runtime.compose)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
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

    cocoapods {
        summary = "Ampairs Auth Feature"
        version = "1.0.0"
        homepage = "https://ampairs.in"
        ios.deploymentTarget = "16.0"
        framework {
            baseName = "auth"
            isStatic = true
        }
        pod("FirebaseCore") {
            version = "~> 11.13"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
        pod("FirebaseAuth") {
            version = "~> 11.13"
            extraOpts += listOf("-compiler-option", "-fmodules")
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
