package io.fergdev.dataflow

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.template.DataFlowComponentRegistrar
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.text.contains
import kotlin.text.first
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
}