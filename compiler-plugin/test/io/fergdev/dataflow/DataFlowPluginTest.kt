package io.fergdev.dataflow

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.compiler.plugin.template.DataFlowComponentRegistrar
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.reflect.full.primaryConstructor
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.text.contains
import kotlin.text.split

class DataFlowPluginTest {

    @Test
    fun `DataFlow plugin generates wrapper class`() {
        val source = SourceFile.Companion.kotlin(
            "MyData.kt", """
            package com.example

            import io.fergdev.dataflow.annotations.DataFlow

            @DataFlow
            data class MyData(val i: Int, val j: Int)
            """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(source)
            compilerPluginRegistrars = listOf(DataFlowComponentRegistrar())
            workingDir = File("build/compile-testing-out") // Or any other path
            inheritClassPath = true
            messageOutputStream = System.out
            val coroutinesCoreJar = System.getProperty("java.class.path")
                .split(File.pathSeparator)
                .first { it.contains("kotlinx-coroutines-core") }
            classpaths = listOf(File(coroutinesCoreJar))
        }.compile()

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, "Compilation failed!")

        // 4. Use Reflection to verify the generated code
//        try {
        // Load the generated wrapper class
        val generatedClass = result.classLoader.loadClass("com.example.MyDataFlow")
        assertNotNull(generatedClass, "MyDataFlow class was not generated.")

        // Check for the 'all' property
        val allField = generatedClass.getDeclaredField("all")
        assertEquals(
            "kotlinx.coroutines.flow.MutableStateFlow",
            allField.type.name,
            "'all' field has wrong type."
        )

//            // Check for the 'i' property
        val iField = generatedClass.getDeclaredField("i")
        assertEquals(
            "kotlinx.coroutines.flow.MutableStateFlow",
            iField.type.name,
            "'i' field has wrong type."
        )

        val jField = generatedClass.getDeclaredField("j")
        assertEquals(
            "kotlinx.coroutines.flow.MutableStateFlow",
            jField.type.name,
            "'j' field has wrong type."
        )

//            // Check for the 'setI' method
//            // The parameter type for 'setI(Int)' is 'java.lang.Integer.TYPE'
        val setIMethod = generatedClass.getMethod("setI", Integer.TYPE)
        assertNotNull(setIMethod, "setI(Int) method was not found.")

        val setJMethod = generatedClass.getMethod("setJ", Integer.TYPE)
        assertNotNull(setIMethod, "setI(Int) method was not found.")

//        } catch (e: ClassNotFoundException) {
//            throw AssertionError(
//                "The expected class 'com.example.MyDataFlow' was not found in the compilation output.",
//                e
//            )
//        } catch (e: NoSuchFieldException) {
//            throw AssertionError("A required field was not found in the generated class.", e)
//        } catch (e: NoSuchMethodException) {
//            throw AssertionError("A required method was not found in the generated class.", e)
//        }
    }
    @Test
    fun `DataFlow plugin generates a usable wrapper class - test calls`() = runBlocking {
        val source = SourceFile.kotlin(
            "MyData.kt", """
            package com.example

            import io.fergdev.dataflow.annotations.DataFlow
            import kotlinx.coroutines.flow.StateFlow

            @DataFlow
            data class MyData(val i: Int, val j: Int)
            """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(source)
            compilerPluginRegistrars = listOf(DataFlowComponentRegistrar())
            workingDir = File("build/compile-testing-out")
            inheritClassPath = true
            messageOutputStream = System.out
            val coroutinesCoreJar = System.getProperty("java.class.path")
                .split(File.pathSeparator)
                .first { it.contains("kotlinx-coroutines-core") }
            classpaths = listOf(File(coroutinesCoreJar))
        }.compile()
        println("Finished compilation")

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, "Compilation failed!")

        // 1. Load the original and generated classes using Kotlin Reflection
        val originalClass = result.classLoader.loadClass("com.example.MyData").kotlin
        val generatedClazz = result.classLoader.loadClass("com.example.MyDataFlow")
        println("Constructors ${generatedClazz.constructors.size}")
        assertEquals(generatedClazz.constructors.size, 0)

        val generatedKlass = generatedClazz.kotlin
//        println("Constructors ${generatedKlass.constructors.size}")
//        assertNotNull(generatedKlass.constructors.singleOrNull {it.parameters.size == 2}, "Generated class must have constructor")
//        assertNotNull(generatedClass.primaryConstructor, "Generated class must have a primary constructor.")

        // 2. Instantiate the generated class: val instance = MyDataFlow(i = 10, j = 20)
        val instance = generatedKlass.primaryConstructor!!.call(10, 20)
        assertNotNull(instance, "Failed to instantiate MyDataFlow")

        // 3. Get the 'all' StateFlow property from the instance
        val allStateFlowProperty = generatedKlass.members.first { it.name == "all" }
        val allStateFlow = allStateFlowProperty.call(instance)
        assertIs<StateFlow<*>>(allStateFlow, "'all' property is not a StateFlow")

        // 4. Verify the initial state
        val initialData = allStateFlow.value
        assertEquals(originalClass.primaryConstructor!!.call(10, 20), initialData, "Initial state is incorrect.")

        // 5. Get and call the 'setI' method: instance.setI(15)
        val setIMethod = generatedKlass.members.first { it.name == "setI" }
        setIMethod.call(instance, 15)

        // 6. Verify the updated state of the 'all' flow
        val updatedData = allStateFlow.value
        assertEquals(originalClass.primaryConstructor!!.call(15, 20), updatedData, "State was not updated correctly after calling setI.")

        // 7. Get and call the 'setJ' method: instance.setJ(99)
        val setJMethod = generatedKlass.members.first { it.name == "setJ" }
        setJMethod.call(instance, 99)

        // 8. Verify the final state
        val finalData = allStateFlow.value
        assertEquals(originalClass.primaryConstructor!!.call(15, 99), finalData, "State was not updated correctly after calling setJ.")

        // 9. (Optional) Also check an individual StateFlow property
        val iStateFlowProperty = generatedKlass.members.first { it.name == "i" }
        val iStateFlow = iStateFlowProperty.call(instance)
        assertIs<StateFlow<*>>(iStateFlow)
        assertEquals(15, iStateFlow.value, "'i' StateFlow has incorrect value.")
    }
}