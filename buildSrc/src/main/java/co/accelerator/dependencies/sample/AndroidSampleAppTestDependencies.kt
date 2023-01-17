package co.accelerator.dependencies.sample

object AndroidSampleAppTestDependencies {

    val ComposeUiTestJunit by lazy { "androidx.compose.ui:ui-test-junit4:${AndroidSampleAppDependenciesVersion.compose}" }
    val ComposeUiTestManifest by lazy { "androidx.compose.ui:ui-test-manifest:${AndroidSampleAppDependenciesVersion.compose}" }

    val EspressoCore by lazy { "androidx.test.espresso:espresso-core:3.4.0" }
    val EspressoIntents by lazy { "androidx.test.espresso:espresso-intents:3.4.0" }
    val FragmentTesting by lazy { "androidx.fragment:fragment-testing:1.4.1" }
    val HiltAnnotationCompiler by lazy { "com.google.dagger:hilt-android-compiler:${AndroidSampleAppDependenciesVersion.hilt}" }
    val HiltTest by lazy { "com.google.dagger:hilt-android-testing:${AndroidSampleAppDependenciesVersion.hilt}" }
}
