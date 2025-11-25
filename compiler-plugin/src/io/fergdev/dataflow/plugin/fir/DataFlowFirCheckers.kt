package io.fergdev.dataflow.plugin.fir

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.constructors
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName

private fun println(string: String) {
    kotlin.io.println("CHK $string")
}

internal class DataFlowFirCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers =
        object : DeclarationCheckers() {
            override val classCheckers: Set<FirClassChecker>
                get() = setOf(FirDataFlowDeclarationChecker)
        }
}

// TODO add check - all properties are not ignore
internal object FirDataFlowDeclarationChecker : FirClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        println("check ${declaration.nameOrSpecialName}")
        val dataFlowClassAnnotations =
            context.session.annotations.mapNotNull { classId ->
                declaration.getAnnotationByClassId(classId, context.session)
                    ?.let { it to classId }
            }

        println("A ${declaration.nameOrSpecialName}")
        val dataFlowIgnoreAnnotation =
            declaration.constructors(context.session)
                .first().valueParameterSymbols.map { ctorParam ->
                    ctorParam.resolvedAnnotationClassIds.any { annotation ->
                        context.session.ignoreAnnotations.contains(annotation)
                    }
                }
        println("B ${declaration.nameOrSpecialName}")

        val classIsDataFlow = dataFlowClassAnnotations.isNotEmpty()
        val classHasIgnore = dataFlowIgnoreAnnotation.any { it }

        println("*** Report classIsDataFlow='$classIsDataFlow' classHasIgnore='${classHasIgnore}'")
        if (!classIsDataFlow && classHasIgnore) {
            println("Reporting")
            reporter.reportOn(
                declaration.source,
                DataFlowDiagnostics.DATAFLOW_ERROR,
                "Class is annotated with @DataFlowIgnore but is not annotated with @DataFlow"
            )
        }
        if (dataFlowIgnoreAnnotation.all { it }) {
            reporter.reportOn(
                declaration.source,
                DataFlowDiagnostics.DATAFLOW_ERROR,
                "@Dataflow class has all properties annotated with @DataFlowIgnore"
            )
        }
    }
}
