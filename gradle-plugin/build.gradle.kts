import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.build.config)
    id("java-gradle-plugin")
    alias(libs.plugins.maven.publish)
    id("signing")
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

    val annotationsProject = project(":annotations")
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

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
    coordinates(Config.group, "gradle-plugin", Config.versionName)

    pom {
        name.set(Config.name)
        description.set(Config.description)
        inceptionYear = Config.inceptionYear

        url.set(Config.url)
        licenses {
            license {
                name = Config.licenseName
                url = Config.licenseUrl
                distribution = Config.licenseDistribution
            }
        }
        scm {
            url.set(Config.url)
            connection.set(Config.connection)
            developerConnection.set(Config.developerConnection)
        }
        developers {
            developer {
                id.set(Config.developerId)
                name.set(Config.developerName)
            }
        }
    }
}

signing {
    val key = providers.gradleProperty("signingInMemoryKey").orNull
    val pass = providers.gradleProperty("signingInMemoryKeyPassword").orNull
    if (!key.isNullOrBlank()) {
        useInMemoryPgpKeys(key, pass)
    }
    sign(publishing.publications)
}

tasks.withType<Sign>().configureEach {
    onlyIf { !project.version.toString().endsWith("SNAPSHOT") }
}
