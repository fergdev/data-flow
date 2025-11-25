import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.build.config)
    id("java-gradle-plugin")
    `maven-publish`
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    test {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(listOf("testResources"))
    }
}

dependencies {
    implementation(kotlin("gradle-plugin-api"))
    testImplementation(kotlin("test-junit5"))
}

buildConfig {
    packageName(project.group.toString())

    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.group}\"")

    val pluginProject = project(":compiler-plugin")
    buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${pluginProject.group}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${pluginProject.name}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${pluginProject.version}\"")

    val annotationsProject = project(":plugin-annotations")
    buildConfigField(
        type = "String",
        name = "ANNOTATIONS_LIBRARY_COORDINATES",
        expression = "\"${annotationsProject.group}:${annotationsProject.name}:${annotationsProject.version}\""
    )
}

gradlePlugin {
    plugins {
        create("DataFlowPlugin") {
            id = rootProject.group.toString()
            displayName = "DataFlowPlugin"
            description = "DataFlowPlugin"
            implementationClass = "io.fergdev.dataflow.DataFlowGradlePlugin"
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(Config.JDK_TOOL_CHAIN))
    }
}
tasks.withType<KotlinCompile> { kotlin.compilerOptions.jvmTarget = Config.jvmTarget }

publishing {
    repositories {
        mavenLocal()
    }
}
