package com.accelerator.plugin.library.kmp

import com.accelerator.plugin.library.LibraryPlugin
import com.accelerator.plugin.library.PluginConstants
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File

private val ALLOWED_CONFIG_REGEX = listOf(".*MainImplementation$".toRegex(), ".*MainApi$".toRegex())

/**
 * Plugin for setting up the Kotlin-MultiPlatform project.
 */
class KmpLibraryPlugin : LibraryPlugin() {

    override fun apply(target: Project) {
        target.apply(plugin = "kotlin-multiplatform")
        target.apply(plugin = "java")

        // Setup java
        setupConfiguration<JavaPluginExtension>(target, "java") {
            sourceCompatibility = PluginConstants.javaVersion
            targetCompatibility = PluginConstants.javaVersion
        }

        setupArtifactoryPublishing(target)
        setupJacocoPlugin(target)
    }

    private fun setupArtifactoryPublishing(target: Project) {
        setupPublishingExtension(
            target = target,
            publicationName = "jar",
            allowedConfigurationRegex = ALLOWED_CONFIG_REGEX
        ) { "${target.buildDir}/libs/${target.name}-jvm-${target.version}.jar" }
    }

    private fun setupJacocoPlugin(target: Project) {
        target.apply(plugin = "jacoco")
        setupConfiguration<JacocoPluginExtension>(target, "jacoco") {
            toolVersion = PluginConstants.jacocoVersion
        }

        target.tasks.withType(JacocoReport::class.java) {
            dependsOn("jvmTest")
            group = "Reporting"
            description = "Generate Jacoco coverage reports."

            val classFiles = target.fileTree("${target.buildDir}/classes/kotlin/jvm/main/")

            classDirectories.setFrom(target.files(classFiles))

            val srcFileDir = target.fileTree("src/") { setExcludes(listOf("*Test/**", "*Test/**")) }
            sourceDirectories.setFrom(target.files(srcFileDir))
            additionalSourceDirs.setFrom(target.files(srcFileDir))

            executionData.setFrom(target.files("${target.buildDir}/jacoco/jvmTest.exec"))
            reports {
                xml.isEnabled = false
                csv.isEnabled = false
                html.isEnabled = true
                html.destination = File("${target.buildDir}/jacoco-reports/html")
            }
        }
    }
}
