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

// google/ksp#2476: for KMP-registered android compilations KSP populates its symbol-resolution
// classpath from `compileDependencyFiles`, which for AGP-KMP-library project dependencies contains
// raw .aar files that the analyzer silently skips — Room then reports every cross-module
// entity/DAO as [MissingType]. Feed the task the AAR -> classes.jar artifact view (the same view
// KSP's own AGP branch uses). Remove once KSP supports com.android.kotlin.multiplatform.library
// project dependencies natively.
tasks.matching { it.name == "kspAndroidMain" }.configureEach {
    val compileConfigName =
        kotlin.targets.getByName("android").compilations.getByName("main").compileDependencyConfigurationName
    val androidClassesJars = configurations.getByName(compileConfigName).incoming.artifactView {
        attributes.attribute(Attribute.of("artifactType", String::class.java), "android-classes-jar")
        lenient(true)
    }.files
    (this as com.google.devtools.ksp.gradle.KspAATask).kspConfig.libraries.from(androidClassesJars)
    // TEMP diagnostics for google/ksp#2476: dump what the analyzer actually receives.
    doFirst {
        val libs = (this as com.google.devtools.ksp.gradle.KspAATask).kspConfig.libraries.files
        logger.lifecycle("KSP kspAndroidMain libraries (${libs.size}):")
        libs.forEach { logger.lifecycle("  [${it.extension.ifEmpty { if (it.isDirectory) "dir" else "?" }}] $it") }
        logger.lifecycle("android-classes-jar artifactView (${androidClassesJars.files.size}):")
        androidClassesJars.files.forEach { logger.lifecycle("  $it") }
    }
}
