package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.test.appendToName
import org.jetbrains.kotlin.tools.kompot.test.assertEqualsIgnoringSeparators
import org.objectweb.asm.ClassReader
import org.objectweb.asm.util.TraceClassVisitor
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class TestClassVerifier(val skipCode: Boolean = true) {

    fun verifyOut(actualOutputDir: File, testDataDir: File) {
        actualOutputDir.walkTopDown().filter { it.extension == "class" }.forEach { file ->
            val sw = StringWriter()
            PrintWriter(sw).use {
                val visitor = TraceClassVisitor(it)
                file.inputStream().use {
                    val reader = ClassReader(it)
                    var flags = ClassReader.SKIP_FRAMES
                    if (skipCode) {
                        flags = flags or ClassReader.SKIP_CODE
                    }
                    reader.accept(visitor, flags)
                }
            }
            val s = sw.toString()

            val expectedFile = testDataDir.resolve(file.relativeTo(actualOutputDir)).appendToName(".txt")
            assertEqualsIgnoringSeparators(expectedFile, s)
        }
    }
}