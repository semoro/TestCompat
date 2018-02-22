package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.commons.formatForReport
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter
import java.io.StringWriter

fun SSGClass.asmText(): String {
    val writer = SSGClassWriter(withBodyStubs = false, configuration = Configuration(writeParameters = true))
    val sw = StringWriter()
    val cv = TraceClassVisitor(PrintWriter(sw))
    writer.write(this, cv)
    return sw.toString()
}

fun Version?.forDisplay(): String {
    if (this == null) return ""
    return "@ExistsIn(${this.asLiteralValue()}) "
}


fun SSGMethod.debugText(): String {
    return buildString {
        append(version.forDisplay())

        append(access.presentableVisibility + " ")
        if (access hasFlag Opcodes.ACC_ABSTRACT) {
            append("abstract ")
        } else if (!(access hasFlag Opcodes.ACC_FINAL)) {
            append("open ")
        }
        if (access hasFlag Opcodes.ACC_STATIC) {
            append("static ")
        }

        append("fun ")
        append(name)

        append(Type.getMethodType(desc).formatForReport())
    }
}

fun SSGField.debugText(): String {
    return buildString {
        append(version.forDisplay())

        append(access.presentableVisibility + " ")

        if (access hasFlag Opcodes.ACC_STATIC) {
            append("static ")
        }

        if (access hasFlag Opcodes.ACC_FINAL) {
            append("var")
        } else {
            append("val")
        }

        append(" $name: ")
        append(Type.getType(desc).formatForReport())
    }
}

fun SSGClass.debugText(): String {
    return buildString {
        append(version.forDisplay())

        append(access.presentableVisibility + " ")

        if (access hasFlag Opcodes.ACC_FINAL) {
            append("final ")
        }

        appendln(access.presentableKind + " $fqName {")

        if (methodsBySignature.isNotEmpty()) {
            methodsBySignature.values.joinTo(this, separator = "\n\t", prefix = "\t", postfix = "\n") { it.debugText() }
        }

        if (fieldsBySignature.isNotEmpty()) {
            fieldsBySignature.values.joinTo(this, separator = "\n\t", prefix = "\t", postfix = "\n")
        }
        appendln("}")
    }
}

fun AnnotationNode.debugText(): String {

    fun Any?.recurse(): String {
        return when (this) {
            is AnnotationNode -> if (values == null) {
                desc
            } else {
                values.chunked(2) { (a, b) ->
                    a as String to b
                }.joinToString(prefix = "$desc(", postfix = ")") { (k, v) ->
                    "$k = ${v.recurse()}"
                }
            }
            is Array<*> -> this.joinToString(prefix = "{", postfix = "}") { it.recurse() }
            is List<*> -> this.joinToString(prefix = "[", postfix = "]") { it.recurse() }
            else -> toString()
        }
    }

    return recurse()
}