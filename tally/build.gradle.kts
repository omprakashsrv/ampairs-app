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
    api(libs.bundles.ktor.common)
    api(libs.ktor.serialization.kotlinx.xml)
    implementation(libs.kermit)

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.0")
}

tasks.test {
    useJUnitPlatform()
    // Forward -Dtally.host / -Dtally.port from Gradle JVM to test JVM
    systemProperty("tally.host", System.getProperty("tally.host") ?: "")
    systemProperty("tally.port", System.getProperty("tally.port") ?: "9008")
}