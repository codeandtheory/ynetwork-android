import co.accelerator.dependencies.library.android.AndroidLibraryDependencies
import co.accelerator.dependencies.sample.AndroidSampleAppDependencies

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "co.yml.chat"
        minSdk = 21
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "co.yml.chat.TestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = co.accelerator.dependencies.sample.AndroidSampleAppDependenciesVersion.compose
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(AndroidLibraryDependencies.prod.CoreKotlinExtension)
    implementation(AndroidLibraryDependencies.prod.AppCompat)
    implementation(AndroidSampleAppDependencies.prod.Material)
    implementation(AndroidSampleAppDependencies.prod.ConstraintLayout)
    implementation(AndroidLibraryDependencies.prod.LiveDataExtensions)
    implementation(AndroidSampleAppDependencies.prod.ViewModelExtensions)
    implementation(AndroidSampleAppDependencies.prod.Socket) {
        exclude("org.json", "json")
    }
    implementation(AndroidSampleAppDependencies.prod.LifecycleRuntimeExtensions)
    implementation(AndroidSampleAppDependencies.prod.Gson)
    implementation(AndroidSampleAppDependencies.prod.ComposeNavigation)
    implementation(AndroidSampleAppDependencies.prod.ComposeHilt)
    implementation(AndroidSampleAppDependencies.prod.ComposeUi)
    implementation(AndroidSampleAppDependencies.prod.ComposeMaterial)
    implementation(AndroidSampleAppDependencies.prod.ComposeMaterialIcons)
    implementation(AndroidSampleAppDependencies.prod.ComposeUiToolingPreview)
    implementation(AndroidSampleAppDependencies.prod.ComposeRuntimeLiveData)
    implementation(AndroidSampleAppDependencies.prod.ComposeConstraintLayout)
    implementation(AndroidSampleAppDependencies.prod.ComposeUiTooling)
    implementation(AndroidSampleAppDependencies.prod.Hilt)
    implementation(AndroidSampleAppDependencies.prod.HiltWorkManager)
    implementation(AndroidSampleAppDependencies.prod.WorkManagerCoroutine)
    implementation(AndroidSampleAppDependencies.prod.YNetwork)

    kapt(AndroidSampleAppDependencies.prod.HiltAnnotationCompiler)
    kapt(AndroidSampleAppDependencies.prod.HiltWorkManagerAnnotationCompiler)

    testImplementation(AndroidSampleAppDependencies.integrationTest.HiltTest)
    testImplementation(AndroidLibraryDependencies.unitTest.JUnit)

    kaptTest(AndroidSampleAppDependencies.integrationTest.HiltAnnotationCompiler)

    androidTestImplementation(AndroidLibraryDependencies.integrationTest.AndroidJunit)
    androidTestImplementation(AndroidSampleAppDependencies.integrationTest.EspressoCore)
    androidTestImplementation(AndroidSampleAppDependencies.integrationTest.EspressoIntents)
    androidTestImplementation(AndroidSampleAppDependencies.integrationTest.HiltTest)
    androidTestImplementation(AndroidSampleAppDependencies.integrationTest.ComposeUiTestJunit)

    kaptAndroidTest(AndroidSampleAppDependencies.integrationTest.HiltAnnotationCompiler)
}
