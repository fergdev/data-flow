package io.fergdev.dataflow.plugin

import java.util.Locale

fun String.titleCase() = this.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
}