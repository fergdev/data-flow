package io.fergdev.dataflow

import io.fergdev.dataflow.annotations.DataFlow
import io.fergdev.dataflow.annotations.DataFlowIgnore

fun box() {
    @DataFlow
    data class Wow(
        val i: Int = 0,
        val j: Int = 1,
        @DataFlowIgnore
        val k: Int = 1,
    )

//    val flow = WowDataFlow(i = 1, j = 2)
}