package org.jetbrains.kotlin.compiler.plugin.template

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.compiler.plugin.template.fir.DataFlowFirExtensionRegistrar
import org.jetbrains.kotlin.compiler.plugin.template.ir.DataFlowIrGenerationExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.name.ClassId

@OptIn(ExperimentalCompilerApi::class)
class DataFlowComponentRegistrar : CompilerPluginRegistrar() {

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val annotations = configuration[KEY_ANNOTATIONS]?.splitToSequence(":")
            ?: sequenceOf("io.fergdev.dataflow.DataFlow")
        val annotationsMapped = annotations.mapTo(LinkedHashSet()) { ClassId.fromString(it) }
        val ignoreAnnotations = configuration[KEY_ANNOTATIONS]?.splitToSequence(":")
            ?: sequenceOf("io.fergdev.dataflow.DataFlowIgnore")
        val ignoreAnnotationsMapped = ignoreAnnotations.mapTo(LinkedHashSet()) { ClassId.fromString(it) }

        FirExtensionRegistrarAdapter.registerExtension(
            DataFlowFirExtensionRegistrar(annotationsMapped, ignoreAnnotationsMapped))
        IrGenerationExtension.registerExtension(
            DataFlowIrGenerationExtension(annotationsMapped, ignoreAnnotationsMapped))
    }

    override val supportsK2: Boolean = true
}
