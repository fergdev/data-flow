package org.jetbrains.kotlin.compiler.plugin.template.ir

import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.compiler.plugin.template.DataFlowClassKey
import org.jetbrains.kotlin.compiler.plugin.template.DataFlowFunctionKey
import org.jetbrains.kotlin.compiler.plugin.template.DataFlowKey
import org.jetbrains.kotlin.compiler.plugin.template.DataFlowNames
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irSetField
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
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private fun println(string: String) {
    kotlin.io.println("IR $string")
}

class BuilderPropertyBacking(
    val holder: IrField,
    val flag: IrField
)

var IrProperty.builderPropertyBacking: BuilderPropertyBacking?
        by irAttribute(copyByDefault = true) // TODO: I am not sure what this does
//by irAttribute(followAttributeOwner = false)


class DataFlowIrVisitor(private val context: IrPluginContext) : IrVisitorVoid() {
    companion object {
        private val DATA_FLOW_ORIGIN = IrDeclarationOrigin.GeneratedByPlugin(DataFlowKey)

        private val ILLEGAL_STATE_EXCEPTION_FQ_NAME =
            FqName("kotlin.IllegalStateException")
        private val ILLEGAL_STATE_EXCEPTION_CLASS_ID =
            ClassId.Companion.topLevel(ILLEGAL_STATE_EXCEPTION_FQ_NAME)
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
//        println("Visit constructor ${declaration.dump()}")
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
        val allProp = dataFlowClass.properties.first { it.name == DataFlowNames.Names.AllFunName }

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
            val mutableStateFlowCtor = this@DataFlowIrVisitor.context.referenceFunctions(
                CallableId(FqName("kotlinx.coroutines.flow"), Name.identifier("MutableStateFlow"))
            ).first()

            val mutableStateFlowCtorCall =
                irCall(mutableStateFlowCtor).apply { arguments[0] = parentCtorCall }
            val dr = dataFlowClass.thisReceiver!!

            // Assign to 'all' MutableStateFlow
            +irSetField(
                receiver = irGet(dr),
//                receiver = irGet(declaration.dispatchReceiverParameter!!),
                field = allProp.backingField!!,
                value = mutableStateFlowCtorCall
            )

            // For each parameter, set the corresponding state flow property
            dataFlowClass.properties.forEach { prop ->
                val paramIdx = paramVars.indexOfFirst { it.name == prop.name }
                if (paramIdx != -1 && prop.name != DataFlowNames.Names.AllFunName) {
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

            for (function in declaration.functions) {
                println("funName ${function.name}")
                val propId =
                    Name.identifier(function.name.identifier.removePrefix("set").lowercase())
                val prop = declaration.properties.firstOrNull { it.name == propId } ?: continue
                println("prop ${prop.dump()}")
            }
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
//        println("visitSimpleFunction ${declaration.kotlinFqName}")
        val pluginKey = (declaration.origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey
        if (pluginKey is DataFlowFunctionKey) {
//            println("visitSimpleFunction ${declaration.kotlinFqName}")
            val propName =
                Name.identifier(
                    declaration.name.identifier.removePrefix("set")
                        .replaceFirstChar { it.lowercase() })

            val dataFlowClass = (declaration.parent as IrClass)
            val dataClass = (dataFlowClass.parent as IrClass)

            val allProp =
                dataFlowClass.properties.first { it.name == DataFlowNames.Names.AllFunName }
            val valProp = dataFlowClass.properties.first { it.name == propName }
            val copy = dataClass.functions.first { it.name.identifier == "copy" }

            declaration.generateSetFunctionForProperty2(
                context = context, property = valProp, allProp = allProp, copy = copy
            )
        } else {
//            println("*** ignoring")
        }
    }

//    private fun IrFunction.generateSetFunctionForProperty(
//        context: IrPluginContext,
//        property: IrProperty,
//        allProp: IrProperty,
//        copy: IrFunction
//    ) {
//        body = DeclarationIrBuilder(context, symbol).irBlockBody {
//            val newValueParam = parameters.first()
//            val valueProp = context.referenceProperties(
//                CallableId(
//                    FqName("kotlinx.coroutines.flow"),
//                    className = FqName("MutableStateFlow"),
//                    Name.identifier("value")
//                )
//            ).first()
//
//            +irCall(valueProp.owner.setter!!).apply {
//                dispatchReceiver =
//                    irGetField(irGet(dispatchReceiverParameter!!), property.backingField!!)
//                arguments[0] = irGet(newValueParam)
//            }
//            +irCall(valueProp.owner.setter!!).apply {
//                dispatchReceiver =
//                    irGetField(irGet(dispatchReceiverParameter!!), allProp.backingField!!)
//                arguments[0] =
//                    irCall(copy).apply {
//                        val index =
//                            copy.parameters.first { it.name == property.name }.indexInParameters
//                        arguments[index] = irGet(newValueParam)
//                    }
//            }
//        }
//    }

    private fun IrFunction.generateSetFunctionForProperty2(
        context: IrPluginContext,
        property: IrProperty,
        allProp: IrProperty,
        copy: IrFunction
    ) {
        body = DeclarationIrBuilder(context, symbol).irBlockBody {
            val newValueParam = parameters[1]

            // Resolve MutableStateFlow.value property (from class, not via CallableId of package)
//            val msfClass =
//                context.referenceClass(FqName("kotlinx.coroutines.flow.MutableStateFlow"))!!.owner
//            val valueProperty = msfClass.properties.first { it.name.asString() == "value" }

            val valueProperty = context.referenceProperties(
                CallableId(
                    FqName("kotlinx.coroutines.flow"),
                    className = FqName("MutableStateFlow"),
                    Name.identifier("value")
                )
            ).first().owner
            val valueSetter = valueProperty.setter!!
            val valueGetter = valueProperty.getter!!

            // 1. propertyFlow.value = newValue
            val propertyFlow =
                irGetField(irGet(dispatchReceiverParameter!!), property.backingField!!)
            +irCall(valueSetter).apply {
                dispatchReceiver = propertyFlow
                arguments[1] = irGet(newValueParam)
//                putValueArgument(0, irGet(newValueParam))
            }

            // 2. allFlow.value = allFlow.value.copy(property = newValue)
            val allFlow = irGetField(irGet(dispatchReceiverParameter!!), allProp.backingField!!)

            // current data object held in all flow
            val currentAllValue = irCall(valueGetter).apply {
                dispatchReceiver = allFlow
            }

            // copy(currentAllValue, override target property)
            val copyCall = irCall(copy.symbol).apply {
                dispatchReceiver = currentAllValue
                // Find copy parameter index for the property and override
                val targetParam = copy.parameters.first { it.name == property.name }
                arguments[targetParam.indexInParameters] = irGet(newValueParam)
            }

            // assign new data object to all flow
            +irCall(valueSetter).apply {
                dispatchReceiver = allFlow
                arguments[1] = copyCall
//                putValueArgument(0, copyCall)
            }
        }
    }

    // Kotlin
    @OptIn(DeprecatedForRemovalCompilerApi::class)
    private fun IrFunction.generateSetFunctionForPropertyFixed(
        context: IrPluginContext,
        property: IrProperty,
        allProp: IrProperty,
        copyFun: IrFunction
    ) {
        body = DeclarationIrBuilder(context, symbol).irBlockBody {
            // Parameter 'i' (the new value)
            val newValueParam = parameters.first()

            // Resolve MutableStateFlow.value property
            val msfClass = context.referenceClass(
                ClassId(
                    FqName("kotlinx.coroutines.flow"),
                    Name.identifier("MutableStateFlow")
                )
            )!!.owner
            val valueProp = msfClass.properties.first { it.name.asString() == "value" }
            val valueGetter = valueProp.getter!!
            val valueSetter = valueProp.setter!!

            // 1. propertyFlow.value = newValue
            val propertyFlow =
                irGetField(irGet(dispatchReceiverParameter!!), property.backingField!!)
            +irCall(valueSetter).apply {
                dispatchReceiver = propertyFlow
                putValueArgument(0, irGet(newValueParam))
            }

            // 2. allFlow.value = allFlow.value.copy(property = newValue)
            val allFlow = irGetField(irGet(dispatchReceiverParameter!!), allProp.backingField!!)

            // current value of the aggregate data object
            val currentAllValue = irCall(valueGetter).apply {
                dispatchReceiver = allFlow
            }

            // copy with overridden target property
            val copyCall = irCall(copyFun.symbol).apply {
                dispatchReceiver = currentAllValue
                val targetParam = copyFun.valueParameters.first { it.name == property.name }
                putValueArgument(targetParam.index, irGet(newValueParam))
            }

            // assign back
            +irCall(valueSetter).apply {
                dispatchReceiver = allFlow.receiver
                putValueArgument(0, copyCall)
            }
        }
    }

}