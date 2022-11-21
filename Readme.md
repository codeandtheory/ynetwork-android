![Y—Network](https://mpospese.com/wp-content/uploads/2022/08/YNetwork-hero-compact.jpeg)

Looking for the iOS version of Y--Network? Check it out [here](https://github.com/yml-org/ynetwork-ios). 

## Documentation

- Network
    - [Network-Core](./network/core/Readme.md)
    - [Network-Android](./network/android/Readme.md)

## How to publish library changes

The process of publishing a library is 3 steps as follow:

- Clean the project
    - From Terminal, execute `./gradlew clean`
    - From Android Studio, Go to "Build" -> "Clean Project".
- Execute release build
    - From Terminal, execute `./gradlew build`
    - From Android Studio
        - Click on "Gradle" at top of the right sidebar
        - Then click on the Gradle symbol (a.k.a. "Execute Gradle Task" symbol)
        - This will open a popup for the command, enter the command as `gradle build` and then press
          enter.
- Publish the artifactory
    - From Terminal, execute `./gradlew artifactoryPublish`
    - From Android Studio
        - Click on "Gradle" at top of the right sidebar
        - Then click on the Gradle symbol (a.k.a. "Execute Gradle Task" symbol)
        - This will open a popup for the command, enter the command as `gradle artifactoryPublish`
          and then press enter.

## How to use the library

- Sync your project to allow the changes to reflect.
- Now add any dependency you like in your project as follow:
    ```groovy
    implementation 'com.accelerator.network:core:1.0.0'
    ```
    ```groovy
    implementation 'com.accelerator.network:android:1.0.0'
    ```

## How to generate Test coverage report

- To generate test coverage report for whole project:
    - From Terminal, execute `./gradlew build jacocoTestReport`
    - From Android Studio
        - Click on "Gradle" at top of the right sidebar
        - Then click on the Gradle symbol (a.k.a. "Execute Gradle Task" symbol)
        - This will open a popup for the command, enter the command
          as `gradle build jacocoTestReport` and then press enter.
    - This will generate the report for each module and you can find the report in each module's
      build folder at "${buildDir}/jacoco-report/html/index.html".

- To generate test coverage report for a module:
    - From Terminal, execute `./gradlew :moduleName:build :moduleName:jacocoTestReport`
    - From Android Studio
        - Click on "Gradle" at top of the right sidebar
        - Then click on the Gradle symbol (a.k.a. "Execute Gradle Task" symbol)
        - This will open a popup for the command, enter the command
          as `gradle :moduleName:build :moduleName:jacocoTestReport` and then press enter.
    - This will generate the report for that specific module and you can find the report in the
      module's build folder at "${buildDir}/jacoco-report/html/index.html".
