package org.jetbrains.kotlin.compiler.plugin.template

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.dump

fun IrElement.print() {
    println("*".repeat(20))
    println(
        this.dump(
            options = DumpIrTreeOptions(normalizeNames = true, printSignatures = true)
        )
    )
    println("*".repeat(20))
}
