package com.accelerator.dependencies.library.android

import com.accelerator.dependencies.library.kmp.JvmTestDependencies

object AndroidLibraryJvmUnitTestDependencies {
    val AndroidxCoreTesting by lazy { "androidx.arch.core:core-testing:2.1.0" }
    val CoroutinesTest by lazy { "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0" }
    val Gson by lazy { "com.google.code.gson:gson:2.8.9" }
    val Hamcrest by lazy { JvmTestDependencies.Hamcrest }
    val JUnit by lazy { JvmTestDependencies.JUnit }
    val KotlinxCoroutinesAndroid by lazy { "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2" }
    val Mockk by lazy { JvmTestDependencies.Mockk }
}
