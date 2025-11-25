package io.fergdev.dataflow.example

import io.fergdev.dataflow.annotations.DataFlow

actual fun platform(): String = "Web"

@DataFlow
data class WebTestClass(
    val i: Int,
    val j: String,
    val k: Boolean
)

private fun Test() {
    val df = WebTestClass.DataFlow(i = 0, j = "hi", k = false)
}