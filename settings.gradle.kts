rootProject.name = "AmpairsApp"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://josm.openstreetmap.de/repository/releases/")
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Add JOSM repository for OpenStreetMap jmapviewer library
        maven("https://josm.openstreetmap.de/repository/releases/")
    }
    versionCatalogs {
        create("awssdk") {
            from("aws.sdk.kotlin:version-catalog:1.4.6")
        }
    }
}

include(":composeApp")
include(":tallyModule")
include(":thirdparty:androidx:paging:compose")
include(
    ":core:analytics",
    ":core:base",
    ":core:logging",
    ":core:performance",
    ":core:permissions",
    ":core:powercontroller",
    ":core:preferences",
    ":core:notifications:core",
    ":core:notifications:protos",
    ":common:ui:compose",
    ":common:imageloading",
    ":shared:common",
    ":shared:prod",
    ":shared:qa",
    ":ui:developer:log",
    ":ui:developer:notifications",
    ":ui:developer:settings",
    ":tasks"
)