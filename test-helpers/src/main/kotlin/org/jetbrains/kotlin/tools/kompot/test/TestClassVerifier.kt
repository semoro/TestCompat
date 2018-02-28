package org.jetbrains.kotlin.tools.kompot.test

import org.objectweb.asm.ClassReader
import org.objectweb.asm.util.CheckClassAdapter
import org.objectweb.asm.util.TraceClassVisitor
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class TestClassVerifier {

    fun verifyOut(
        actualOutputDir: File,
        actualFile: File,
        testDataDir: File,
        skipCode: Boolean = true,
        verifyBytecode: Boolean = true
    ) {
        val sw = StringWriter()
        val s: String

        if (actualFile.exists()) {
            PrintWriter(sw).use { pw ->
                val visitor = TraceClassVisitor(pw)
                actualFile.inputStream().use {
                    val reader = ClassReader(it)
                    var flags = ClassReader.SKIP_FRAMES
                    if (skipCode) {
                        flags = flags or ClassReader.SKIP_CODE
                    }
                    reader.accept(visitor, flags)
                }

                if (verifyBytecode) {
                    actualFile.inputStream().use {
                        CheckClassAdapter.verify(ClassReader(it), false, pw)
                    }
                }
            }
            s = sw.toString()
        } else {
            s = "<no-file>"
        }


        val expectedFile = testDataDir.resolve(actualFile.relativeTo(actualOutputDir)).appendToName(".txt")
        assertEqualsIgnoringSeparators(expectedFile, s)
    }
}