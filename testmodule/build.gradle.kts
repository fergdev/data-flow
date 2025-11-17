@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
}

kotlin {
    androidLibrary {
        namespace = "io.fergdev.dataflow.testmodule"
        compileSdk = 36
        minSdk = 24

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    val xcfName = "testmoduleKit"
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = xcfName
        }
    }
    jvm()
    wasmJs {
        browser()
        nodejs()
        binaries.library()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(project(":plugin-annotations"))
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.runner)
                implementation(libs.core)
                implementation(libs.ext.junit)
            }
        }

        iosMain {
            dependencies {
            }
        }
    }
}

// 1) Build a *constant* path string to the plugin jar
// Make sure your compiler-plugin jar name is stable (see note below)
val pluginProject = project(":compiler-plugin")
val pluginJarPath: String =
    rootProject.layout.projectDirectory
        .dir("compiler-plugin/build/libs")
        .file("${pluginProject.name}-${pluginProject.version}.jar")
        .asFile
        .absolutePath

// 2) Apply the plugin to all compilations, and depend on :compiler-plugin:jar
kotlin.targets.withType<KotlinJvmTarget>().configureEach {
    compilations.configureEach {
        compileTaskProvider.configure {
            // ensure the plugin jar is built before compiling
            dependsOn(":compiler-plugin:jar")

            if(this is KotlinCompilationTask<*>) {
                compilerOptions.freeCompilerArgs.add(
                    "-Xplugin=$pluginJarPath"
                )
            }
        }
    }
}