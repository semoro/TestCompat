package org.jetbrains.kotlin.tools.kompot.ssg

import org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter
import java.io.StringWriter

fun SSGClass.asmText(): String {
    val writer = SSGClassWriter()
    val sw = StringWriter()
    val cv = TraceClassVisitor(PrintWriter(sw))
    writer.write(this, cv)
    return sw.toString()
}
