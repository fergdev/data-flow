package org.jetbrains.kotlin.compiler.plugin.template.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.name.FqName

class DataFlowFirDeclarationGenerationExtension(
    session: FirSession
) : FirDeclarationGenerationExtension(session) {

    object Key : GeneratedDeclarationKey()

    private val dataFlowAnnotation = FqName("io.fergdev.dataflow.annotations.DataFlow")

//    override val key: FirPluginKey get() = Companion


//    override fun getAnnotatedClassLikeDeclarationGenerators(): List<AnnotationsPredicate> {
//        return listOf(
//            lookupBased { annotated(dataFlowAnnotation) }
//        )
//    }
//
//    override fun generateClassLikeDeclaration(
//        context: GeneratorContext,
//        owner: FirClassLikeSymbol<*>
//    ): FirClassLikeSymbol<*>? {
//        val original = owner.fir
//        if (original !is FirRegularClass) return null
//        if (!original.isData) return null  // keep it minimal & safe
//
//        val originalClassId = original.symbol.classId
//        val flowName = Name.identifier(originalClassId.shortClassName.asString() + "Flow")
//
//        val flowClassId = ClassId(
//            originalClassId.packageFqName,
//            flowName
//        )
//
//        val symbol = FirRegularClassSymbol(flowClassId)
//
//        val flowClass = buildRegularClass {
//            this.moduleData = original.moduleData
//            this.origin = FirDeclarationOrigin.Plugin
//            this.source = null
//            this.classId = flowClassId
//            this.symbol = symbol
//            this.resolvePhase = original.resolvePhase
//            this.name = flowName
//            this.status = original.status.copy(isCompanion = false, isData = false)
//            this.annotations.addAll(emptyList())
//            this.typeParameters.addAll(emptyList())
//            this.superTypeRefs.add(
//                // just inherit from Any; IR will adjust/extend if needed
//                buildResolvedTypeRef {
//                    coneType = session.builtinTypes.anyType.coneType
//                }
//            )
//        }
//
//        // Register the class with the plugin context
//        context.generatedDeclarations += flowClass
//
//        return symbol
//    }
}