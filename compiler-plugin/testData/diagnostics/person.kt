// RUN_PIPELINE_TILL: FRONTEND

// MODULE: lib
// FILE: foo.kt
package foo
import io.fergdev.dataflow.annotations.DataFlow
import io.fergdev.dataflow.annotations.DataFlowIgnore

@DataFlow
data class Person(
    val i : Int = 0,
    val j : Int = 0,
    @DataFlowIgnore
    val k : Int = 0,
)

fun takeInt(x: Int) {}

fun test() {
    takeInt(10)
}

// MODULE: main(lib)
// FILE: bar.kt
package bar

import foo.takeInt
import io.fergdev.dataflow.annotations.DataFlow
import io.fergdev.dataflow.annotations.DataFlowIgnore

data class Wow(
    val i : Int = 0,
    val j : Int = 0,
    @DataFlowIgnore
    val k : Int = 0,
)

fun test() {
    takeInt(10)
    takeInt(<!ARGUMENT_TYPE_MISMATCH!>"Hello"<!>)
}
