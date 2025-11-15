package io.fergdev.dataflow.annotations

/**
 * An annotation that triggers the generation of a DataFlow wrapper class
 * for the annotated data class.
 */
@Target(AnnotationTarget.CLASS) // Important: This can only be applied to classes
@Retention(AnnotationRetention.SOURCE) // The annotation isn't needed at runtime
public annotation class DataFlow
