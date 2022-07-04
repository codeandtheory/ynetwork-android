package com.accelerator.dependencies.library.kmp

import com.accelerator.dependencies.kotlinLibrary

object JvmTestDependencies {
    val JUnit by lazy { "org.junit.jupiter:junit-jupiter:5.5.2" }
    val KotlinCommonTest by lazy { kotlinLibrary("test-common") }
    val Hamcrest by lazy { "org.hamcrest:hamcrest-all:1.3" }
    val Mockk by lazy { "io.mockk:mockk:1.12.1" }
}
