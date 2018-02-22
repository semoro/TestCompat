package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.test.appendToName
import org.jetbrains.kotlin.tools.kompot.test.assertEqualsIgnoringSeparators
import org.objectweb.asm.ClassReader
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceClassVisitor
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class TestClassVerifier {

    fun verifyOut(actualOutputDir: File, actualFile: File, testDataDir: File, skipCode: Boolean = true) {
        val sw = StringWriter()
        PrintWriter(sw).use {
            val visitor = TraceClassVisitor(it)
            actualFile.inputStream().use {
                val reader = ClassReader(it)
                var flags = ClassReader.SKIP_FRAMES
                if (skipCode) {
                    flags = flags or ClassReader.SKIP_CODE
                }
                reader.accept(visitor, flags)
            }
        }
        val s = sw.toString()

        val expectedFile = testDataDir.resolve(actualFile.relativeTo(actualOutputDir)).appendToName(".txt")
        assertEqualsIgnoringSeparators(expectedFile, s)
    }
}