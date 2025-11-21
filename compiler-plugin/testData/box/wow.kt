// FIR_DUMP
// DUMP_IR

package io.fergdev.dataflow

import io.fergdev.dataflow.annotations.DataFlow
import io.fergdev.dataflow.annotations.DataFlowIgnore

fun box() {
    val df = Wow.DataFlow(i = 1, j = 2, k = 3)
//    df.setI(0)

    df.i.value = 0
    df.all.value = Wow(i = 0, j = 2, k = 3)
}

@DataFlow
data class Wow(
    val i: Int = 0,
    val j: Int = 1,
    @DataFlowIgnore
    val k: Int = 1,
)
