package co.accelerator.dependencies.sample

object AndroidSampleAppDependencies {
    val prod = AndroidSampleAppProductionDependencies
    val integrationTest = AndroidSampleAppTestDependencies
    val unitTest = AndroidSampleAppJvmUnitDependencies
}
