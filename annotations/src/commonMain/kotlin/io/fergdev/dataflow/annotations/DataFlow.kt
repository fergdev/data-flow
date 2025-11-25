package io.fergdev.dataflow.annotations

/**
 * An annotation to indicate that a particular class should have a DataFlow nested class.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class DataFlow

/**
 * An annotation to indicate that a particular parameter should be ignored by the DataFlow compiler.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
public annotation class DataFlowIgnore
