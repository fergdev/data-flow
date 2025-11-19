package org.jetbrains.kotlin.compiler.plugin.template.ir

import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.compiler.plugin.template.print
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildReceiverParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private fun println(string: String) {
    kotlin.io.println("IR $string")
}

internal class DataFlowClassVisitor(
    private val pluginContext: IrPluginContext,
    private val classAnnotations: Set<ClassId>,
    private val ignoreClassAnnotations: Set<ClassId>,
) : IrElementTransformerVoid() {
    private val generatedClasses = mutableListOf<IrClass>()
    private val dataFlowAnnotation = FqName("io.fergdev.dataflow.annotations.DataFlow")
    private val fqNameMutableStateFlow = FqName("kotlinx.coroutines.flow.MutableStateFlow")
    private val fqNameStateFlow = FqName("kotlinx.coroutines.flow.StateFlow")

    private val classIdMutableStateFlow =
        ClassId(FqName("kotlinx.coroutines.flow"), FqName("MutableStateFlow"), false)

    private val msfCId = CallableId(
        packageName = FqName("kotlinx.coroutines.flow"),
        callableName = Name.identifier("MutableStateFlow")
    )

    private val msfCId2 = CallableId(
        packageName = FqName("kotlinx.coroutines.flow"),
        className = FqName("MutableStateFlow"),
        callableName = Name.identifier("value")
    )

    private val sfCId = CallableId(
        packageName = FqName("kotlinx.coroutines.flow"),
        className = FqName("StateFlow"),
        callableName = Name.identifier("value")
    )

    private val mutableStateFlowValue =
        pluginContext.referenceProperties(msfCId2)
            .single { it.owner.name.asString() == "value" }.owner.setter!!
    private val stateFlowValue =
        pluginContext.referenceProperties(sfCId)
            .single { it.owner.name.asString() == "value" }.owner.getter!!

    private val mutableStateFlowOf = pluginContext.referenceFunctions(msfCId)
        .single { it.owner.parameters.size == 1 }

    override fun visitClass(declaration: IrClass): IrStatement {
        println("visiting ${declaration.name}")
        if (declaration.annotations.any { it.type.classFqName == dataFlowAnnotation }) {
            val generatedClass = generateDataFlowClass(declaration)
            generatedClasses.add(generatedClass)
        }
        return super.visitClass(declaration)
    }

    override fun visitFile(declaration: IrFile): IrFile {
        println("visiting ${declaration.name}")
        val result = super.visitFile(declaration)
        generatedClasses.forEach {
            println("Adding generated class")
            if (it.parent == declaration) {
                println("It eq")
                result.addChild(it)
            } else {
                println("It not eq")
            }
        }
        generatedClasses.clear()
        return result
    }

    /**
     * The main generation logic. Creates the '...Flow' class.
     */
    private fun generateDataFlowClass(annotatedClass: IrClass): IrClass {
        val factory = IrFactoryImpl
        val generatedClass = factory.createClass(
            startOffset = annotatedClass.startOffset,
            endOffset = annotatedClass.endOffset,
            origin = IrDeclarationOriginImpl("DataFlow"),
            name = Name.identifier("${annotatedClass.name.asString()}Flow"),
            visibility = DescriptorVisibilities.PUBLIC,
            modality = Modality.FINAL,
            kind = ClassKind.CLASS,
            isInner = false,
            isCompanion = false,
            isData = false,
            isExternal = false,
            isFun = false,
            isExpect = false,
            isValue = false,
            symbol = IrClassSymbolImpl()
        ).apply {
            parent = annotatedClass.parent
            createThisReceiverParameter()
        }

        val allProperty = generatedClass.addStateFlowProperty("all", annotatedClass.defaultType)
        annotatedClass.primaryConstructor?.parameters?.forEach { param ->
            val propertyName = param.name.asString()
            val propertyType = param.type

            val flowProperty = generatedClass.addStateFlowProperty(
                name = propertyName,
                type = propertyType
            )
            generatedClass.addSetField(
                propertyName = propertyName,
                propertyType = propertyType,
                annotatedClass = annotatedClass,
                allProperty = allProperty
            )
        }
        generatedClass.generateConstructor(annotatedClass, allProperty)

        generatedClass.print()
        return generatedClass
    }

    private fun IrClass.generateConstructor(annotatedClass: IrClass, allProperty: IrProperty) {
        addConstructor {
            isPrimary = true
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            buildReceiverParameter {
                name = Name.special("<this>")
                type = annotatedClass.defaultType
            }
            val params = annotatedClass.primaryConstructor!!.parameters.map { p ->
                this.addValueParameter(p.name, p.type)
            }

            body = IrBlockBodyBuilder(
                context = pluginContext,
                scope = Scope(this.symbol),
                startOffset = startOffset,
                endOffset = endOffset
            ).blockBody {
                +irDelegatingConstructorCall(
                    pluginContext.referenceConstructors(ClassId.Companion.fromString("kotlin/Any"))
                        .single().owner
                )

                this@apply.parameters.filter { it.kind == IrParameterKind.Regular }
                    .forEach { param ->
                        val property =
                            this@generateConstructor.properties.single { it.name == param.name }
                        +irSetField(
                            receiver = null,
                            field = property.backingField!!,
                            value = irCall(mutableStateFlowOf).apply {
                                typeArguments[0] = param.type
                                arguments[0] = irGet(param)
                            }
                        )
                    }

                +irSetField(
                    receiver = null,
                    field = allProperty.backingField!!,
                    value = irCall(mutableStateFlowOf).apply {
                        typeArguments[0] = annotatedClass.defaultType
                        arguments[0] = irCall(annotatedClass.constructors.single().symbol).apply {
                            println("well params ${params.map { it.name }}")
                            params.forEachIndexed { idx, param ->
                                arguments[idx] = irGet(param)
                            }
                        }
                    }
                )
            }
        }
    }

    private fun IrClass.addSetField(
        propertyName: String,
        propertyType: IrType,
        annotatedClass: IrClass,
        allProperty: IrProperty
    ) {
        addFunction {
            name = Name.identifier("set${propertyName.replaceFirstChar { it.uppercase() }}")
            returnType = pluginContext.irBuiltIns.unitType
        }.apply {
            parent = this@addSetField
            val dispatchReceiverParameter = this@addSetField.thisReceiver!!.copyTo(
                irFunction = this,
                kind = IrParameterKind.DispatchReceiver
            )

            this.parameters = listOf(dispatchReceiverParameter)

            val parameter = addValueParameter(propertyName, propertyType)
            println(parameter.name)
            body = IrBlockBodyBuilder(
                context = pluginContext,
                scope = Scope(this.symbol),
                startOffset = startOffset,
                endOffset = endOffset
            ).blockBody {
                val thisExpr = irGet(dispatchReceiverParameter)

                val copyFunction = annotatedClass.functions.single { it.name.asString() == "copy" }
                val copyFunctionParameter =
                    copyFunction.parameters.single { it.name == parameter.name }

//                val getThis = irGet(this@addSetField.thisReceiver!!)
//                val getAllProperty = irGetField(getThis, allProperty.backingField!!)
//                val getCurrentAllValue = irCall(stateFlowValue).apply {
//                    this.dispatchReceiver = getAllProperty
//                }

                // all: MutableStateFlow<MyData>
                val allFlowField = irGetField(thisExpr, allProperty.backingField!!)

                // current MyData value from all.value
                val currentAllValue = irCall(stateFlowValue).apply {
                    dispatchReceiver = allFlowField
                }
                val newAllValue = irCall(copyFunction).apply {
                    this.dispatchReceiver = currentAllValue
                    arguments[copyFunctionParameter.indexInParameters] = irGet(parameter)
                }

                +irCall(mutableStateFlowValue).apply {
                    this.dispatchReceiver = allFlowField
                    arguments[1] = newAllValue
                }
            }
            this.print()
        }
    }

    @OptIn(DeprecatedForRemovalCompilerApi::class)
    private fun IrClass.addStateFlowProperty(
        name: String,
        type: IrType
    ): IrProperty {
        val factory = pluginContext.irFactory
        // Get a reference to the MutableStateFlow<T> type.
        val mutableStateFlowType = pluginContext.referenceClass(classIdMutableStateFlow)!!
            .typeWith(type)

        // 1. Create the IR Property.
        val property = factory.createProperty(
            startOffset = this.startOffset,
            endOffset = this.endOffset,
            origin = IrDeclarationOriginImpl("DataFlow"), // An IR Origin
            name = Name.identifier(name),
            isVar = false,
            isConst = false,
            isLateinit = false,
            isDelegated = false,
            visibility = DescriptorVisibilities.PUBLIC,
            modality = Modality.FINAL,
            symbol = IrPropertySymbolImpl()
        ).apply {
            parent = this@addStateFlowProperty // Set the parent to the IR class
        }
        // 2. Create the IR Backing Field for the property.
        property.backingField = factory.createField(
            startOffset = this.startOffset,
            endOffset = this.endOffset,
            origin = IrDeclarationOriginImpl("DataFlow"),
            name = Name.identifier(name),
            visibility = DescriptorVisibilities.PRIVATE,
            type = mutableStateFlowType,
            isFinal = true,
            isStatic = false,
            symbol = IrFieldSymbolImpl()
        ).apply {
            parent = this@addStateFlowProperty
            correspondingPropertySymbol = property.symbol
        }

        // 3. Create the IR Getter for the property.
        property.getter = factory.createSimpleFunction(
            startOffset = this.startOffset,
            endOffset = this.endOffset,
            origin = IrDeclarationOriginImpl("DataFlow"),
            name = Name.identifier("get$name"),
            visibility = DescriptorVisibilities.PUBLIC,
            modality = Modality.FINAL,
            returnType = mutableStateFlowType,
            isInline = false,
            isExternal = false,
            isTailrec = false,
            isSuspend = false,
            isExpect = false,
            isFakeOverride = false,
            symbol = IrSimpleFunctionSymbolImpl(),
            isInfix = false,
            isOperator = false
        ).apply {
            parent = this@addStateFlowProperty
            correspondingPropertySymbol = property.symbol
            dispatchReceiverParameter = this@addStateFlowProperty.thisReceiver!!.copyTo(
                irFunction = this,
                kind = IrParameterKind.DispatchReceiver
            )

            // The body of the getter simply returns the value from the backing field.
            body = IrBlockBodyBuilder(
                pluginContext,
                Scope(this.symbol),
                startOffset,
                endOffset
            ).blockBody {
                +irReturn(
                    irGetField(
                        irGet(dispatchReceiverParameter!!),
                        property.backingField!!
                    )
                )
            }
        }

        this.addMember(property)

        return property
    }

    // Helper to add a value parameter to a function
    private fun IrSimpleFunction.addValueParameter(
        name: String,
        type: IrType
    ): IrValueParameter {
        return this.addValueParameter(Name.identifier(name), type)
    }
}