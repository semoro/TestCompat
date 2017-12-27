package compat.process

import org.gradle.api.Project
import org.gradle.api.file.FileTree
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path

fun Project.classpathToClassFiles(classpath: Iterable<File>): Sequence<Path> {
    val (dirs, files) = classpath.partition { it.isDirectory }
    val jars = files.filter { it.extension == "jar" }


    val dirClasses = dirs.asSequence().map {
        project.fileTree(it).include {
            it.isDirectory || it.name.endsWith(".class")
        } as FileTree
    }.flatten().map { it.toPath() }


    val jarClasses = jars.map { it.toPath() }.asSequence().map {
        FileSystems.newFileSystem(it, null).rootDirectories
    }.flatten().map {
        filteredFiles(it) { it.fileName.toString().endsWith("class") }
    }.flatten()

    return dirClasses + jarClasses
}