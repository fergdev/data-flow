package io.fergdev.dataflow.annotations

/**
 * An annotation that triggers the generation of a DataFlow wrapper class
 * for the annotated data class.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class DataFlow

/**
 * An annotation that triggers the generation of a DataFlow wrapper class
 * for the annotated data class.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
public annotation class DataFlowIgnore
