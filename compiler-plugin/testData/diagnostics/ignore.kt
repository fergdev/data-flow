// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import io.fergdev.dataflow.annotations.DataFlow
import io.fergdev.dataflow.annotations.DataFlowIgnore

@DataFlow
data class Person(
    val i: Int = 0,
    val j: Int = 1,
    @DataFlowIgnore
    val k: Int = 1,
)

fun test() {
    val s = Person.DataFlow(i = 0, j = 0, k = 0)
    s.<!UNRESOLVED_REFERENCE!>k<!> // should be an error
    s.<!UNRESOLVED_REFERENCE!>updateK<!>() // should be an error
}
