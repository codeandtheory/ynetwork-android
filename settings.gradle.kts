dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://ymedia.jfrog.io/artifactory/ynetwork-gradle-release/")
        }
    }
}
rootProject.name = "Accelerator"

include(":network:core")
include(":network:android")
include(":chat-application")
