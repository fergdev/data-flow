// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import io.fergdev.dataflow.annotations.DataFlow
import io.fergdev.dataflow.annotations.DataFlowIgnore

@DataFlow
data class <!DATAFLOW_ERROR!>Person<!>(
    @DataFlowIgnore
    val i: Int = 0,
    @DataFlowIgnore
    val j: Int = 1,
    @DataFlowIgnore
    val k: Int = 1,
)

fun test() {
}
