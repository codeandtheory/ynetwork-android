package co.accelerator.plugin.library

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getPluginByName
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention

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
    }

    private fun setupArtifactory(target: Project, publicationName: String) {
        target.convention.getPluginByName<ArtifactoryPluginConvention>("artifactory").apply {
            setContextUrl(project.findProperty("artifactory_contextUrl"))
            publish {
                repository {
                    setProperty("repoKey", "ynetwork-gradle-release")
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
                    setProperty("repoKey", "ynetwork-gradle-dev")
                    setProperty("username", project.findProperty("artifactory_user"))
                    setProperty("password", project.findProperty("artifactory_password"))
                    setProperty("maven", true)
                }
            }
        }
    }
}