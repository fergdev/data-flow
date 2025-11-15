package org.jetbrains.kotlin.compiler.plugin.template

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

// The @OptIn annotation is required because the compiler plugin API is experimental.
@OptIn(ExperimentalCompilerApi::class)
class DataFlowComponentRegistrar : CompilerPluginRegistrar() {

    /**
     * This is the main entry point of the plugin. The compiler calls this method
     * during its initialization.
     *
     * @param configuration The current compiler configuration. You can use this to
     * pass options to your plugin.
     */
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // Register our custom IR generation extension.
        // This extension is responsible for finding the @DataFlow annotation and
        // generating the new wrapper class.
        IrGenerationExtension.registerExtension(
            DataFlowIrGenerationExtension()
        )
    }

    /**
     * Indicates whether this plugin supports K2 (the new Kotlin compiler).
     * It's good practice to set this to true as K2 is the future of Kotlin.
     */
    override val supportsK2: Boolean = true
}

/**
 * This class will contain the core logic for your plugin. It will traverse the
 * Intermediate Representation (IR) of the source code, find classes annotated
 * with @DataFlow, and generate the corresponding '...Flow' wrapper classes.
 */
class DataFlowIrGenerationExtension : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        // TODO: This is where the magic happens.
        // 1. Create a visitor that will traverse the IR tree.
        // 2. In the visitor, look for classes annotated with @DataFlow.
        // 3. For each annotated data class, use the IrFactory and SymbolTable
        //    from the pluginContext to construct a new IrClass (the '...Flow' wrapper).
        // 4. Add the generated class to the moduleFragment.

        println("DataFlow compiler plugin is running!")
        val visitor = DataFlowClassVisitor(pluginContext)
        moduleFragment.transform(visitor, null)
    }
}

private class DataFlowClassVisitor(
    private val pluginContext: IrPluginContext
) :
    IrElementTransformerVoid() {
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

    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration.annotations.any { it.type.classFqName == dataFlowAnnotation }) {
            val generatedClass = generateDataFlowClass(declaration)
            generatedClasses.add(generatedClass)
        }
        return super.visitClass(declaration)
    }

    override fun visitFile(declaration: IrFile): IrFile {
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
        // 1. Create the new class itself
        val factory = org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
        val generatedClassName = Name.identifier("${annotatedClass.name.asString()}Flow")
        val generatedClass = factory.createClass(
            startOffset = annotatedClass.startOffset,
            endOffset = annotatedClass.endOffset,
            origin = IrDeclarationOriginImpl(generatedClassName.identifier),
            name = generatedClassName,
            visibility = DescriptorVisibilities.PUBLIC,
            modality = Modality.FINAL,
            kind = org.jetbrains.kotlin.descriptors.ClassKind.CLASS,
            isInner = false,
            isCompanion = false,
            isData = false,
            isExternal = false,
            isFun = false,
            isExpect = false,
            isValue = false,
            symbol = IrClassSymbolImpl()
        ).apply {
            // Set the parent of the generated class to be the same file as the annotated class.
            parent = annotatedClass.parent
            thisReceiver = factory.createValueParameter(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = IrDeclarationOrigin.INSTANCE_RECEIVER,
                kind = IrParameterKind.DispatchReceiver,
                name = Name.special("<this>"),
                type = this.symbol.typeWith(), // The type is the class itself
                isAssignable = false,
                symbol = IrValueParameterSymbolImpl(),
                varargElementType = null,
                isCrossinline = false,
                isNoinline = false,
                isHidden = false,
            ).also { it.parent = this }
        }
        // 2. Find references to external types we need (like MutableStateFlow)
        val mutableStateFlowOf =
            pluginContext.referenceFunctions(msfCId)
                .single { it.owner.parameters.size == 1 }
        println("wowowow $mutableStateFlowOf")
        println("asdfasdf $msfCId2")
        val mutableStateFlowValue =
            pluginContext.referenceProperties(msfCId2)
                .single {
                    println(it.owner.name)
                    it.owner.name.asString() == "value"
                }.owner.setter!!
        val stateFlowValue =
            pluginContext.referenceProperties(sfCId)
                .single { it.owner.name.asString() == "value" }.owner.getter!!

        // 3. Create the 'all' property: val all = MutableStateFlow(MyData())
        val allProperty = generatedClass.addStateFlowProperty("all", annotatedClass.defaultType)

        // 4. Generate properties and setters for each property in the original data class
        annotatedClass.primaryConstructor?.parameters?.forEach { param ->
            val propertyName = param.name.asString()
            val propertyType = param.type

            // Create the state flow property: val i = MutableStateFlow(...)
            val flowProperty = generatedClass.addStateFlowProperty(propertyName, propertyType)

            // Create the setter method: fun setI(i: Int)
            generatedClass.addFunction {
                name = Name.identifier("set${propertyName.replaceFirstChar { it.uppercase() }}")
                returnType = pluginContext.irBuiltIns.unitType
            }.apply {
                val dispatchReceiver = generatedClass.thisReceiver!!.copyTo(
                    irFunction = this@apply,
                    kind = IrParameterKind.DispatchReceiver
                )
                this.parameters = listOf(dispatchReceiver)

                val parameter = addValueParameter(propertyName, propertyType)
                body = IrBlockBodyBuilder(
                    context = pluginContext,
                    scope = Scope(this.symbol),
                    startOffset = startOffset,
                    endOffset = endOffset
                ).blockBody {
                    val copyFunction = annotatedClass.functions.single { it.name.asString() == "copy" }
                    val copyFunctionParameter = copyFunction.parameters.single { it.name == parameter.name }
                    val getThis = irGet(this@apply.dispatchReceiverParameter!!)
                    val getAllProperty = irGetField(getThis, allProperty.backingField!!)
                    val getCurrentAllValue = irCall(stateFlowValue).apply {
                        this.dispatchReceiver = getAllProperty
                    }
                    val newAllValue = irCall(copyFunction).apply {
                        this.dispatchReceiver = getCurrentAllValue
                        arguments[copyFunctionParameter.indexInParameters] = irGet(parameter)
                    }

                    +irCall(mutableStateFlowValue).apply {
                        this.dispatchReceiver = getAllProperty
                        arguments[0] = newAllValue
                    }
                }
            }
        }

        return generatedClass
    }
    private fun IrClass.addStateFlowProperty(
        name: String,
        type: org.jetbrains.kotlin.ir.types.IrType
    ): org.jetbrains.kotlin.ir.declarations.IrProperty {
        val factory = pluginContext.irFactory
        // Get a reference to the MutableStateFlow<T> type.
        val mutableStateFlowType = pluginContext.referenceClass(classIdMutableStateFlow)!!
            .typeWith(type)

        // 1. Create the IR Property.
//        val property = org.jetbrains.kotlin.ir.builders.declarations.buildProperty(
        val property = factory.createProperty(
            startOffset = this.startOffset,
            endOffset = this.endOffset,
            origin = IrDeclarationOriginImpl("DataFlow"), // An IR Origin
            name = Name.identifier(name),
//            type = mutableStateFlowType,
            isVar = false,
            isConst = false,
            isLateinit = false,
            isDelegated = false,
            visibility = DescriptorVisibilities.PUBLIC,
            modality = Modality.FINAL,
            symbol = IrPropertySymbolImpl(),

        ).apply {
            parent = this@addStateFlowProperty // Set the parent to the IR class
        }
        // 2. Create the IR Backing Field for the property.
        property.backingField = factory.createField(
            startOffset = this.startOffset,
            endOffset = this.endOffset,
            origin = IrDeclarationOriginImpl("DataFlow"),
            name = Name.identifier(name),
            visibility = org.jetbrains.kotlin.descriptors.DescriptorVisibilities.PRIVATE,
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
            val dispatchReceiver = this@addStateFlowProperty.thisReceiver!!.copyTo(
                irFunction = this,
                kind = IrParameterKind.DispatchReceiver
            )
            this.parameters = listOf(dispatchReceiver)
//            parameters = listOf(
//                buildReceiverParameter {
//                    this.type = this@addStateFlowProperty.thisReceiver!!.type
//                }
//            )

            // The body of the getter simply returns the value from the backing field.
            body = IrBlockBodyBuilder(pluginContext, Scope(this.symbol), startOffset, endOffset).blockBody {
                +irReturn(
                    irGetField(
                        irGet(dispatchReceiver),
                        property.backingField!!
                    )
                )
            }
        }

        // 4. Add the fully constructed IR property to the IR class.
        this.addMember(property)

        return property
    }

    // Helper to add a value parameter to a function
    private fun IrSimpleFunction.addValueParameter(
        name: String,
        type: org.jetbrains.kotlin.ir.types.IrType
    ): org.jetbrains.kotlin.ir.declarations.IrValueParameter {
        return this.addValueParameter(Name.identifier(name), type)
    }
}
