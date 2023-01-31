package co.accelerator.dependencies.library.android

import co.accelerator.dependencies.library.kmp.KmpDependencies

object AndroidLibraryProductionDependencies {
    val AdobeAnalytics by lazy { "com.adobe.marketing.mobile:analytics:1.2.9" }
    val AdobeSdk by lazy { "com.adobe.marketing.mobile:sdk-core:1.9.0" }
    val AppCompat by lazy { "androidx.appcompat:appcompat:1.4.1" }

    val CoreKotlinExtension by lazy { "androidx.core:core-ktx:1.7.0" }
    val DataStore by lazy { "androidx.datastore:datastore-preferences:1.0.0" }

    val FirebaseBOM by lazy { "com.google.firebase:firebase-bom:29.0.0" }
    val FirebaseAnalytics by lazy { "com.google.firebase:firebase-analytics:20.0.2" }

    // Ref: https://stackoverflow.com/a/49738127
    val JacocoRuntime by lazy { "org.jacoco:org.jacoco.agent:0.8.7:runtime" }

    val kotlinCoroutineCore by lazy { KmpDependencies.prod.kotlinCoroutineCore }

    val LiveDataExtensions by lazy { "androidx.lifecycle:lifecycle-livedata-ktx:${AndroidLibraryDependenciesVersion.lifecycle}" }

    val OkHttp by lazy { "com.squareup.okhttp3:okhttp:4.9.1" }
}
