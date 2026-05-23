rootProject.name = "AmpairsApp"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
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
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        // Add JOSM repository for OpenStreetMap jmapviewer library
        maven("https://josm.openstreetmap.de/repository/releases/")
    }
    versionCatalogs {
        create("awssdk") {
            from("aws.sdk.kotlin:version-catalog:1.4.6")
        }
    }
}

include(":shared")
include(":androidApp")
include(":desktopApp")
include(":data:common")
include(":tallyModule")
include(
    ":feature:auth-api",
    ":feature:auth",
    ":feature:customer-api",
    ":feature:product-api",
    ":feature:tax-api",
    ":feature:form-api",
    ":feature:unit-api",
    ":feature:subscription-api",
    ":feature:agent",
    ":feature:form",
    ":feature:unit",
    ":feature:update",
    ":feature:event",
    ":feature:tax",
    ":feature:subscription",
    ":feature:business",
    ":feature:product",
    ":feature:customer",
    ":feature:inventory",
    ":feature:order",
    ":feature:invoice",
    ":feature:workspace"
)
include(":thirdparty:androidx:paging:compose")