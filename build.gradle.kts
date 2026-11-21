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

// ── CocoaPods synthetic Podfile: raise the pod deployment-target floor ───────
// The Kotlin CocoaPods plugin generates a synthetic Podfile whose post_install raises every pod
// target's IPHONEOS_DEPLOYMENT_TARGET to at least a HARDCODED 12.0 (KT-57741, an Xcode 14.3
// workaround; not configurable). Pods such as FirebaseCore/GoogleUtilities 11.x declare 12.0 in
// their podspecs, and Xcode 26+/27 rejects anything below 15.0 ("supported deployment target
// versions is 15.0 to …"), failing `podBuild*` and so every iOS compile. Rewrite the floor to the
// project's iOS minimum — keep in sync with `ios.deploymentTarget` in the cocoapods {} blocks
// (shared, shared-ecom, feature/auth) and `platform :ios` in the app Podfiles.
val iosPodsMinDeploymentTarget = 16
subprojects {
    tasks.withType(org.jetbrains.kotlin.gradle.targets.native.tasks.PodGenTask::class.java).configureEach {
        val podfile = this.podfile
        val floor = iosPodsMinDeploymentTarget
        doLast {
            val file = podfile.get()
            val original = file.readText()
            val patched = original
                .replace("deployment_target_major < 12 ||", "deployment_target_major < $floor ||")
                .replace("deployment_target_major == 12 &&", "deployment_target_major == $floor &&")
                .replace("version = \"#{12}.#{0}\"", "version = \"#{$floor}.#{0}\"")
            if (patched == original) {
                logger.warn("PodGen: KT-57741 deployment-target block not found in $file — floor not raised to $floor.0")
            } else {
                file.writeText(patched)
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