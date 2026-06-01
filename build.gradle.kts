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
}