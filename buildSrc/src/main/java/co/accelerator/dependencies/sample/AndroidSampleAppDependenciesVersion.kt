package co.accelerator.dependencies.sample

object AndroidSampleAppDependenciesVersion {
    const val androidxActivity = "1.4.0"
    const val compose = "1.2.0-alpha02"
    const val hilt =
        "2.39.1" // Due to build issue with KMP for Hilt 2.40.*, sticking with 2.39.* version
    const val hiltWorkManager = "1.0.0"
    const val workManager = "2.7.1"
    const val yNetwork = "1.0.0"
}