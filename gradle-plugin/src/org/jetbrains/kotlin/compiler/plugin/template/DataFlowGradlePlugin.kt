package org.jetbrains.kotlin.compiler.plugin.template

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@Suppress("unused")
class DataFlowGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        target.extensions.create("dataFlow", DataFlowExtension::class.java)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = BuildConfig.KOTLIN_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = BuildConfig.KOTLIN_PLUGIN_GROUP,
        artifactId = BuildConfig.KOTLIN_PLUGIN_NAME,
        version = BuildConfig.KOTLIN_PLUGIN_VERSION,
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(DataFlowExtension::class.java)

        val annotations =
            extension.annotations.zip(extension.annotations, Set<String>::plus)

        val unredactedAnnotations =
            extension.ignoreAnnotations.zip(extension.ignoreAnnotations, Set<String>::plus)

        // Default annotation is used, so add it as a dependency
        // Note only multiplatform, jvm/android, and js are supported. Anyone else is on their own.
        val useDefaults =
            annotations.getOrElse(DEFAULT_ANNOTATION_SET) == DEFAULT_ANNOTATION_SET ||
                    unredactedAnnotations.getOrElse(DEFAULT_IGNORE_ANNOTATION_SET) ==
                    DEFAULT_IGNORE_ANNOTATION_SET
        if (useDefaults) {
            project.dependencies.add(
                kotlinCompilation.implementationConfigurationName,
                "io.fergdev.dataflow:dataflow-compiler-plugin-annotations:${BuildConfig.KOTLIN_PLUGIN_VERSION}",
            )
        }

        return project.provider {
            listOf(
                SubpluginOption(
                    key = "dataFlowAnnotations",
                    value = annotations.get().joinToString(":")
                ),
            )
        }
    }
}
