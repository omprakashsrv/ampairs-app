plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.metro)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.ampairs.data.database"
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
                implementation(projects.data.sync)
                implementation(projects.feature.auth)
                implementation(projects.feature.workspace)
                implementation(projects.feature.agent)
                implementation(projects.feature.customer)
                implementation(projects.feature.product)
                implementation(projects.feature.tax)
                implementation(projects.feature.order)
                implementation(projects.feature.invoice)
                implementation(projects.feature.purchase)
                implementation(projects.feature.payment)
                implementation(projects.feature.inventory)
                implementation(projects.feature.unit)
                implementation(projects.feature.form)
                implementation(projects.feature.file)
                implementation(projects.feature.business)
                implementation(projects.feature.store)
                implementation(projects.feature.subscription)
                implementation(projects.feature.supplier)
                implementation(projects.feature.sequence)
                implementation(projects.feature.pricing)
                implementation(projects.feature.printing)
                implementation(projects.feature.notification)
                implementation(projects.feature.ecom)
                implementation(libs.metro.runtime)
                implementation(libs.room.runtime)
                implementation(libs.room.paging)
                implementation(libs.paging.common)
                implementation(libs.sqlite.bundled)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.dateTime)
                implementation(libs.kermit)
            }
        }

        val desktopMain by getting

        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain.get())
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // kspCommonMainMetadata intentionally omitted: Room KSP generates an actual object for the
    // Database constructor in the metadata context, which conflicts with the expect object declared
    // in source when Kotlin 2.x compiles commonMainKotlinMetadata (expect/actual same-module error).
    // Platform KSPs (kspAndroid, kspIos*, kspDesktop) generate the actuals in the correct targets.
    add("kspAndroid", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

// google/ksp#2476: KSP2 resolves an incomplete project classpath for modules produced by the AGP
// KMP library plugin, so Room reports every cross-module entity/DAO as [MissingType]. The Kotlin
// compilation's compile classpath IS resolved correctly (AAR -> classes.jar transforms applied),
// so feed it to the matching KSP task explicitly. Remove once KSP supports
// com.android.kotlin.multiplatform.library project dependencies natively.
fun wireKspClasspath(taskName: String, targetName: String) {
    tasks.matching { it.name == taskName }.configureEach {
        (this as com.google.devtools.ksp.gradle.KspAATask).kspConfig.libraries.from(
            kotlin.targets.getByName(targetName).compilations.getByName("main").compileDependencyFiles
        )
    }
}
wireKspClasspath("kspAndroidMain", "android")
wireKspClasspath("kspDesktop", "desktop")
wireKspClasspath("kspIosArm64", "iosArm64")
wireKspClasspath("kspIosSimulatorArm64", "iosSimulatorArm64")
