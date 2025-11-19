package org.jetbrains.kotlin.compiler.plugin.template.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.name.ClassId

internal class DataFlowFirBuiltins(
    session: FirSession,
    val annotations: Set<ClassId>,
    val ignoreAnnotations: Set<ClassId>
) : FirExtensionSessionComponent(session) {

    companion object {
        fun getFactory(
            annotations: Set<ClassId>,
            ignoreAnnotations: Set<ClassId>
        ) = Factory { session ->
            DataFlowFirBuiltins(session, annotations, ignoreAnnotations)
        }
    }
}

internal val FirSession.dataFlowFirBuiltins: DataFlowFirBuiltins by FirSession.sessionComponentAccessor()

internal val FirSession.annotations: Set<ClassId>
    get() = dataFlowFirBuiltins.annotations

internal val FirSession.ignoreAnnotations: Set<ClassId>
    get() = dataFlowFirBuiltins.ignoreAnnotations
