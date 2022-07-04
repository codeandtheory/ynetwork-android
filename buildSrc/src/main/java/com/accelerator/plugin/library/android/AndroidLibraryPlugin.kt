package com.accelerator.plugin.library.android

import com.accelerator.plugin.library.LibraryPlugin
import com.accelerator.plugin.library.PluginConstants
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByName
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import java.io.File
import java.util.Locale

private const val EXTENSION_ANDROID = "android"
private val ALLOWED_CONFIGURATION_REGEX = listOf("implementation", "api").map { it.toRegex() }

/**
 * Plugin for setting up the Android Library project.
 */
class AndroidLibraryPlugin : LibraryPlugin() {
    override fun apply(target: Project) {
        target.apply(plugin = "com.android.library")
        target.apply(plugin = "kotlin-android")
        setupAndroidSettings(target)
        setupJacocoPlugin(target)
        setupArtifactoryPublishing(target)
        setupResolutionStrategy(target)
    }

    private fun setupAndroidSettings(target: Project) {
        setupConfiguration<LibraryExtension>(target, EXTENSION_ANDROID) {
            compileSdk = 31

            defaultConfig {
                minSdk = 21
                targetSdk = 31

                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }

            buildTypes {
                getByName("release") {
                    // For an APP module, we have manifest file, which allows minifier (a.k.a. proguard)
                    // to know that which activity classes are being used to launch app and then all the classes
                    // used by activity (even transitively) would be kept and other classes would be removed.
                    // Whereas, there is no such thing present for library modules.
                    // Hence classes of library module are considered unused and hence all the classes
                    // were getting removed from release build. Hence disabling minify for release build.
                    // Ref:
                    //      For minify Enabled = https://stackoverflow.com/questions/65482189
                    //      For consumerProguardFiles = https://stackoverflow.com/questions/49046490/
                    isMinifyEnabled = false
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                    consumerProguardFiles("proguard-rules.pro")
                }
                getByName("debug") {
                    isTestCoverageEnabled = true
                }
            }
            compileOptions {
                sourceCompatibility = PluginConstants.javaVersion
                targetCompatibility = PluginConstants.javaVersion
            }
            setupConfiguration<KotlinJvmOptions>(this as ExtensionAware, "kotlinOptions") {
                jvmTarget = PluginConstants.javaVersion.toString()
            }

            testOptions {
                unitTests.isIncludeAndroidResources = true
                unitTests.all {
                    it.useJUnitPlatform { }
                }
            }
        }
    }

    private fun setupJacocoPlugin(target: Project) {
        target.apply(plugin = "jacoco")

        setupConfiguration<JacocoPluginExtension>(target, "jacoco") {
            toolVersion = PluginConstants.jacocoVersion
        }
        target.afterEvaluate {
            target.extensions.getByName<LibraryExtension>(EXTENSION_ANDROID).libraryVariants
                .find { it.name.toLowerCase(Locale.ROOT) == "debug" }?.apply {
                    val unitTestTask = "testDebugUnitTest"
                    val androidTestCoverageTask = "createDebugCoverageReport"

                    target.tasks.create("jacocoTestReport", JacocoReport::class.java) {
                        dependsOn(unitTestTask, androidTestCoverageTask)
                        group = "Reporting"
                        description = "Generate Jacoco coverage reports for the debug build"

                        reports {
                            html.isEnabled = true
                            xml.isEnabled = true
                            csv.isEnabled = true
                            html.destination = File("${buildDir}/jacoco-reports/html")
                        }

                        val excludeDirs = setOf(
                            // data binding
                            "android/databinding/**/*.class",
                            "**/android/databinding/*Binding.class",
                            "**/android/databinding/*",
                            "**/androidx/databinding/*",
                            "**/BR.*",
                            // android
                            "**/R.class",
                            "**/R$*.class",
                            "**/BuildConfig.*",
                            "**/Manifest*.*",
                            "**/*Test*.*",
                            "android/**/*.*",
                            // butterKnife
                            "**/*\$ViewInjector*.*",
                            "**/*\$ViewBinder*.*",
                            // dagger
                            "**/*_MembersInjector.class",
                            "**/Dagger*Component.class",
                            "**/Dagger*Component\$Builder.class",
                            "**/*Module_*Factory.class",
                            "**/di/module/*",
                            "**/*_Factory*.*",
                            "**/*Module*.*",
                            "**/*Dagger*.*",
                            "**/*Hilt*.*",
                            // kotlin
                            "**/*MapperImpl*.*",
                            "**/*\$ViewInjector*.*",
                            "**/*\$ViewBinder*.*",
                            "**/BuildConfig.*",
                            "**/*Component*.*",
                            "**/*BR*.*",
                            "**/Manifest*.*",
                            "**/*\$Lambda$*.*",
                            "**/*Companion*.*",
                            "**/*Module*.*",
                            "**/*Dagger*.*",
                            "**/*Hilt*.*",
                            "**/*MembersInjector*.*",
                            "**/*_MembersInjector.class",
                            "**/*_Factory*.*",
                            "**/*_Provide*Factory*.*",
                            "**/*Extensions*.*",
                            // sealed and data classes
                            "**/*\$Result.*",
                            "**/*\$Result$*.*"
                        )

                        val javaClasses =
                            fileTree("${buildDir}/intermediates/javac/Debug/classes") {
                                setExcludes(excludeDirs)
                            }
                        val kotlinClasses = fileTree("${buildDir}/tmp/kotlin-classes/Debug") {
                            setExcludes(excludeDirs)
                        }

                        classDirectories.setFrom(files(javaClasses, kotlinClasses))

                        val variantSourceSets =
                            sourceSets.map { it.kotlinDirectories.map { dir -> dir.path } }
                                .flatten()
                        sourceDirectories.setFrom(project.files(variantSourceSets))

                        val androidTestsData =
                            fileTree("${buildDir}/outputs/code_coverage/debugAndroidTest/connected/") {
                                setIncludes(listOf("**/*.ec"))
                            }

                        val executionDataFiles = fileTree(buildDir) {
                            setIncludes(listOf("**/${unitTestTask}.exec"))
                        }

                        executionData(files(executionDataFiles, androidTestsData))
                    }
                }
        }
    }

    private fun setupArtifactoryPublishing(target: Project) {
        target.tasks.create("sourcesJar", Jar::class.java) {
            archiveClassifier.set("sources")
            val javaConfig = target.extensions.getByName<LibraryExtension>(EXTENSION_ANDROID)
                .sourceSets
                .getByName("main")
                .java
            javaConfig.srcDir("src/main/kotlin")
            from(javaConfig.srcDirs)
        }
        setupPublishingExtension(
            target = target,
            publicationName = "aar",
            allowedConfigurationRegex = ALLOWED_CONFIGURATION_REGEX
        ) { "${target.buildDir}/outputs/aar/${target.name}-release.aar" }
    }

    private fun setupResolutionStrategy(target: Project) {
        target.configurations.all {
            resolutionStrategy {
                eachDependency {
                    if ("org.jacoco" == requested.group) {
                        this.useVersion(PluginConstants.jacocoVersion)
                    }
                }
                // Ref: https://stackoverflow.com/questions/63612606/
                force("org.objenesis:objenesis:2.6")
            }
        }
    }
}
