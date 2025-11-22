package io.fergdev.dataflow.plugin

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.dump
import java.util.Locale

fun IrElement.print() {
    println("*".repeat(20))
    println(
        this.dump(
            options = DumpIrTreeOptions(normalizeNames = true, printSignatures = true)
        )
    )
    println("*".repeat(20))
}

fun String.titleCase() = this.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
}