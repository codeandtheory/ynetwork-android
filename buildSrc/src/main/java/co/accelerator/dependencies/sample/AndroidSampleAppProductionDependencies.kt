package co.accelerator.dependencies.sample

import co.accelerator.dependencies.library.android.AndroidLibraryDependencies
import co.accelerator.dependencies.library.android.AndroidLibraryDependenciesVersion

object AndroidSampleAppProductionDependencies {
    val AndroidxActivityCompose by lazy { "androidx.activity:activity-compose:${AndroidSampleAppDependenciesVersion.androidxActivity}" }
    val AndroidxActivityExtensions by lazy { "androidx.activity:activity-ktx:${AndroidSampleAppDependenciesVersion.androidxActivity}" }

    val ComposeConstraintLayout by lazy { "androidx.constraintlayout:constraintlayout-compose:1.0.0"  }
    val ComposeHilt by lazy { "androidx.hilt:hilt-navigation-compose:1.0.0-rc01" }
    val ComposeUi by lazy { "androidx.compose.ui:ui:${AndroidSampleAppDependenciesVersion.compose}" }
    val ComposeMaterial by lazy { "androidx.compose.material:material:${AndroidSampleAppDependenciesVersion.compose}" }
    val ComposeMaterialIcons by lazy { "androidx.compose.material:material-icons-extended:${AndroidSampleAppDependenciesVersion.compose}" }
    val ComposeNavigation by lazy { "androidx.navigation:navigation-compose:2.5.0-alpha01" }
    val ComposeRuntimeLiveData by lazy { "androidx.compose.runtime:runtime-livedata:${AndroidSampleAppDependenciesVersion.compose}" }
    val ComposeUiTooling by lazy { "androidx.compose.ui:ui-tooling:${AndroidSampleAppDependenciesVersion.compose}" }
    val ComposeUiToolingPreview by lazy { "androidx.compose.ui:ui-tooling-preview:${AndroidSampleAppDependenciesVersion.compose}" }
    val ConstraintLayout by lazy { "androidx.constraintlayout:constraintlayout:2.1.3" }

    val FragmentNavigation by lazy { "androidx.navigation:navigation-fragment-ktx:2.4.0" }

    val Gson by lazy { AndroidLibraryDependencies.unitTest.Gson }

    val Hilt by lazy { "com.google.dagger:hilt-android:${AndroidSampleAppDependenciesVersion.hilt}" }
    val HiltAnnotationCompiler by lazy { "com.google.dagger:hilt-android-compiler:${AndroidSampleAppDependenciesVersion.hilt}" }

    // Ref: https://developer.android.com/training/dependency-injection/hilt-jetpack#workmanager
    val HiltWorkManager by lazy { "androidx.hilt:hilt-work:${AndroidSampleAppDependenciesVersion.hiltWorkManager}" }
    val HiltWorkManagerAnnotationCompiler by lazy { "androidx.hilt:hilt-compiler:${AndroidSampleAppDependenciesVersion.hiltWorkManager}" }
    val Jackson by lazy { "com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0" }

    val LifecycleRuntimeExtensions by lazy { "androidx.lifecycle:lifecycle-runtime-ktx:${AndroidLibraryDependenciesVersion.lifecycle}" }
    val Material by lazy { "com.google.android.material:material:1.5.0" }

    val NavigationUIExtensions by lazy { "androidx.navigation:navigation-ui-ktx:2.4.0" }
    val ViewModelExtensions by lazy { "androidx.lifecycle:lifecycle-viewmodel-ktx:${AndroidLibraryDependenciesVersion.lifecycle}" }

    val Socket by lazy { "io.socket:socket.io-client:2.0.0" }

    val WorkManagerCoroutine by lazy { "androidx.work:work-runtime-ktx:${AndroidSampleAppDependenciesVersion.workManager}" }

    val YNetwork by lazy { "com.accelerator.network:android:${AndroidSampleAppDependenciesVersion.yNetwork}" }
}
