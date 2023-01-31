package co.accelerator.dependencies.library.android

object AndroidLibraryTestDependencies {
    val AndroidJunit by lazy { "androidx.test.ext:junit:1.1.3" }
    val MockkAndroid by lazy { "io.mockk:mockk-android:1.12.1" }
    val TestRunner by lazy { "androidx.test:runner:1.4.0" }
    val Annotation by lazy { "androidx.annotation:annotation:1.3.0" }

    val AndroidxCoreTesting by lazy { AndroidLibraryJvmUnitTestDependencies.AndroidxCoreTesting }
    val CoroutinesTest by lazy { AndroidLibraryJvmUnitTestDependencies.CoroutinesTest }
}
