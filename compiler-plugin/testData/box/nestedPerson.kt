package io.fergdev.dataflow

//import kotlin.test.assertEquals
import io.fergdev.dataflow.annotations.DataFlow
import io.fergdev.dataflow.annotations.DataFlowIgnore

fun assertEquals(a: Any?, b: Any?) {
    if (a != b) throw IllegalStateException("a $a != b $b")
}

@DataFlow
data class Person(
    val i: Int = 0,
    val j: Int = 1,
    val friend: Person? = null
)

fun box(): String {

    val friend = Person(100, 200, null)
    val df = Person.DataFlow(10, 20, null)

    assertEquals(df.i.value, 10)
    assertEquals(df.j.value, 20)
    assertEquals(df.friend.value, null)
    assertEquals(df.all.value, Person(10, 20, null))

    df.updateAll(Person(-10, -20, friend))
    assertEquals(df.i.value, -10)
    assertEquals(df.j.value, -20)
    assertEquals(df.friend.value, friend)
    assertEquals(df.all.value, Person(-10, -20, friend))

    return "OK"
}
