package org.jetbrains.kotlin.tools.kompot.test

import com.intellij.rt.execution.junit.FileComparisonFailure
import java.io.File
import java.nio.file.FileSystems.*
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes


fun assertEqualsIgnoringSeparators(expectedFile: File, output: String) {
    if (!expectedFile.exists()) {
        expectedFile.parentFile.mkdirs()
        expectedFile.createNewFile()
    }
    val expectedText = expectedFile.readText().replace("\r\n", "\n")
    val actualText = output.replace("\r\n", "\n")

    if (expectedText != actualText)
        throw FileComparisonFailure("", expectedText, actualText, expectedFile.canonicalPath)
}

fun File.appendToName(s: String) = resolveSibling(name + s)