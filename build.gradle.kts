// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("org.sonarqube") version "3.3"
    id("jacoco")
}

subprojects {
    apply(plugin ="org.sonarqube")
    sonarqube {
        properties {
            property ("sonar.coverage.jacoco.xmlReportPaths", "${projectDir.parentFile.path}/build/reports/jacoco/codeCoverageReport/codeCoverageReport.xml")
        }
    }
}
apply(plugin="org.sonarqube")

sonarqube {
    properties {
        property("sonar.projectKey", "Hermes_Android_Sonar")
        property("sonar.organization", "ymedialabs")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.projectName","Hermes Android")
    }
}
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.39.1")
        classpath("com.google.gms:google-services:4.3.10")
        classpath("org.jfrog.buildinfo:build-info-extractor-gradle:4.24.20")
        classpath("org.jacoco:org.jacoco.core:0.8.7")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
