package co.accelerator.dependencies.library.kmp

import co.accelerator.dependencies.kotlinLibrary

object KmpProductionDependencies {
    val KotlinCommonStdlib by lazy { kotlinLibrary("stdlib-common") }
    val kotlinCoroutineCore by lazy { "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2" }
}
