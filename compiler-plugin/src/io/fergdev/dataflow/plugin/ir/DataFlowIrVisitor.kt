package io.fergdev.dataflow.plugin.ir

import io.fergdev.dataflow.plugin.DataFlowClassKey
import io.fergdev.dataflow.plugin.DataFlowFunctionKey
import io.fergdev.dataflow.plugin.DataFlowKey
import io.fergdev.dataflow.plugin.DataFlowNames
import io.fergdev.dataflow.plugin.DataFlowNames.Names.updateToParamName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name

private fun println(string: String) {
    kotlin.io.println("IR $string")
}

class DataFlowIrVisitor(private val context: IrPluginContext) : IrElementTransformerVoid() {
    companion object {
        private val DATA_FLOW_ORIGIN = IrDeclarationOrigin.GeneratedByPlugin(DataFlowKey)
    }

    // This map is the key. It will hold symbols for the fields we create in Pass 1
    // so the constructor in Pass 2 can find them reliably.
    private val backingFieldSymbols = mutableMapOf<Name, IrFieldSymbol>()

    /**
     * This is the main entry point. We override visitClass to take control of the processing order.
     */
    override fun visitClass(declaration: IrClass): IrStatement {
        val pluginKey = (declaration.origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey
        if (pluginKey !is DataFlowClassKey) {
            // This is not our generated class, let the default visitor handle it.
            return super.visitClass(declaration)
        }

        println("visitClass: Found DataFlow class ${declaration.kotlinFqName}. Taking manual control.")
        backingFieldSymbols.clear() // Clear state for each class

        // --- PASS 1: Generate Backing Fields and Getters ---
        // Iterate over a stable copy of the properties.
        val propertiesToTransform = declaration.properties.toList()
        val newFields = mutableListOf<IrField>()

        for (property in propertiesToTransform) {
            val propertyPluginKey =
                (property.origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey
//            if (propertyPluginKey is DataFlowPropertyKey) {
            println("  [Pass 1] Transforming property '${property.name}'")
            // 1a. Create the backing field and store its symbol.
            val backingField = createBackingField(property)
            newFields.add(backingField)
            backingFieldSymbols[property.name] = backingField.symbol
            property.backingField = backingField // Attach it immediately

            // 1b. Create the getter body.
            property.getter!!.body = createGetterBody(property.getter!!)
//            } else {
//
//                println("  [Pass 1] Transforming ignore property '${property.name}'")
//            }
        }

        // --- SINGLE MODIFICATION STEP ---
        // Now that the loop is done, add all the new fields to the class at once.
//        declaration.declarations.addAll(0, newFields)
        println("visitClass [MODIFICATION]: Added ${newFields.size} new backing fields to ${declaration.name}.")

        // --- PASS 2: Generate Bodies for Constructor and Functions ---
        // Now that the class structure is complete, we can safely fill in the bodies.
        for (member in declaration.declarations) {
            when {
                member is IrConstructor && member.origin == DATA_FLOW_ORIGIN && member.body == null -> {
                    println("  [Pass 2] Generating constructor body.")
                    member.body = generateConstructorBody(member)
                }

                member is IrSimpleFunction && (member.origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey is DataFlowFunctionKey -> {
                    println("  [Pass 2] Generating function body for '${member.name}'.")
                    generateSetFunctionBody2(member)
                }
            }
        }

        // We return the modified declaration. We DO NOT call super.visitClass() because
        // we've handled the entire transformation manually.
        return declaration
    }

    private fun generateSetFunctionBody2(function: IrSimpleFunction) {
        val propName = function.name.updateToParamName()
        val dataFlowClass = function.parent as IrClass
        val dataClass = dataFlowClass.parent as IrClass
        val copyFunction = dataClass.functions.first { it.name.identifier == "copy" }

        // Find the backing field symbols from our reliable map
        val propertyBackingField = backingFieldSymbols[propName]
            ?: error("Could not find backing field for property '$propName'")
        val allBackingField = backingFieldSymbols[DataFlowNames.Names.AllPropName]
            ?: error("Could not find backing field for 'all' property")

        // Get symbols for the MutableStateFlow's 'value' property (getter and setter)
        val valueProperty =
            context.referenceProperties(DataFlowNames.Callable.mutableStateFlowValue).first().owner
        val valueSetter = valueProperty.setter
            ?: error("MutableStateFlow.value has no setter")
        val valueGetter = valueProperty.getter
            ?: error("MutableStateFlow.value has no getter")

        // Use a DeclarationIrBuilder for cleaner and safer IR construction
        function.body = DeclarationIrBuilder(context, function.symbol).irBlockBody {
            // These are the parameters for the function we are building, e.g., updateI(i: Int)
            val newValueParam =
                function.parameters.first { it.kind == IrParameterKind.Regular || it.kind == IrParameterKind.Context }
            val thisReceiver = function.dispatchReceiverParameter!!

            // --- 1. Set the individual property flow: this._i.value = newValue ---
            +irCall(valueSetter).apply {
                dispatchReceiver = irGetField(irGet(thisReceiver), propertyBackingField.owner)
                arguments[1] = irGet(newValueParam)
            }

            // --- 2. Update the 'all' property: this._all.value = this._all.value.copy(i = newValue) ---
            val allFlowField = irGetField(irGet(thisReceiver), allBackingField.owner)

            val currentAllValue = irCall(valueGetter).apply {
                dispatchReceiver = allFlowField
            }
            val copyFunctionParam = copyFunction.parameters.firstOrNull { it.name == propName }

            if (copyFunctionParam != null) {
                // Create the call to the copy function: currentAllValue.copy(i = newValue)
                val copiedAllValue = irCall(copyFunction).apply {
                    dispatchReceiver = currentAllValue
                    arguments[copyFunctionParam.indexInParameters] = irGet(newValueParam)
                }

                // Assign the new, copied data object back to the 'all' flow: this._all.value = copiedAllValue
                +irCall(valueSetter).apply {
//                dispatchReceiver = allFlowField
                    dispatchReceiver = irGetField(irGet(thisReceiver), allBackingField.owner)
                    arguments[1] = copiedAllValue
//                putValueArgument(0, copiedAllValue)
                }
            } else {
                val copyFunctionParam = copyFunction.parameters.firstOrNull { it.name == propName }

                dataFlowClass.properties
                    .filter { it.name != DataFlowNames.Names.AllPropName }
                    .forEach { propertyToUpdate ->
                        val dataClassProp =
                            dataClass.properties.firstOrNull { it.name == propertyToUpdate.name }
                                ?: error("Could not find property ${propertyToUpdate.name}")

                        val newValueParam =
                            function.parameters.first { it.kind == IrParameterKind.Regular || it.kind == IrParameterKind.Context }
                        val functionParam = irGet(newValueParam)
                        val param = irGetField(functionParam, dataClassProp.backingField!!)

                        +irCall(valueSetter).apply {
                            dispatchReceiver = irGetField(irGet(thisReceiver), propertyToUpdate.backingField!!)
                            arguments[1] = param
                        }
                    }
            }
        }
    }

    private fun createBackingField(property: IrProperty): IrField {
        val originalType =
            (property.getter!!.returnType as IrSimpleType).arguments.first() as IrType
        val mutableStateFlowType =
            context.referenceClass(DataFlowNames.Class.mutableStateFlow)!!.typeWith(originalType)

//        return context.irFactory.createField(
//            startOffset = property.startOffset,
//            endOffset = property.endOffset,
//            origin = DATA_FLOW_ORIGIN,
//            name = Name.identifier("_" + property.name), // The IR backend will handle mangling if needed
//            visibility = DescriptorVisibilities.PRIVATE,
//            type = mutableStateFlowType,
//            isFinal = true,
//            isStatic = false,
//            symbol = IrFieldSymbolImpl()
//        ).apply {
//            this.parent = property.parent
//            this.correspondingPropertySymbol = property.symbol
//        }
        // --- THE FIX IS HERE ---
        // Get the existing, default backing field that was created by the frontend.
        val field = property.backingField!!

        // Now, re-configure its properties instead of creating a new one.
        field.name = Name.identifier("_" + property.name.identifier) // Change the name
        field.type = mutableStateFlowType                           // Change the type
        field.visibility = DescriptorVisibilities.PRIVATE             // Ensure it's private
        field.origin = DATA_FLOW_ORIGIN                             // Set our plugin's origin

        // `parent` and `correspondingPropertySymbol` are already correct on the existing field.
        // We just return the re-configured field.
        return field
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

    private fun generateConstructorBody(constructor: IrConstructor): IrBody {
        val dataFlowClass = constructor.parentAsClass
        val dataClass = dataFlowClass.parent as IrClass
        val parentConstructor = dataClass.primaryConstructor!!
        val irBuilder = DeclarationIrBuilder(context, constructor.symbol)

        val mutableStateFlowCtor =
            context.referenceFunctions(DataFlowNames.Callable.mutableStateFlow).first()

        return irBuilder.irBlockBody {
            // 1. Standard boilerplate
            +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.primaryConstructor!!)
            +IrInstanceInitializerCallImpl(
                startOffset,
                endOffset,
                dataFlowClass.symbol,
                context.irBuiltIns.unitType
            )

            // 2. Initialize the 'all' property
            val allBackingFieldSymbol = backingFieldSymbols[DataFlowNames.Names.AllPropName]!!
            val parentCtorCall = irCall(parentConstructor.symbol).apply {
                parentConstructor.parameters.forEach { param ->
                    constructor.parameters.find { it.name == param.name }?.let { ctorParam ->
                        arguments[param.indexInParameters] = irGet(ctorParam)
//                        putValueArgument(param.index, irGet(ctorParam))
                    }
                }
            }
            val allInitializer = irCall(mutableStateFlowCtor).apply {
                arguments[0] = parentCtorCall
//                putValueArgument(0, parentCtorCall)
            }
            +irSetField(
                irGet(dataFlowClass.thisReceiver!!),
                allBackingFieldSymbol.owner,
                allInitializer
            )
            println("    - Set backing field '${allBackingFieldSymbol.owner.name}'")

            // 3. For each parameter, set the corresponding state flow property
            constructor.parameters.forEach { param ->
                val propBackingFieldSymbol = backingFieldSymbols[param.name] ?: return@forEach
                val propInitializer = irCall(mutableStateFlowCtor).apply {
                    arguments[0] = irGet(param)
//                    putValueArgument(0, irGet(param))
                }
                +irSetField(
                    irGet(dataFlowClass.thisReceiver!!),
                    propBackingFieldSymbol.owner,
                    propInitializer
                )
                println("    - Set backing field '${propBackingFieldSymbol.owner.name}'")
            }
        }
    }

    private fun generateSetFunctionBody(function: IrSimpleFunction) {
        val propName = function.name.updateToParamName()
        val dataFlowClass = function.parent as IrClass
        val dataClass = dataFlowClass.parent as IrClass
        val copyFunction = dataClass.functions.first { it.name.identifier == "copy" }

        // Find the backing field symbols from our reliable map
        val propertyBackingField = backingFieldSymbols[propName]!!
        val allBackingField = backingFieldSymbols[DataFlowNames.Names.AllPropName]!!

        val valueProperty =
            context.referenceProperties(DataFlowNames.Callable.mutableStateFlowValue).first().owner
        val valueSetter = valueProperty.setter!!
        val valueGetter = valueProperty.getter!!

        function.body = DeclarationIrBuilder(context, function.symbol).irBlockBody {
            val newValueParam = function.parameters.first()
            val thisReceiver = function.dispatchReceiverParameter!!

            // 1. this._i.value = newValue
            val setPropValue = irCall(valueSetter).apply {
                dispatchReceiver = irGetField(irGet(thisReceiver), propertyBackingField.owner)
                arguments[0] = irGet(newValueParam)
//                putValueArgument(0, irGet(newValueParam))
            }
            +setPropValue

            // 2. this._all.value = this._all.value.copy(i = newValue)
            val allFlowValue = irCall(valueGetter).apply {
                dispatchReceiver = irGetField(irGet(thisReceiver), allBackingField.owner)
            }
            val copyCall = irCall(copyFunction.symbol).apply {
                dispatchReceiver = allFlowValue
                val copyParam = copyFunction.parameters.first { it.name == propName }
                arguments[copyParam.indexInParameters] = irGet(newValueParam)
//                putValueArgument(copyParam.indexInParameters, irGet(newValueParam))
            }
            val setAllValue = irCall(valueSetter).apply {
                dispatchReceiver = irGetField(irGet(thisReceiver), allBackingField.owner)
                arguments[0] = copyCall
//                putValueArgument(0, copyCall)
            }
            +setAllValue
        }
    }
}