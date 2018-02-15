package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.commons.formatForReport
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter
import java.io.StringWriter

fun SSGClass.asmText(): String {
    val writer = SSGClassWriter(withBodyStubs = false)
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