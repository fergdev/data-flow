package io.fergdev.dataflow.plugin.ir

import io.fergdev.dataflow.plugin.DataFlowClassKey
import io.fergdev.dataflow.plugin.DataFlowFunctionKey
import io.fergdev.dataflow.plugin.DataFlowKey
import io.fergdev.dataflow.plugin.DataFlowNames
import io.fergdev.dataflow.plugin.DataFlowNames.Names.updateToParamName
import io.fergdev.dataflow.plugin.DataFlowPropertyKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

//private fun println(string: String) {
////    kotlin.io.println("IR $string")
//}

class DataFlowIrVisitorOld(private val context: IrPluginContext) : IrVisitorVoid() {
    companion object Companion {
        private val DATA_FLOW_ORIGIN = IrDeclarationOrigin.GeneratedByPlugin(DataFlowKey)
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
        if (declaration.origin == DATA_FLOW_ORIGIN && declaration.body == null) {
            println("Visit constructor ${declaration.dump()}")
            declaration.body = generateConstructor(declaration)
        }
    }

    private fun generateConstructor(declaration: IrConstructor): IrBody? {
        val dataFlowClass = declaration.parent as? IrClass ?: return null
        val dataClass = dataFlowClass.parent as IrClass
        val parentConstructor = dataClass.primaryConstructor ?: return null
        val irBuilder = DeclarationIrBuilder(context, declaration.symbol)

        // Assume 'allProp' is the property for the MutableStateFlow holding the parent data class
        val allProp = dataFlowClass.properties.first { it.name == DataFlowNames.Names.AllPropName }

        // Map constructor parameters to parent constructor parameters
        val paramVars = declaration.parameters
        val parentParams = parentConstructor.parameters
        val anyConstructor = context.irBuiltIns.anyClass.owner.primaryConstructor
            ?: return null

        return irBuilder.irBlockBody {
            +irDelegatingConstructorCall(anyConstructor)
            // Call parent constructor with parameters
            val parentCtorCall = irCall(parentConstructor.symbol).apply {
                parentParams.forEachIndexed { idx, param ->
                    arguments[idx] = irGet(paramVars[idx])
                }
            }
            val mutableStateFlowCtor = this@DataFlowIrVisitorOld
                .context
                .referenceFunctions(DataFlowNames.Callable.mutableStateFlow)
                .first()

            val mutableStateFlowCtorCall = irCall(mutableStateFlowCtor)
                .apply { arguments[0] = parentCtorCall }
            val dr = dataFlowClass.thisReceiver!!

            // Assign to 'all' MutableStateFlow
            +irSetField(
                receiver = irGet(dr),
                field = allProp.backingField!!,
                value = mutableStateFlowCtorCall
            )

            // For each parameter, set the corresponding state flow property
            dataFlowClass.properties.forEach { prop ->
                val paramIdx = paramVars.indexOfFirst { it.name == prop.name }
                if (paramIdx != -1 && prop.name != DataFlowNames.Names.AllPropName) {
                    val mutableStateFlowCtorCall = irCall(mutableStateFlowCtor).apply {
                        arguments[0] = irGet(paramVars[paramIdx])
                    }
                    +irSetField(
                        receiver = irGet(dr),
                        field = prop.backingField!!,
                        value = mutableStateFlowCtorCall
                    )
                }
            }

            +IrInstanceInitializerCallImpl(
                startOffset, endOffset,
                classSymbol = dataFlowClass.symbol,
                type = context.irBuiltIns.unitType,
            )
        }
    }

    override fun visitClass(declaration: IrClass) {
        val pluginKey = (declaration.origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey
        if (pluginKey is DataFlowClassKey) {
            println("visitClass ${declaration.kotlinFqName.asString()}")
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitProperty(declaration: IrProperty) {
        super.visitProperty(declaration)
        println("Visit property ${declaration.dumpKotlinLike()}")
        val pluginKey = (declaration.origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey
        if (pluginKey is DataFlowPropertyKey) {
            println("visitProperty: Found property '${declaration.name.identifier}' generated by FIR. Now adding backing field and getter.")

            val originalType = (declaration.getter!!.returnType as IrSimpleType).arguments.first() as IrType
            declaration.backingField = createBackingField(declaration, originalType)
            declaration.getter!!.body = createGetterBody(declaration.getter!!)
        }
        super.visitProperty(declaration)
    }

    private fun createBackingField(property: IrProperty, originalType: IrType): IrField {
        val factory = context.irFactory
        val mutableStateFlowType =
            context.referenceClass(DataFlowNames.Class.mutableStateFlow)!!.typeWith(originalType)

        return factory.createField(
            startOffset = property.startOffset,
            endOffset = property.endOffset,
            origin = IrDeclarationOrigin.GeneratedByPlugin(DataFlowKey),
            name = property.name,
            visibility = DescriptorVisibilities.PRIVATE,
            type = mutableStateFlowType,
            isFinal = true,
            isStatic = false,
            symbol = IrFieldSymbolImpl()
        ).apply {
            this.parent = property.parent
            this.correspondingPropertySymbol = property.symbol
        }
    }

    private fun createGetterBody(getter: IrSimpleFunction): IrBody {
        return DeclarationIrBuilder(context, getter.symbol).irBlockBody {
            +irReturn(
                irGetField(
                    irGet(getter.dispatchReceiverParameter!!), // Get 'this'
                    getter.correspondingPropertySymbol!!.owner.backingField!! // Get the backing field
                )
            )
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        println("visitSimpleFunction ${declaration.kotlinFqName}")
        val pluginKey = (declaration.origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey
        if (pluginKey is DataFlowFunctionKey) {
            println("*** processing")
            val propName = declaration.name.updateToParamName()

            val dataFlowClass = (declaration.parent as IrClass)
            val dataClass = (dataFlowClass.parent as IrClass)

            val allProp =
                dataFlowClass.properties.first { it.name == DataFlowNames.Names.AllPropName }
            val valProp = dataFlowClass.properties.first { it.name == propName }
            val copy = dataClass.functions.first { it.name.identifier == "copy" }

            declaration.generateSetFunctionForProperty2(
                context = context, property = valProp, allProp = allProp, copyFunction = copy
            )
            println("body... " + declaration.dump())
        } else {
//            println("*** ignoring")
        }
    }

    private fun IrFunction.generateSetFunctionForProperty2(
        context: IrPluginContext,
        property: IrProperty,
        allProp: IrProperty,
        copyFunction: IrFunction
    ) {
        body = DeclarationIrBuilder(context, symbol).irBlockBody {
            val newValueParam = parameters[1]
            val valueProperty = context.referenceProperties(
                DataFlowNames.Callable.mutableStateFlowValue
            ).first().owner

            val valueSetter = valueProperty.setter!!
            val valueGetter = valueProperty.getter!!

            // 1. propertyFlow.value = newValue
            val propertyFlow =
                irGetField(irGet(dispatchReceiverParameter!!), property.backingField!!)
            +irCall(valueSetter).apply {
                dispatchReceiver = propertyFlow
                arguments[1] = irGet(newValueParam)
            }

            // 2. allFlow.value = allFlow.value.copy(property = newValue)
            val allFlow =
                irTemporary(
                    irGetField(irGet(dispatchReceiverParameter!!), allProp.backingField!!),
                    nameHint = "all"
                )

            // current data object held in all flow
            val currentAllValue = irTemporary(irCall(valueGetter).apply {
                dispatchReceiver = irGet(allFlow)
            })

            // copy(currentAllValue, override target property)
            val copyFunctionParam = copyFunction.parameters.first { it.name == property.name }
            val copyCall = irCall(copyFunction.symbol).apply {
                dispatchReceiver = irGet(currentAllValue)
                arguments[copyFunctionParam.indexInParameters] = irGet(newValueParam)
            }

            // assign new data object to all flow
            +irCall(valueSetter).apply {
                dispatchReceiver = irGet(allFlow)
                arguments[1] = copyCall
            }
        }
    }
}