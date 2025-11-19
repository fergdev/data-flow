package org.jetbrains.kotlin.compiler.plugin.template

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


object DataFlowNames {
    object Annotation {
        //        ClassId(FqName.ROOT, "DataFlow")
        val annotationPackage = FqName("io.fergdev.dataflow.annotations")
        val DataFlow = FqName("io.fergdev.dataflow.annotations.DataFlow")
        val Ignore = FqName("io.fergdev.dataflow.annotations.DataFlowIgnore")
        val CIgnore = ClassId(annotationPackage, Name.identifier("DataFlowIgnore"))
        val CDataFlow = ClassId(annotationPackage, Name.identifier("DataFlow"))
    }

    object Names {
        val DataFlow = Name.identifier("DataFlow")
        val AllFunName = Name.identifier("all")
    }

    val unit = ClassId(FqName("kotlin"), Name.identifier("Unit"))
    val mutableStateFlow =
        ClassId(FqName("kotlinx.coroutines.flow"), Name.identifier("MutableStateFlow"))
    val stateFlow = ClassId(FqName("kotlinx.coroutines.flow"), Name.identifier("StateFlow"))
}