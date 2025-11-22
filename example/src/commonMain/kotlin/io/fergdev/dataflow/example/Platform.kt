package io.fergdev.dataflow.example

import io.fergdev.dataflow.annotations.DataFlow
import io.fergdev.dataflow.annotations.DataFlowIgnore
import kotlinx.coroutines.flow.MutableStateFlow

expect fun platform(): String

@DataFlow
data class Wow(
    val i: Int = 0,
    val j: Int = 0,
    @DataFlowIgnore val k: Int = 0
)

data class Powow(

    @DataFlowIgnore
    val i: Int = 0,
)

fun A() {
//    val df = Wow.DataFlow(i = 1, j = 2, k = 3)
    val a = MutableStateFlow("a")
    a.value = "a"
//    Wow.DataFlow()
//    WowFlow(i = 1, j = 1)
//    WowDataFlow(i = 1, j = 1)
}