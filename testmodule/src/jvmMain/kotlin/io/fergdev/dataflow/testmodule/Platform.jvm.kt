package io.fergdev.dataflow.testmodule

import io.fergdev.dataflow.annotations.DataFlow
import io.fergdev.dataflow.annotations.DataFlowIgnore

actual fun platform(): String = "Jvm"

@DataFlow
data class Tea(
    val i: Int = 0,
    val j: Int = 0,
    @DataFlowIgnore val k: Int = 0
)

fun B() {
//    Wow(i = 1, j = 1)
//    WowFlow(i = 1, j = 1)
//    WowDataFlow(i = 1, j = 1)
}
