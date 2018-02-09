package org.jetbrains.kotlin.tools.kompot.commons

import org.objectweb.asm.Type

fun Type.formatForReport(): String {
    return when (sort) {
        Type.VOID -> "Unit"
        Type.ARRAY -> "Array<" + elementType.formatForReport() + ">"
        Type.INT -> "Int"
        Type.BOOLEAN -> "Boolean"
        Type.BYTE -> "Byte"
        Type.CHAR -> "Char"
        Type.DOUBLE -> "Double"
        Type.FLOAT -> "Float"
        Type.LONG -> "Long"
        Type.SHORT -> "Short"
        Type.OBJECT -> className
        Type.METHOD -> "(" + argumentTypes.joinToString { it.formatForReport() } + "): " + returnType.formatForReport()
        else -> error("Unsupported type kind")
    }
}