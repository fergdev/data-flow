package org.jetbrains.kotlin.compiler.plugin.template.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId

class DataFlowIrGenerationExtension(
    private val classAnnotations: Set<ClassId>,
    private val ignoreAnnotations: Set<ClassId>,
) : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        println("IR DataFlow compiler plugin is running!")
        val transformers = listOf(DataFlowIrVisitor(pluginContext))
        for (transformer in transformers) {
            moduleFragment.acceptChildrenVoid(transformer)
        }
        println("IR DataFlow compiler plugin is over")
    }
}