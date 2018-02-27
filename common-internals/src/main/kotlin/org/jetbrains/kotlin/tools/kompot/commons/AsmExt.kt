package org.jetbrains.kotlin.tools.kompot.commons

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionLoader
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Opcodes
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


fun readVersionAnnotation(
    versionLoader: VersionLoader,
    desc: String?,
    superVisitor: AnnotationVisitor? = null,
    out: (Version) -> Unit
): AnnotationVisitor? {
    if (desc != existsInDesc && desc != compatibleWithDesc) {
        return superVisitor
    }
    return object : AnnotationVisitor(Opcodes.ASM5, superVisitor) {
        override fun visit(name: String?, value: Any?) {
            if (name == "version") {
                out(versionLoader.load(value as String))
            }
            super.visit(name, value)
        }
    }
}