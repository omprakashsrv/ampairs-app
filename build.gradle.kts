import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// Load local.properties into project extra so all submodules can access them via findProperty()
val localProps = java.util.Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { localProps.load(it) }
    localProps.forEach { key, value -> rootProject.ext[key.toString()] = value }
}

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinCocoapods) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.firebaseCrashlytics) apply false
    alias(libs.plugins.firebasePerf) apply false
    alias(libs.plugins.sentryPlugin) apply false
    // Applied (not `apply false`) on the root so it can aggregate the per-module
    // coverage declared via the `kover(project(...))` dependencies below.
    alias(libs.plugins.kover)
}

// ── Colon-free Kotlin module names ───────────────────────────────────────────
// Kotlin 2.4.0 derives a compilation's default module name from the Gradle project
// path (e.g. ":feature:customer"), so the ':' ends up in the @Metadata(moduleName)
// recorded in every compiled .class file. KSP2's Analysis API cannot parse that ':'
// when it reads those annotations from a *dependency* module's binaries, so any
// cross-module Room processor (the consolidated @Database classes in :data:database
// reference @Entity/@Dao types that live in the feature modules) fails with
// "[ksp] [MissingType]: ... references a type that is not present" on the JVM/Android
// targets. Native (iOS) is unaffected because klibs don't use JVM module-name
// mangling — which is exactly why iOS compiled while :data:database:kspAndroidMain
// did not. Forcing a colon-free, per-module name makes those binaries readable by
// KSP2. Applied uniformly across every compilation of a module so internal-member
// mangling stays consistent within the module.
subprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        val safeModuleName = path.removePrefix(":").replace(":", "_")
        extensions.configure<KotlinMultiplatformExtension> {
            targets.configureEach {
                compilations.configureEach {
                    compileTaskProvider.configure {
                        compilerOptions.moduleName.set(safeModuleName)
                    }
                }
            }
        }
    }
}

// ── Aggregated code coverage (Kover) ─────────────────────────────────────────
// ./gradlew koverXmlReport koverHtmlReport
//   → build/reports/kover/report.xml   (consumed by CI publishing)
//   → build/reports/kover/html/        (uploaded as a CI artifact)
//
// Each module listed here must apply the `kover` plugin. Add new modules as their
// test suites grow so they fold into the project-wide number.
dependencies {
    kover(project(":feature:unit"))
    kover(project(":feature:tax"))
    kover(project(":feature:invoice"))
    kover(project(":feature:customer"))
    kover(project(":feature:product"))
    kover(project(":feature:order"))
    kover(project(":feature:agent"))
}