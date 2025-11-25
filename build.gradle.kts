plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId) apply false
    id(libs.plugins.kotlin.jvm.get().pluginId) apply false
    alias(libs.plugins.build.config)
    alias(libs.plugins.binary.compatibility.validator) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
}

allprojects {
    group = Config.group
    version = Config.versionName
}
