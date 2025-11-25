package io.fergdev.dataflow.plugin

import io.fergdev.dataflow.BuildConfig
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

internal val KEY_ANNOTATIONS =
    CompilerConfigurationKey<String>(
        "The dataflow marker annotations (i.e. io/test/DataFlow) to look for when DataFlowing"
    )

@Suppress("unused")
class DataFlowCommandLineProcessor : CommandLineProcessor {
    private val optionAnnotations =
        CliOption(
            optionName = "annotations",
            valueDescription = "String",
            description = KEY_ANNOTATIONS.toString(),
            required = false,
            allowMultipleOccurrences = false,
        )
    override val pluginId: String = BuildConfig.KOTLIN_PLUGIN_ID

    override val pluginOptions: Collection<CliOption> = listOf(optionAnnotations)

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        when (option) {
            optionAnnotations -> configuration.put(KEY_ANNOTATIONS, value)
        }
    }
}
