package org.jetbrains.kotlin.compiler.plugin.template.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.name.ClassId

class DataFlowFirExtensionRegistrar(
    private val annotations: Set<ClassId>,
    private val ignoreAnnotations: LinkedHashSet<ClassId>
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +DataFlowFirBuiltins.getFactory(annotations, ignoreAnnotations)
        +::DataFlowFirDeclarationGenerationExtension
        +::DataFlowFirCheckers
    }
}
