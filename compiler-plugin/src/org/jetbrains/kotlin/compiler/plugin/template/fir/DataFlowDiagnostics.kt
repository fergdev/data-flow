package org.jetbrains.kotlin.compiler.plugin.template.fir

import org.jetbrains.kotlin.compiler.plugin.template.fir.DataFlowDiagnostics.DATAFLOW_ERROR
import org.jetbrains.kotlin.diagnostics.AbstractSourceElementPositioningStrategy
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1DelegateProvider
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies.NAME_IDENTIFIER
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers.STRING


/**
 * The compiler and the IDE use a different version of this class, so use reflection to find the
 * available version.
 */
// Adapted from
// https://github.com/TadeasKriz/K2PluginBase/blob/main/kotlin-plugin/src/main/kotlin/com/tadeaskriz/example/ExamplePluginErrors.kt#L8
private val psiElementClass by lazy {
    try {
        Class.forName("org.jetbrains.kotlin.com.intellij.psi.PsiElement")
    } catch (_: ClassNotFoundException) {
        Class.forName("com.intellij.psi.PsiElement")
    }.kotlin
}

/** Copy of [org.jetbrains.kotlin.diagnostics.error0] with hack for correct `PsiElement` class. */
context(container: KtDiagnosticsContainer)
private fun <A> error1(
    positioningStrategy: AbstractSourceElementPositioningStrategy =
        SourceElementPositioningStrategies.DEFAULT
): DiagnosticFactory1DelegateProvider<A> {
    return DiagnosticFactory1DelegateProvider<A>(
        severity = Severity.ERROR,
        positioningStrategy = positioningStrategy,
        psiType = psiElementClass,
        container = container,
    )
}

internal object DataFlowDiagnostics : KtDiagnosticsContainer() {
    val DATAFLOW_ERROR by error1<String>(NAME_IDENTIFIER)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory {
        return DataFlowErrorMessages
    }
}

internal object DataFlowErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP by
    KtDiagnosticFactoryToRendererMap("DataFlow") { map -> map.put(DATAFLOW_ERROR, "{0}", STRING) }
}