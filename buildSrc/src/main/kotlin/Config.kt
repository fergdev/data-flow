import org.jetbrains.kotlin.gradle.dsl.JvmTarget

public object Config {
    const val group = "io.fergdev.dataflow"

    const val name = "Kotlin Data Flow"

    const val majorRelease = 0
    const val minorRelease = 1
    const val patch = 0
    const val postfix = "-alpha.1"

    const val majorVersionName = "$majorRelease.$minorRelease.$patch"
    const val versionName = "$majorVersionName$postfix"

    const val inceptionYear = "2025"

    const val description =
        """Kotlin compiler plugin to turn data classes into reactive objects"""

    const val url = "https://github.com/fergdev/dataflow"
    const val connection = "scm:git:git://github.com/fergdev/dataflow.git"
    const val developerConnection = "scm:git:ssh://git@github.com/fergdev/dataflow.git"

    const val licenseName = "The Apache License, Version 2.0"
    const val licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    const val licenseDistribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"

    const val developerId = "fergdev"
    const val developerName = "Fergus Hewson"

    const val JDK_TOOL_CHAIN = 17
    val jvmTarget = JvmTarget.JVM_17
}
