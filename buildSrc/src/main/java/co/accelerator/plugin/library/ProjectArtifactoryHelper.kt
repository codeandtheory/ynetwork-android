package co.accelerator.plugin.library

import org.gradle.api.Project
import java.util.Locale

private const val ROOT_PACKAGE = "com.accelerator"

fun setArtifactoryDetails(project: Project) {
    // The default group name of the project follows the name of the directories from the root level of the project in package name fashion.
    // Eg: For networking core, the default group name is "Accelerator.network" (Accelerator being root project name - specified in "Settings.gradle" file).
    // Replace the root project name with the Root package name for artifactory.
    val modifiedGroupName = project.group.toString()
        .replace(project.rootProject.name, ROOT_PACKAGE)
        .replace(" ", "_")
        .toLowerCase(Locale.ROOT)

    val version = when (modifiedGroupName) {
        "$ROOT_PACKAGE.analytics.adobe",
        "$ROOT_PACKAGE.analytics",
        "$ROOT_PACKAGE.analytics.firebase",
        "$ROOT_PACKAGE.authentication",
        "$ROOT_PACKAGE.logger.android",
        "$ROOT_PACKAGE.logger.file",
        "$ROOT_PACKAGE.logger",
        "$ROOT_PACKAGE.downloader" -> "1.0.0"
        "$ROOT_PACKAGE.network.android",
        "$ROOT_PACKAGE.network" -> "2.0.0"
        else -> throw NotImplementedError("Accelerator ProjectArtifactoryException: Version for $modifiedGroupName group is not specified")
    }

    // Setup the modified data.
    project.group = modifiedGroupName
    project.version = version
}
