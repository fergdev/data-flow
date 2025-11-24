// RUN_PIPELINE_TILL: FRONTEND

import io.fergdev.dataflow.annotations.DataFlow
import io.fergdev.dataflow.annotations.DataFlowIgnore

data class <!DATAFLOW_ERROR!>Person<!>(
    val i : Int = 0,
    val j : Int = 0,
    @DataFlowIgnore
    val k : Int = 0,
)

fun test() {}
