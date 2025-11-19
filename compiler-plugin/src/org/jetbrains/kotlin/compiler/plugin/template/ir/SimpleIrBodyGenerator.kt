package org.jetbrains.kotlin.compiler.plugin.template.ir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.compiler.plugin.template.DataFlowClassKey
import org.jetbrains.kotlin.compiler.plugin.template.DataFlowKey
import org.jetbrains.kotlin.compiler.plugin.template.fir.SimpleClassGenerator
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class SimpleIrBodyGenerator(pluginContext: IrPluginContext) :
    AbstractTransformerForGenerator(pluginContext) {
    override fun interestedIn(key: GeneratedDeclarationKey?): Boolean {
        return key == SimpleClassGenerator.Key
    }

    override fun generateBodyForFunction(
        function: IrSimpleFunction,
        key: GeneratedDeclarationKey?
    ): IrBody {
        require(function.name == SimpleClassGenerator.FOO_ID.callableName)
        val const =
            IrConstImpl(-1, -1, irBuiltIns.stringType, IrConstKind.String, value = "Hello fergus")
        val returnStatement = IrReturnImpl(-1, -1, irBuiltIns.nothingType, function.symbol, const)
        return irFactory.createBlockBody(-1, -1, listOf(returnStatement))
    }

    override fun generateBodyForConstructor(
        constructor: IrConstructor,
        key: GeneratedDeclarationKey?
    ): IrBody? {
        return generateBodyForDefaultConstructor(constructor)
    }
}

class DataFlowIrVisitor(private val pluginContext: IrPluginContext) : IrVisitorVoid() {
    companion object {
        private val DATA_FLOW_ORIGIN = IrDeclarationOrigin.GeneratedByPlugin(DataFlowKey)

        private val ILLEGAL_STATE_EXCEPTION_FQ_NAME =
            FqName("kotlin.IllegalStateException")
        private val ILLEGAL_STATE_EXCEPTION_CLASS_ID =
            ClassId.topLevel(ILLEGAL_STATE_EXCEPTION_FQ_NAME)
    }

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration,
            is IrFile,
            is IrModuleFragment,
                -> element.acceptChildrenVoid(this)

            else -> Unit
        }
    }

    override fun visitConstructor(declaration: IrConstructor) {
        println("Visit constructor ${declaration.dump()}")
        if (declaration.origin == DATA_FLOW_ORIGIN && declaration.body == null) {
            declaration.body = generateConstructor(declaration)
        }
    }

    private fun generateConstructor(declaration: IrConstructor): IrBody? {
//        addConstructor {
//            isPrimary = true
//            visibility = DescriptorVisibilities.PUBLIC
//        }.apply {
//            buildReceiverParameter {
//                name = Name.special("<this>")
//                type = annotatedClass.defaultType
//            }
//            val params = annotatedClass.primaryConstructor!!.parameters.map { p ->
//                this.addValueParameter(p.name, p.type)
//            }
//
//            body = IrBlockBodyBuilder(
//                context = pluginContext,
//                scope = Scope(this.symbol),
//                startOffset = startOffset,
//                endOffset = endOffset
//            ).blockBody {
//                +irDelegatingConstructorCall(
//                    pluginContext.referenceConstructors(ClassId.Companion.fromString("kotlin/Any"))
//                        .single().owner
//                )
//
//                this@apply.parameters.filter { it.kind == IrParameterKind.Regular }
//                    .forEach { param ->
//                        val property =
//                            this@generateConstructor.properties.single { it.name == param.name }
//                        +irSetField(
//                            receiver = null,
//                            field = property.backingField!!,
//                            value = irCall(mutableStateFlowOf).apply {
//                                typeArguments[0] = param.type
//                                arguments[0] = irGet(param)
//                            }
//                        )
//                    }
//
//                +irSetField(
//                    receiver = null,
//                    field = allProperty.backingField!!,
//                    value = irCall(mutableStateFlowOf).apply {
//                        typeArguments[0] = annotatedClass.defaultType
//                        arguments[0] = irCall(annotatedClass.constructors.single().symbol).apply {
//                            println("well params ${params.map { it.name }}")
//                            params.forEachIndexed { idx, param ->
//                                arguments[idx] = irGet(param)
//                            }
//                        }
//                    }
//                )
//            }
//        }
        return null
    }

    override fun visitClass(declaration: IrClass) {
        println("visitClass ${declaration.dump()}")
        val pluginKey = (declaration.origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey
        if (pluginKey is DataFlowClassKey) {
            val declarations = declaration.declarations

//            val fields = mutableListOf<IrField>()
//            for (property in declarations) {
//                if (property !is IrProperty) continue
//                val backing = generateBacking(declaration, property)
//                updatePropertyAccessors(property, backing)
//
//                property.builderPropertyBacking = backing
//                fields += backing.flag
//                fields += backing.holder
//            }

//            declarations.addAll(0, fields)
        }

        declaration.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        println("visitSimpleFunction ${declaration.dump()}")
//        if (declaration.origin == BUILDABLE_ORIGIN && declaration.body == null) {
//            declaration.body = generateBuildFunction(declaration)
//        }
    }
}
