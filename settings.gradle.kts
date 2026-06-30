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
        maven("https://josm.openstreetmap.de/repository/releases/")
    }
}

include(":shared")
include(":androidApp")
include(":whispercpp")
include(":desktopApp")
include(":data:common")
include(":data:sync")
include(":data:event")
include(":tally")
include(":printing:core")
include(":printing:render")
include(":printing:transport")
include(":feature:printing")
include(
    ":feature:file-api",
    ":feature:file",
    ":feature:auth-api",
    ":feature:auth",
    ":feature:ecom-api",
    ":feature:ecom",
    ":feature:customer-api",
    ":feature:product-api",
    ":feature:inventory-api",
    ":feature:tax-api",
    ":feature:form-api",
    ":feature:unit-api",
    ":feature:subscription-api",
    ":feature:agent",
    ":feature:form",
    ":feature:formwidgets",
    ":feature:unit",
    ":feature:sfa",
    ":feature:pricing",
    ":feature:sequence",
    ":feature:store",
    ":feature:update",
    ":feature:tax",
    ":feature:subscription",
    ":feature:business",
    ":feature:product",
    ":feature:customer",
    ":feature:supplier",
    ":feature:inventory",
    ":feature:order",
    ":feature:invoice",
    ":feature:purchase",
    ":feature:payment",
    ":feature:notification",
    ":feature:workspace"
)
