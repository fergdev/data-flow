plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.build.config)
    alias(libs.plugins.binary.compatibility.validator) apply false
}

allprojects {
    group = "org.jetbrains.kotlin.compiler.plugin.template"
    version = "0.1.0-SNAPSHOT"
}
