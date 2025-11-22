plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.build.config)
    alias(libs.plugins.binary.compatibility.validator) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.lint) apply false
}

allprojects {
    group = "io.fergdev.dataflow"
    version = "0.1.0-SNAPSHOT"
}
