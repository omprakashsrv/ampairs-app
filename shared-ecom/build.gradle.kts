plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.metro)
}

// Slim, cross-platform shared layer for customer-facing ecom ordering apps. Four thin app modules
// build on it: :clientApp (Android, per-client white-label pinned to one storefront, -Pclient=<id>)
// and :marketplaceApp (Android, multi-store picker), plus their iOS counterparts (clientApp/iosApp,
// marketplaceApp/iosApp) which consume the `SharedEcom` framework produced by this module's
// cocoapods block. Reuses the login (auth), data, sync, store and ecom feature modules with its own
// DI graph and navigation graph — it deliberately does NOT depend on :shared (which aggregates all
// 25 business modules). Brand-neutral: nothing here is tenant-specific.
// See CLAUDE.md and the /metro-di + /offline-sync skills.
kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.ampairs.storefront"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
    }

    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Ampairs Storefront (ecom) shared layer"
        version = "1.0.0"
        homepage = "https://ampairs.in"
        ios.deploymentTarget = "16.0"
        // Static framework: Firebase (Core/Auth) and reCAPTCHA ObjC symbols referenced transitively
        // by :feature:auth are left undefined here and resolved at the app link step by the storefront
        // apps' own Podfiles (marketplaceApp/iosApp, clientApp/iosApp) — mirrors iosApp/Podfile.
        framework {
            baseName = "SharedEcom"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Reused feature modules (login + ecom storefront/order + workspace settings)
                api(projects.feature.auth)
                api(projects.feature.authApi)
                api(projects.feature.ecom)
                api(projects.feature.ecomApi)
                api(projects.feature.store)
                // File upload/pick bindings (FileRepository + FilePicker used by ecom VMs)
                api(projects.feature.file)
                api(projects.feature.fileApi)
                // Shared data infrastructure
                api(projects.data.common)
                api(projects.data.sync)
                // Consolidated Room databases (incl. the storefront @Database classes) live here.
                api(projects.data.database)
                // LocationService + ContactPickerService bindings (address location picker)
                implementation(projects.feature.formwidgets)
                // DataStore (AppPreferences public type)
                implementation(libs.datastore)
                implementation(libs.datastore.preferences)

                // Metro DI
                implementation(libs.metro.runtime)
                implementation(libs.metrox.viewmodel.compose)

                // Ktor
                implementation(libs.bundles.ktor.common)

                // Coroutines (StorefrontWorkspaceManager cleanup scope, sync)
                implementation(libs.kotlinx.coroutines.core)

                // Compose
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.components.resources)
                implementation(libs.material3.adaptive)

                // Lifecycle + Navigation3
                implementation(libs.lifecycle.runtime.compose)
                implementation(libs.lifecycle.viewmodel)
                implementation(libs.lifecycle.viewmodel.compose)
                implementation(libs.lifecycle.viewmodel.navigation3)
                implementation(libs.navigation3.ui)

                // Coil image loading
                implementation(libs.coil.core)
                implementation(libs.coil.compose)
                implementation(libs.coil.network)

                // Serialization (NavKey routes) + logging
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kermit)

                // Consolidated storefront Room databases (app + workspace) live in :data:database
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
            }
        }

        val androidMain by getting {
            dependencies {
                // Ktor OkHttp engine (Android)
                implementation(libs.ktor.client.okHttp)

                // Firebase — native Android SDK (analytics binding used by LoginViewModel; auth =
                // phone sign-in via :feature:auth)
                implementation(libs.google.firebase.analytics)
                implementation(libs.firebase.auth)
            }
        }

        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                // Ktor Darwin engine (iOS)
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}
