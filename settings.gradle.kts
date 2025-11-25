pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

include(":compiler-plugin")
include(":gradle-plugin")
include(":plugin-annotations")
include(":example")

rootProject.name = "dataflow"
