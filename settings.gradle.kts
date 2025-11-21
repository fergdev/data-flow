pluginManagement {
    repositories {
        maven("https://digital.artifacts.nz.thenational.com/repository/gradle-plugins")
        maven("https://digital.artifacts.nz.thenational.com/repository/digital-maven")
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://digital.artifacts.nz.thenational.com/repository/gradle-plugins")
        maven("https://digital.artifacts.nz.thenational.com/repository/digital-maven")
        google()
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "dataflow"

include("compiler-plugin")
include("gradle-plugin")
include("plugin-annotations")
include(":testmodule")
