plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinSerialization)
}

group = "com.ampairs"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(libs.koin.core)
    api(libs.bundles.ktor.common)
    api(libs.ktor.serialization.kotlinx.xml)
}

tasks.test {
    useJUnitPlatform()
}