plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.metro)
}

// Slim, Android-only shared layer for customer-facing ecom ordering apps. Two thin app modules build
// on it: :clientApp (a per-client white-label build pinned to one storefront, selected with
// -Pclient=<id>) and :marketplaceApp (the common multi-store app with a storefront picker). Reuses the
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

                // Consolidated storefront Room databases (app + workspace) live in this module
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
            }
        }
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Android-only module: the storefront @Database classes live in androidMain.
    add("kspAndroid", libs.room.compiler)
}

// google/ksp#2476: same workaround as :data:database — KSP's KMP android task can't read the raw
// .aar project dependencies on its classpath, so feed it the AAR -> classes.jar artifact view.
tasks.matching { it.name == "kspAndroidMain" }.configureEach {
    val compileConfigName =
        kotlin.targets.getByName("android").compilations.getByName("main").compileDependencyConfigurationName
    val androidClassesJars = configurations.getByName(compileConfigName).incoming.artifactView {
        attributes.attribute(Attribute.of("artifactType", String::class.java), "android-classes-jar")
        lenient(true)
    }.files
    (this as com.google.devtools.ksp.gradle.KspAATask).kspConfig.libraries.from(androidClassesJars)
}
