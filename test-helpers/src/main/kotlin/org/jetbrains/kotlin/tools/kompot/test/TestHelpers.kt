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


fun classpathToClassFiles(classpath: Iterable<File>): Sequence<Path> {
    val (dirs, files) = classpath.partition { it.isDirectory }
    val jars = files.filter { it.extension == "jar" }

    val dirClasses = dirs.asSequence().map {
        it.walkTopDown().filter { it.extension == "class" }
    }.flatten().map { it.toPath() }


    val jarClasses = jars.map { it.toPath() }.asSequence().map {
        newFileSystem(it, null).rootDirectories
    }.flatten().map {
        filteredFiles(it) { it.fileName.toString().endsWith("class") }
    }.flatten()

    return dirClasses + jarClasses
}


fun filteredFiles(root: Path, f: (Path) -> Boolean): List<Path> {
    val out = mutableListOf<Path>()
    Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            if (f(file)) {
                out.add(file)
            }
            return super.visitFile(file, attrs)
        }
    })
    return out
}

fun File.appendToName(s: String) = resolveSibling(name + s)