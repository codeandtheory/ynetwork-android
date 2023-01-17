import co.accelerator.dependencies.library.android.AndroidLibraryDependencies

plugins {
    id("co.accelerator.library.android")
}

dependencies {
    implementation(AndroidLibraryDependencies.prod.CoreKotlinExtension)
    implementation(AndroidLibraryDependencies.prod.OkHttp)
    implementation(AndroidLibraryDependencies.prod.JacocoRuntime)

    api(project(":network:core"))
    api(AndroidLibraryDependencies.prod.LiveDataExtensions)

    testImplementation(AndroidLibraryDependencies.unitTest.JUnit)
    testImplementation(AndroidLibraryDependencies.unitTest.Hamcrest)
    testImplementation(AndroidLibraryDependencies.unitTest.Mockk)
    testImplementation(AndroidLibraryDependencies.unitTest.AndroidxCoreTesting)
    testImplementation(AndroidLibraryDependencies.unitTest.KotlinxCoroutinesAndroid)
    testImplementation(AndroidLibraryDependencies.unitTest.CoroutinesTest)

    androidTestImplementation(AndroidLibraryDependencies.integrationTest.AndroidJunit)
    androidTestImplementation(AndroidLibraryDependencies.integrationTest.TestRunner)
    androidTestImplementation(AndroidLibraryDependencies.integrationTest.MockkAndroid)
}
