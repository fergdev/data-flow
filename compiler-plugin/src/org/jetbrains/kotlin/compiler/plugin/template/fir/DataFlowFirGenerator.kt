package org.jetbrains.kotlin.compiler.plugin.template.fir

import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.kotlin.compiler.plugin.template.DataFlowClassKey
import org.jetbrains.kotlin.compiler.plugin.template.DataFlowFunctionKey
import org.jetbrains.kotlin.compiler.plugin.template.DataFlowKey
import org.jetbrains.kotlin.compiler.plugin.template.DataFlowNames
import org.jetbrains.kotlin.compiler.plugin.template.DataFlowPropertyKey
import org.jetbrains.kotlin.compiler.plugin.template.titleCase
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.toTypeProjection
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance

data class A(val i: Int = 9) {
    class DataFlow(i: Int = 9) {
        val all = MutableStateFlow(A(i))
        val i: MutableStateFlow<Int> = MutableStateFlow(i)
        fun setI(i: Int) {
            this.all.value = all.value.copy(i = i)
            this.i.value = i
        }
    }
}


private fun println(string: String) {
    kotlin.io.println("FIR $string")
}

class DataFlowFirDeclarationGenerationExtension(session: FirSession) :
    FirDeclarationGenerationExtension(session) {
    companion object {
        val DataFlowPredicate = LookupPredicate.create {
            annotated(DataFlowNames.Annotation.DataFlow)
        }
        private val DATA_FLOW_PREDICATE = DeclarationPredicate.create {
            annotated(DataFlowNames.Annotation.DataFlow)
        }

        private val HAS_DATA_FLOW_PREDICATE = DeclarationPredicate.create {
            hasAnnotated(DataFlowNames.Annotation.DataFlow)
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
        println("A")
        val scope: FirScope = owner.declaredMemberScope(
            session,
            memberRequiredPhase = null
        )
        println("B")
        val provider = session.predicateBasedProvider
        println("C")
//         scope.getDeclaredConstructors()
//            .forEach { println("Constructor ${it}") }
//
//        val constructorSymbol = scope.getDeclaredConstructors()
//            .singleOrNull { provider.matches(DATA_FLOW_PREDICATE, it) } ?: return null
        val constructorSymbol = scope.getDeclaredConstructors().first()
//            .singleOrNull { provider.matches(DATA_FLOW_PREDICATE, it) } ?: return null

        println("D")
        val dataFlowClass = createNestedClass(
            owner = owner,
            name = DataFlowNames.Names.DataFlow,
            key = DataFlowClassKey(owner, constructorSymbol)
        ) {
            visibility = constructorSymbol.visibility.takeIf { it != Visibilities.Unknown }
                ?: owner.visibility
        }
        println("E")
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
                .filter { !it.hasAnnotation(DataFlowNames.Annotation.CIgnore, session) }
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
            add(DataFlowNames.Names.AllFunName)
            key.constructorSymbol.valueParameterSymbols.filter {
                !it.hasAnnotation(DataFlowNames.Annotation.CIgnore, session)
            }.forEach { param ->
                add(param.name)
            }
        }
    }

    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        println("generateProperties '${callableId.asFqNameForDebugInfo()}' - context $context")
        val key = (context?.owner?.origin as? FirDeclarationOrigin.Plugin)?.key
        if (key !is DataFlowClassKey) return emptyList()
        val matchedClassSymbol = key.ownerClassSymbol

        if (callableId.callableName == DataFlowNames.Names.AllFunName) {
            return listOf(
                createMemberProperty(
                    matchedClassSymbol,
                    DataFlowKey,
                    callableId.callableName,
                    {
                        DataFlowNames.mutableStateFlow.createConeType(
                            session,
                            typeArguments = arrayOf(
                                matchedClassSymbol.classId
                                    .createConeType(session)
                                    .toTypeProjection(Variance.INVARIANT)
                            )
                        )
                    }
                ).symbol)
        }
        val parameterSymbol = key.constructorSymbol.valueParameterSymbols
            .singleOrNull { it.name == callableId.callableName }
            ?: return emptyList()

        return listOf(
            this@DataFlowFirDeclarationGenerationExtension.createMemberProperty(
                matchedClassSymbol,
                DataFlowPropertyKey(
                    ownerClassSymbol = key.ownerClassSymbol,
                    constructorSymbol = key.constructorSymbol,
                    parameterSymbol = parameterSymbol
                ),
                callableId.callableName, {
                    DataFlowNames.mutableStateFlow.createConeType(
                        this@DataFlowFirDeclarationGenerationExtension.session,
                        typeArguments = arrayOf(parameterSymbol.resolvedReturnType)
                    )
                }
            ).symbol)
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> {
        println("generateFunctions ${callableId.asFqNameForDebugInfo()}")
        val key = (context?.owner?.origin as? FirDeclarationOrigin.Plugin)?.key
        if (key !is DataFlowClassKey) return emptyList()
        val matchedClassSymbol = key.ownerClassSymbol
        println("*** generating function ${callableId.asFqNameForDebugInfo()}")
        val parameterSymbol = key.constructorSymbol.valueParameterSymbols
            .singleOrNull { it.name == callableId.callableName } ?: return emptyList()

        return listOf(
            this.createMemberFunction(
                matchedClassSymbol,
                DataFlowFunctionKey(
                    ownerClassSymbol = key.ownerClassSymbol,
                    constructorSymbol = key.constructorSymbol,
                    parameterSymbol = parameterSymbol
                ),

                Name.identifier("set${callableId.callableName.identifier.titleCase()}"), {
                    DataFlowNames.unit.createConeType(session)
                }
            ).symbol
        )
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
