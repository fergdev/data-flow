package io.fergdev.dataflow

//import kotlin.test.assertEquals
import io.fergdev.dataflow.annotations.DataFlow
import io.fergdev.dataflow.annotations.DataFlowIgnore

@DataFlow
data class Person(
    val i: Int = 0,
    val j: Int = 1,
    @DataFlowIgnore
    val k: Int = 1,
)

fun assertEquals(a: Any?, b: Any?) {
    if (a != b) throw IllegalStateException("a $a != b $b")
}

fun box(): String {
    val flow = Person.DataFlow(i = 1, j = 2, k = 3)

    // Assert init
    assertEquals(flow.i.value, 1)
    assertEquals(flow.j.value, 2)
    assertEquals(flow.all.value, Person(i = 1, j = 2, k = 3))

    // Assert update i
    flow.updateI(i = -1)
    assertEquals(flow.i.value, -1)
    assertEquals(flow.j.value, 2)
    assertEquals(flow.all.value, Person(i = -1, j = 2, k = 3))

    // Assert update j
    flow.updateJ(j = -2)
    assertEquals(flow.i.value, -1)
    assertEquals(flow.j.value, -2)
    assertEquals(flow.all.value, Person(i = -1, j = -2, k = 3))

    val person = Person(i = -10, j = -20, k = -30)
    flow.updateAll(person)
    assertEquals(flow.i.value, person.i)
    assertEquals(flow.j.value, person.j)
    assertEquals(flow.all.value, person)

    return "OK"
}
