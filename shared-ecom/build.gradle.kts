plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.metro)
}

// Slim, Android-only shared layer for customer-facing ecom ordering apps (one thin app module per
// enterprise customer — e.g. :ambikaApp — supplies the pinned workspace slug + branding). Reuses the
// login (auth), data, sync, store and ecom feature modules with its own DI graph and navigation graph
// — it deliberately does NOT depend on :shared (which aggregates all 25 business modules). Brand-neutral:
// nothing here is tenant-specific. See CLAUDE.md and the /metro-di + /offline-sync skills.
kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.ampairs.storefront"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
    }

    sourceSets {
        androidMain {
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
                implementation(libs.ktor.client.okHttp)

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

                // Firebase (analytics binding used by LoginViewModel; auth = phone sign-in)
                implementation(libs.google.firebase.analytics)
                implementation(libs.firebase.auth)

                // Serialization (NavKey routes) + logging
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kermit)
            }
        }
    }
}
