plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
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
                // This module now OWNS every @Entity/@Dao/@Database (moved out of the feature
                // modules so Room's KSP processor runs single-module — cross-module @Entity
                // resolution is unsupported by KSP2 on JVM/Android). It therefore depends only on
                // the lowest-level shared infra; the feature modules depend on THIS module for the
                // persistence types.
                implementation(projects.data.common)
                implementation(libs.metro.runtime)
                implementation(libs.room.runtime)
                implementation(libs.room.paging)
                implementation(libs.paging.common)
                implementation(libs.sqlite.bundled)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.dateTime)
                implementation(libs.kotlinx.serialization.json)
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

// NOTE: cross-module Room processing (entities/DAOs living in the feature modules, @Database
// classes here) requires KSP >= 2.3.10 — earlier KSPs cannot read binary classes from modules
// compiled with Kotlin 2.4's default `{group}:{project}` module names and report every
// cross-module entity/DAO as [MissingType].
