import co.accelerator.dependencies.library.kmp.KmpDependencies

plugins {
    id("co.accelerator.library.kmp")
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(KmpDependencies.prod.KotlinCommonStdlib)
                api(KmpDependencies.prod.kotlinCoroutineCore)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(KmpDependencies.jvmTest.KotlinCommonTest)
                implementation(KmpDependencies.jvmTest.JUnit)
                implementation(KmpDependencies.jvmTest.Hamcrest)
                implementation(KmpDependencies.jvmTest.Mockk)
            }
        }
    }
}

val jvmTest by tasks.getting(Test::class) {
    useJUnitPlatform { }
}
