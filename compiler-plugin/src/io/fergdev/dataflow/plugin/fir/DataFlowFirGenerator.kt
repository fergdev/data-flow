package io.fergdev.dataflow.plugin.fir

import io.fergdev.dataflow.plugin.DataFlowClassKey
import io.fergdev.dataflow.plugin.DataFlowFunctionKey
import io.fergdev.dataflow.plugin.DataFlowKey
import io.fergdev.dataflow.plugin.DataFlowNames
import io.fergdev.dataflow.plugin.DataFlowNames.Names.isUpdateName
import io.fergdev.dataflow.plugin.DataFlowNames.Names.toUpdateName
import io.fergdev.dataflow.plugin.DataFlowNames.Names.updateToParamName
import io.fergdev.dataflow.plugin.DataFlowPropertyKey
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.toTypeProjection
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance

private fun println(string: String) {
    kotlin.io.println("FIR $string")
}

class DataFlowFirDeclarationGenerationExtension(session: FirSession) :
    FirDeclarationGenerationExtension(session) {
    companion object {
        val DataFlowPredicate = LookupPredicate.create {
            annotated(DataFlowNames.Annotation.DataFlow)
        }
    }

    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(DataFlowPredicate)
            .filterIsInstance<FirRegularClassSymbol>()
    }

    @ExperimentalTopLevelDeclarationsGenerationApi
    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        println("generateTopLevelClassLikeDeclaration $classId")
        return super.generateTopLevelClassLikeDeclaration(classId)
    }

    @ExperimentalTopLevelDeclarationsGenerationApi
    override fun getTopLevelCallableIds(): Set<CallableId> {
        val res = super.getTopLevelCallableIds()
        println("getTopLevelCallableIds '$res'")
        return res
    }

    @ExperimentalTopLevelDeclarationsGenerationApi
    override fun getTopLevelClassIds(): Set<ClassId> {
        println("getTopLevelClassIds")
        return super.getTopLevelClassIds()
    }

    override fun hasPackage(packageFqName: FqName): Boolean {
        println("hasPackage $packageFqName")
        return super.hasPackage(packageFqName)
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? {
        println("generateNestedClassLikeDeclaration ${owner.packageFqName()}.${owner.name} - $name")
        if (name != DataFlowNames.Names.DataFlow) return null
        val scope: FirScope = owner.declaredMemberScope(
            session,
            memberRequiredPhase = null
        )
        val constructorSymbol = scope.getDeclaredConstructors().first()
        val dataFlowClass = createNestedClass(
            owner = owner,
            name = DataFlowNames.Names.DataFlow,
            key = DataFlowClassKey(ownerClassSymbol = owner, constructorSymbol = constructorSymbol)
        ) {
            visibility = constructorSymbol.visibility.takeIf { it != Visibilities.Unknown }
                ?: owner.visibility
        }
        return dataFlowClass.symbol
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        println("generateConstructors ${context.owner.name}")
        val key = (context.owner.origin as? FirDeclarationOrigin.Plugin)?.key
        println("key generateConstructors $key")
        if (key !is DataFlowClassKey) return emptyList()
        println("proc generateConstructors ${context.owner.name}")

        return listOf(createConstructor(context.owner, DataFlowKey, isPrimary = true) {
            key.constructorSymbol.valueParameterSymbols
                .forEach { parameter ->
                    println("*** param $parameter")
                    valueParameter(
                        name = parameter.name,
                        type = parameter.resolvedReturnType,
                    )
                }
        }.symbol)
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext
    ): Set<Name> {
        println("getCallableNamesForClass - ${classSymbol.classId.asFqNameString()}")
        val key = (classSymbol.origin as? FirDeclarationOrigin.Plugin)?.key
        println("key getCallableNamesForClass - ${key}")
        if (key !is DataFlowClassKey) return emptySet()

        return buildSet {
            add(SpecialNames.INIT)
            add(DataFlowNames.Names.AllPropName)
            add(DataFlowNames.Names.AllFunName)
            key.constructorSymbol.valueParameterSymbols.filter {
                !it.hasAnnotation(DataFlowNames.Annotation.CIgnore, session)
            }.forEach { param ->
                add(param.name)
                add(param.name.toUpdateName())
            }
        }
    }

    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        val owner = context?.owner
        println("generateProperties '${callableId.asFqNameForDebugInfo()}' - context $owner")
        val key = (owner?.origin as? FirDeclarationOrigin.Plugin)?.key
        if (key !is DataFlowClassKey) return emptyList()
        if(callableId.callableName.isUpdateName()) {
            println("*** ignoring update fun")
            return emptyList()
        }
        val matchedClassSymbol = key.ownerClassSymbol
        println("*** matched ${matchedClassSymbol}")

        if (callableId.callableName == DataFlowNames.Names.AllPropName) {
            return listOf(
                createMemberProperty(
                    owner = owner,
                    key = DataFlowKey,
                    name = callableId.callableName,
                    returnTypeProvider = {
                        DataFlowNames.Class.stateFlow.createConeType(
                            session = session,
                            typeArguments = arrayOf(
                                matchedClassSymbol.classId
                                    .createConeType(session)
                                    .toTypeProjection(Variance.INVARIANT)
                            )
                        )
                    },
                ).symbol
            )
        }
        val parameterSymbol = key.constructorSymbol.valueParameterSymbols
            .singleOrNull { it.name == callableId.callableName }
            ?: return emptyList()

        return listOf(
            this.createMemberProperty(
                owner = owner,
                key = DataFlowPropertyKey(
                    ownerClassSymbol = key.ownerClassSymbol,
                    constructorSymbol = key.constructorSymbol,
                    parameterSymbol = parameterSymbol
                ),
                name = callableId.callableName,
                returnTypeProvider = {
                    DataFlowNames.Class.stateFlow.createConeType(
                        session = this.session,
                        typeArguments = arrayOf(parameterSymbol.resolvedReturnType)
                    )
                },
            ).symbol
        )
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> {
        val dataFlowOwner = context?.owner
        println("generateFunctions ${callableId.asFqNameForDebugInfo()} - $dataFlowOwner")
        val key = (dataFlowOwner?.origin as? FirDeclarationOrigin.Plugin)?.key
        if (key !is DataFlowClassKey) return emptyList()
        if (!callableId.callableName.isUpdateName()) {
            println("*** ignoring prop name")
            return emptyList()
        }
        val functionName = callableId.callableName
        val paramName = functionName.updateToParamName()

        println("*** generating function ${functionName.identifier}")
        val parameterSymbol =
            if(functionName == DataFlowNames.Names.AllFunName) {
                val ownerClassType = key.ownerClassSymbol.defaultType()
                buildValueParameter {
                    // This is boilerplate for synthetic elements
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.Plugin(DataFlowKey)

                    // The containing function symbol isn't critical here, but for correctness,
                    // we can point to a non-existent symbol or leave it. The key parts are name and type.
                    containingDeclarationSymbol = FirNamedFunctionSymbol(callableId)

                    // The name of the parameter and its type
                    name = DataFlowNames.Names.AllFunName.updateToParamName() // "all"
                    returnTypeRef = buildResolvedTypeRef {
                        coneType = ownerClassType
                    }
                    symbol = FirValueParameterSymbol()
                    isVararg = false
                }.symbol
            } else {
                key.constructorSymbol.valueParameterSymbols
                    .singleOrNull { it.name == paramName } ?: return emptyList()
            }

        println("*** param $parameterSymbol ${parameterSymbol.resolvedReturnType}")

        val createMemberFunction = this.createMemberFunction(
            owner = dataFlowOwner,
            key = DataFlowFunctionKey(
                ownerClassSymbol = dataFlowOwner,
                constructorSymbol = key.constructorSymbol,
                parameterSymbol = parameterSymbol
            ),
            name = functionName,
            returnType = this.session.builtinTypes.unitType.coneType,
            config = {
                valueParameter(
                    name = paramName,
                    type = parameterSymbol.resolvedReturnType
                )
            }
        )

        println(createMemberFunction.symbol)
        println(createMemberFunction.dispatchReceiverType)
        println("params " + createMemberFunction.valueParameters.joinToString { it.name.identifier + " " + it.symbol })

        return listOf(createMemberFunction.symbol)
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext
    ): Set<Name> {
        println("getNestedClassifiersNames ${classSymbol.classId.asFqNameString()}")
        return if (classSymbol in matchedClasses) {
            println("*** matches")
            setOf(DataFlowNames.Names.DataFlow)
        } else {
            emptySet()
        }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        println("registerPredicates")
        register(DataFlowPredicate)
    }
}
