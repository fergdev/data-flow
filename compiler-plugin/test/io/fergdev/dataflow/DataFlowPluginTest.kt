package io.fergdev.dataflow

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.fergdev.dataflow.plugin.DataFlowComponentRegistrar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.text.contains
import kotlin.text.split

class DataFlowPluginTest {

    @Test
    fun `DataFlow plugin generates wrapper class`() {
        val source = SourceFile.kotlin(
            "MyData.kt", """
            package com.example1

            import io.fergdev.dataflow.annotations.DataFlow

            @DataFlow
            data class Example1Data(val i: Int, val j: String)
            """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(source)
            compilerPluginRegistrars = listOf(DataFlowComponentRegistrar())
            workingDir = File("build/compile-testing-out-1") // Or any other path
            inheritClassPath = true
            messageOutputStream = System.out
            val coroutinesCoreJar = System.getProperty("java.class.path")
                .split(File.pathSeparator)
                .first { it.contains("kotlinx-coroutines-core") }
            classpaths = listOf(File(coroutinesCoreJar))
        }.compile()

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, "Compilation failed!")


        // 1. Load the original and generated classes using Kotlin Reflection
        val originalClass = result.classLoader.loadClass("com.example1.Example1Data").kotlin
        val generatedClazz = result.classLoader.loadClass("com.example1.Example1Data\$DataFlow")
        assertEquals(generatedClazz.constructors.size, 1)

        val generatedKlass = generatedClazz.kotlin
        assertNotNull(
            generatedKlass.constructors.singleOrNull { it.parameters.size == 2 },
            "Generated class must have a constructor with 2 parameters."
        )

        // 2. Instantiate the generated class: val instance = Example1Data(i = 10, j = 20)
        val instance = generatedKlass.primaryConstructor!!.call(10, "20")
        assertNotNull(instance, "Failed to instantiate Example1Data")

        // 3. Get the 'all' StateFlow property from the instance
        val allStateFlowProperty = generatedKlass.memberProperties.first { it.name == "all" }
        val allStateFlow = allStateFlowProperty.call(instance)
        assertIs<MutableStateFlow<*>>(allStateFlow, "'a' property is not a StateFlow")

        // 4. Verify the initial state
        val initialData = allStateFlow.value
        assertEquals(
            originalClass.primaryConstructor!!.call(10, "20"),
            initialData,
            "Initial state is incorrect."
        )

        // 5. Get and call the 'setI' method: instance.setI(15)
        val setAMethod = generatedKlass.members.first { it.name == "updateI" }
        setAMethod.call(instance, 15)

        // 6. Verify the updated state of the 'all' flow
        val updatedData = allStateFlow.value
        assertEquals(
            originalClass.primaryConstructor!!.call(15, "20"),
            updatedData,
            "State was not updated correctly after calling updateI."
        )

        // 7. Get and call the 'setJ' method: instance.setJ(99)
        val setBMethod = generatedKlass.members.first { it.name == "updateJ" }
        setBMethod.call(instance, "99")

        // 8. Verify the final state
        val finalData = allStateFlow.value
        assertEquals(
            originalClass.primaryConstructor!!.call(15, "99"),
            finalData,
            "State was not updated correctly after calling setJ."
        )
    }

    @Test
    fun `DataFlow plugin generates a usable wrapper class - test calls`() = runBlocking {
        val source = SourceFile.kotlin(
            "MyData.kt", """
            package com.example2

            import io.fergdev.dataflow.annotations.DataFlow
            import io.fergdev.dataflow.annotations.DataFlowIgnore

            @DataFlow
            data class Example2Data(val a: Int, val b: String, @DataFlowIgnore val c: Int)
            """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(source)
            compilerPluginRegistrars = listOf(DataFlowComponentRegistrar())
            workingDir = File("build/compile-testing-out-2")
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
        val originalClass = result.classLoader.loadClass("com.example2.Example2Data").kotlin
        val generatedClazz = result.classLoader.loadClass("com.example2.Example2Data\$DataFlow")
        println("Constructors ${generatedClazz.constructors.size}")
        assertEquals(generatedClazz.constructors.size, 1)

        val generatedKlass = generatedClazz.kotlin
        println("Constructors ${generatedKlass.constructors.size}")
        assertNotNull(
            generatedKlass.constructors.singleOrNull { it.parameters.size == 3 },
            "Generated class must have a constructor with 3 parameters."
        )

        // 2. Instantiate the generated class: val instance = MyDataFlow(i = 10, j = 20)
        val instance = generatedKlass.primaryConstructor!!.call(10, "hi", 20)
        assertNotNull(instance, "Failed to instantiate MyDataFlow")

        // 3. Get the 'all' StateFlow property from the instance
        val allStateFlowProperty = generatedKlass.memberProperties.first { it.name == "all" }
        val allStateFlow = allStateFlowProperty.call(instance)
        assertIs<MutableStateFlow<*>>(allStateFlow, "'a' property is not a StateFlow")

        // 4. Verify the initial state
        val initialData = allStateFlow.value
        assertEquals(
            originalClass.primaryConstructor!!.call(10, "hi", 20),
            initialData,
            "Initial state is incorrect."
        )

        // 5. Get and call the 'setI' method: instance.setI(15)
        val setAMethod = generatedKlass.members.first { it.name == "updateA" }
        setAMethod.call(instance, 15)

        // 6. Verify the updated state of the 'all' flow
        val updatedData = allStateFlow.value
        assertEquals(
            originalClass.primaryConstructor!!.call(15, "hi", 20),
            updatedData,
            "State was not updated correctly after calling updateA."
        )

        // 7. Get and call the 'setJ' method: instance.setJ(99)
        val setBMethod = generatedKlass.members.first { it.name == "updateB" }
        setBMethod.call(instance, "99")

        // 8. Verify the final state
        val finalData = allStateFlow.value
        assertEquals(
            originalClass.primaryConstructor!!.call(15, "99", 20),
            finalData,
            "State was not updated correctly after calling setJ."
        )

        // 9. (Optional) Also check an individual StateFlow property
        val iStateFlowProperty = generatedKlass.members.first { it.name == "a" }
        val aStateFlow = iStateFlowProperty.call(instance)
        assertIs<StateFlow<*>>(aStateFlow)
        assertEquals(15, aStateFlow.value, "'a' StateFlow has incorrect value.")
    }
}


@Suppress("unused")
fun <T : Any> KClass<T>.debugPrint() {
    println(this.debugToString())
}

fun <T : Any> KClass<T>.debugToString(): String {
    val builder = StringBuilder()
    builder.appendLine("=".repeat(40))
    builder.appendLine("Debug Info for KClass: ${this.qualifiedName}")
    builder.appendLine("=".repeat(40))

    // Simple Name
    builder.appendLine("Simple Name: ${this.simpleName}")
    builder.appendLine("Visibility: ${this.visibility}")
    builder.appendLine("Is Data: ${this.isData}, Is Companion: ${this.isCompanion}, Is Fun: ${this.isFun}")

    // Constructors
    builder.appendLine("\nConstructors (${this.constructors.size}):")
    this.constructors.forEach { constructor ->
        val params = constructor.parameters.joinToString(", ") { "${it.name}: ${it.type}" }
        val primary = if (constructor == this.primaryConstructor) " [PRIMARY]" else ""
        builder.appendLine("  - ${constructor.visibility} fun <init>($params)$primary")
    }

    // Properties
    builder.appendLine("\nProperties (${this.memberProperties.size}):")
    this.memberProperties.forEach { prop ->
        builder.appendLine("  - ${prop.visibility} val ${prop.name}: ${prop.returnType}")
    }

    // Functions
    builder.appendLine("\nFunctions (${this.memberFunctions.size}):")
    this.memberFunctions.forEach { func ->
        val params = func.parameters.drop(1) // Drop the instance parameter
            .joinToString(", ") { "${it.name ?: "_"}: ${it.type}" }
        builder.appendLine("  - ${func.visibility} fun ${func.name}($params): ${func.returnType}")
    }

    builder.appendLine("=".repeat(40))
    return builder.toString()
}

@Suppress("unused")
fun Class<*>.debugPrint() {
    println(debugToString())
}

/**
 * A debug extension function to print a detailed, human-readable summary of a java.lang.Class.
 * This is useful for inspecting a class immediately after loading it from a class loader.
 */
fun Class<*>.debugToString(): String {
    val builder = StringBuilder()
    builder.appendLine("=".repeat(40))
    builder.appendLine("Debug Info for Java Class: ${this.canonicalName}")
    builder.appendLine("=".repeat(40))

    // Class Name & Modifiers
    builder.appendLine("Simple Name: ${this.simpleName}")
    val classModifiers = java.lang.reflect.Modifier.toString(this.modifiers)
    builder.appendLine("Modifiers: $classModifiers")

    // Constructors
    builder.appendLine("\nConstructors (${this.declaredConstructors.size}):")
    this.declaredConstructors.forEach { constructor ->
        val modifiers = java.lang.reflect.Modifier.toString(constructor.modifiers)
        // FIX: Use generic parameter types for constructors
        val params = constructor.parameters.joinToString(", ") { "${it.name}: ${it.parameterizedType.typeName}" }
        builder.appendLine("  - $modifiers fun <init>($params)")
    }

    // Fields (corresponds to properties)
    builder.appendLine("\nFields (${this.declaredFields.size}):")
    this.declaredFields.forEach { field ->
        val modifiers = java.lang.reflect.Modifier.toString(field.modifiers)
        // FIX: Use genericType to get type parameters for fields
        builder.appendLine("  - $modifiers val ${field.name}: ${field.genericType.typeName}")
    }

    // Methods
    builder.appendLine("\nMethods (${this.declaredMethods.size}):")
    this.declaredMethods.forEach { method ->
        val modifiers = java.lang.reflect.Modifier.toString(method.modifiers)
        // FIX: Use generic parameter types for methods
        val params = method.parameters.joinToString(", ") { "${it.name}: ${it.parameterizedType.typeName}" }
        // FIX: Use genericReturnType for method return values
        builder.appendLine("  - $modifiers fun ${method.name}($params): ${method.genericReturnType.typeName}")
    }

    builder.appendLine("=".repeat(40))
    return builder.toString()
}

