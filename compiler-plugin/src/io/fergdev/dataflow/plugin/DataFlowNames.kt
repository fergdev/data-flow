package io.fergdev.dataflow.plugin

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


object DataFlowNames {
    object Annotation {
        val annotationPackage = FqName("io.fergdev.dataflow.annotations")
        val DataFlow = FqName("io.fergdev.dataflow.annotations.DataFlow")
        val CDataFlow = ClassId(
            packageFqName = annotationPackage,
            topLevelName = Name.identifier("DataFlow")
        )
        val CIgnore = ClassId(
            packageFqName = annotationPackage,
            topLevelName = Name.identifier("DataFlowIgnore")
        )
    }

    object Names {
        val DataFlow = Name.identifier("DataFlow")
        val AllPropName = Name.identifier("all")
        val AllFunName = Name.identifier("updateAll")

        private const val PREFIX = "update"
        fun Name.toUpdateName() = Name.identifier("${PREFIX}${this.identifier.titleCase()}")
        fun Name.isUpdateName(): Boolean {
            return this.identifier.startsWith(PREFIX)
        }

        fun Name.updateToParamName() =
            Name.identifier(
                this.identifier.removePrefix(PREFIX).replaceFirstChar { it.lowercase() })
    }

    object Callable {
        val mutableStateFlow =
            CallableId(
                packageName = FqName("kotlinx.coroutines.flow"),
                callableName = Name.identifier("MutableStateFlow")
            )

        val mutableStateFlowValue = CallableId(
            packageName = FqName("kotlinx.coroutines.flow"),
            className = FqName("MutableStateFlow"),
            callableName = Name.identifier("value")
        )
    }

    object Class {
        val mutableStateFlow =
            ClassId(
                packageFqName = FqName("kotlinx.coroutines.flow"),
                topLevelName = Name.identifier("MutableStateFlow")
            )
        val stateFlow =
            ClassId(
                packageFqName = FqName("kotlinx.coroutines.flow"),
                topLevelName = Name.identifier("StateFlow")
            )
    }
}