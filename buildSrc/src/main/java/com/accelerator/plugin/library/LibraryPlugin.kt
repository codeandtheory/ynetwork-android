package com.accelerator.plugin.library

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.*
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import de.marcphilipp.gradle.nexus.NexusPublishExtension
import org.gradle.plugins.signing.SigningExtension
import java.net.URI
import io.codearte.gradle.nexus.NexusStagingExtension
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.collections.HashMap


/**
 * Base class for configuring common components of library modules.
 *
 * @see <a href="https://quickbirdstudios.com/blog/gradle-kotlin-buildsrc-plugin-android/">https://quickbirdstudios.com/blog/gradle-kotlin-buildsrc-plugin-android/</a>
 */
abstract class LibraryPlugin : Plugin<Project> {

    /**
     * Function to setup project configuration.
     * NOTE: Added this function to allow type-safety while setting up extension configuration.
     *
     * @param target instance for which the configuration needs to be setup
     * @param extensionName name of the extension
     * @param configure configuration for project extension.
     */
    protected fun <CONFIGURATION> setupConfiguration(
        target: ExtensionAware,
        extensionName: String,
        configure: Action<CONFIGURATION>
    ) = target.extensions.configure(extensionName, configure)

    protected fun setupPublishingExtension(
        target: Project,
        publicationName: String,
        allowedConfigurationRegex: List<Regex>,
        artifactoryFilePath: () -> String
    ) {
        target.apply(plugin = "com.jfrog.artifactory")
        target.apply(plugin = "maven-publish")
        setArtifactoryDetails(target)
        setupArtifactory(target, publicationName)
        setupConfiguration<PublishingExtension>(target, "publishing") {
            publications {
                register(publicationName, MavenPublication::class.java) {
                    groupId = target.group.toString()
                    version = target.version.toString()
                    artifactId = target.name

                    // Tell maven to prepare the generated file for publishing
                    artifact(artifactoryFilePath())
                    artifact(target.tasks["sourcesJar"])    // Also, take the Jar containing source code in artifactory.
                    pom.withXml {
                        val dependenciesNode = asNode().appendNode("dependencies")

                        // Iterate over the compile dependencies (we don't want the test ones), adding a <dependency> node for each.
                        target.configurations.asMap
                            .filter { allowedConfigurationRegex.any { regex -> regex.matches(it.key) } } // Pick all the configuration, which matches the allowed configuration regex (implementation, api, commonMainApi, etc.)
                            .values // Extract values of each configuration, there is no need for key in further operation
                            .map { it.allDependencies } // Extract all dependencies.
                            .flatten()  // Flatten/Merge all dependencies into a single list.
                            .toSet()    // Remove duplicates.
                            .forEach {
                                val dependencyNode = dependenciesNode.appendNode("dependency")
                                dependencyNode.appendNode("groupId", it.group)
                                dependencyNode.appendNode("artifactId", it.name)
                                dependencyNode.appendNode("version", it.version)
                            }
                    }
                }
            }
        }
        setUpNexusArtifactory(target)
    }

    private fun setupArtifactory(target: Project, publicationName: String) {
        target.convention.getPluginByName<ArtifactoryPluginConvention>("artifactory").apply {
            setContextUrl(project.findProperty("artifactory_contextUrl"))
            publish {
                repository {
                    setProperty("repoKey", "accelerator-gradle-release-local")
                    setProperty("username", project.findProperty("artifactory_user"))
                    setProperty("password", project.findProperty("artifactory_password"))
                    setProperty("maven", true)
                }
                defaults {
                    // Tell the Artifactory Plugin which artifacts should be published to Artifactory.
                    publications(publicationName)
                    setProperty("publishArtifacts", true)
                    // Publish generated POM files to Artifactory (true by default)
                    setProperty("publishPom", true)
                }
            }
            resolve {
                repository {
                    setProperty("repoKey", "accelerator-gradle-dev")
                    setProperty("username", project.findProperty("artifactory_user"))
                    setProperty("password", project.findProperty("artifactory_password"))
                    setProperty("maven", true)
                }
            }
        }
    }

    fun setUpNexusArtifactory(target: Project) {
        target.plugins.apply("maven-publish")
        target.repositories {
            mavenCentral()
        }
        // read properties
        var sonaTypeProperties : HashMap<String, String>
                = HashMap<String, String> ()
        val secretPropsFile = target.rootProject.file("local.properties")
        if (secretPropsFile.exists()) {
            // Read local.properties file first if it exists
            val  p = Properties()
            val fis = FileInputStream(secretPropsFile)
            p.load(fis)
            p.onEach{ map -> sonaTypeProperties[map.key as String] = map.value as String }
        }
        // configure ossh_username and ossh_password for the same
        target.plugins.apply("de.marcphilipp.nexus-publish")
        target.extensions.configure<NexusPublishExtension>("nexusPublishing") {
            repositories {
                sonatype {
                    username.set(
                        sonaTypeProperties.get("OSSRH_USERNAME")
                    )
                    password.set(
                        sonaTypeProperties.get("OSSRH_PASSWORD")
                    )
                    nexusUrl.set(URI("https://s01.oss.sonatype.org/service/local/"))
                    snapshotRepositoryUrl.set(URI("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
                    stagingProfileId.set(sonaTypeProperties.get("SONATYPE_STAGING_PROFILE_ID"))
                }
            }
        }
        // publish sources
        target.afterEvaluate {
            when {
                plugins.hasPlugin("com.android.library") -> plugins.apply(AndroidLibraryPlugin::class.java)
            }
        }

        target.repositories{
            maven {
                url = URI("https://s01.oss.sonatype.org/service/local/").resolve("/content/repositories/public")
                credentials {
                    username = sonaTypeProperties.get("OSSRH_USERNAME")
                    password = sonaTypeProperties.get("OSSRH_PASSWORD")
                }
            }
        }

        // sign publications
        target.plugins.apply("signing")
        target.afterEvaluate {
            extensions.configure<PublishingExtension>() {
                configure<SigningExtension>() {
                    useInMemoryPgpKeys(
                        sonaTypeProperties["signing.keyId"],
                        sonaTypeProperties["signing.key"],
                        sonaTypeProperties["signing.password"]
                    )
                    sign(publications)
                }
            }
        }
    }
}