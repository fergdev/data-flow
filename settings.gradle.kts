pluginManagement {
    repositories {
        maven("https://digital.artifacts.nz.thenational.com/repository/gradle-plugins")
        maven("https://digital.artifacts.nz.thenational.com/repository/digital-maven")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    
}

dependencyResolutionManagement {
    repositories {
        maven("https://digital.artifacts.nz.thenational.com/repository/gradle-plugins")
        maven("https://digital.artifacts.nz.thenational.com/repository/digital-maven")
        google()
        mavenCentral()
    }
}

rootProject.name = "compiler-plugin-template"

include("compiler-plugin")
include("gradle-plugin")
include("plugin-annotations")
include(":testmodule")
include(":testmodule2")
