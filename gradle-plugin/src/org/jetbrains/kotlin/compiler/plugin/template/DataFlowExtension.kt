package org.jetbrains.kotlin.compiler.plugin.template

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty

internal const val DEFAULT_ANNOTATION = "io/fergdev/dataflow/annotations/DataFlow"
internal val DEFAULT_ANNOTATION_SET = setOf(DEFAULT_ANNOTATION)
internal const val DEFAULT_IGNORE_ANNOTATION = "io/fergdev/dataflow/annotations/DataFlowIgnore"
internal val DEFAULT_IGNORE_ANNOTATION_SET = setOf(DEFAULT_IGNORE_ANNOTATION)

open class DataFlowExtension(objects: ObjectFactory) {

    val annotations: SetProperty<String> =
        objects.setProperty(String::class.java).convention(setOf(DEFAULT_ANNOTATION))

    val ignoreAnnotations: SetProperty<String> =
        objects.setProperty(String::class.java).convention(setOf(DEFAULT_IGNORE_ANNOTATION))
}
