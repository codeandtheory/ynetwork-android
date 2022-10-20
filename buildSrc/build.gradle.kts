import org.gradle.kotlin.dsl.*

plugins {
    `java-gradle-plugin`
    `maven-publish`
    `kotlin-dsl`
    `signing`
    kotlin("jvm") version embeddedKotlinVersion
    id("io.codearte.nexus-staging") version "0.22.0"
    id("de.marcphilipp.nexus-publish") version "0.4.0"
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}


dependencies {
    /* Example Dependency */
    /* Depend on the android gradle plugin, since we want to access it in our plugin */
    implementation(gradleApi())
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib"))
    implementation("de.marcphilipp.gradle:nexus-publish-plugin:0.4.0")
    implementation("io.codearte.nexus-staging:io.codearte.nexus-staging.gradle.plugin:0.22.0")
    implementation("com.android.tools.build:gradle:7.1.3")

    /* Example Dependency */
    /* Depend on the kotlin plugin, since we want to access it in our plugin */
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
    implementation("org.jfrog.buildinfo:build-info-extractor-gradle:4.24.20")
}
