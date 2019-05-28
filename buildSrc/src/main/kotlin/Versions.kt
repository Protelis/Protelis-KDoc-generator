import kotlin.String

/**
 * Find which updates are available by running
 *     `$ ./gradlew buildSrcVersions`
 * This will only update the comments.
 *
 * YOU are responsible for updating manually the dependency version. */
object Versions {
    const val com_gradle_build_scan_gradle_plugin: String = "2.1" // available: "2.3"

    const val com_gradle_plugin_publish_gradle_plugin: String = "0.10.1" 

    const val de_fayard_buildsrcversions_gradle_plugin: String = "0.3.2" 

    const val kotlintest_runner_junit5: String = "3.3.2" 

    const val org_danilopianini_git_sensitive_semantic_versioning_gradle_plugin: String = "0.2.2"

    const val org_danilopianini_publish_on_central_gradle_plugin: String = "0.1.1" 

    const val dokka_gradle_plugin: String = "0.9.18" 

    const val org_jetbrains_dokka_gradle_plugin: String = "0.9.18"

    const val org_jetbrains_kotlin_jvm_gradle_plugin: String = "1.3.21" // available: "1.3.31"

    const val org_jetbrains_kotlin: String = "1.3.21" // available: "1.3.31"

    const val org_jlleitschuh_gradle_ktlint_gradle_plugin: String = "7.3.0" // available: "8.0.0"

    /**
     *
     *   To update Gradle, edit the wrapper file at path:
     *      ./gradle/wrapper/gradle-wrapper.properties
     */
    object Gradle {
        const val runningVersion: String = "5.3.1"

        const val currentVersion: String = "5.4.1"

        const val nightlyVersion: String = "5.6-20190528000143+0000"

        const val releaseCandidate: String = ""
    }
}
