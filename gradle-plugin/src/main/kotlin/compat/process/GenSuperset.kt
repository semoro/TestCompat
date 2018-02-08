package compat.process/*
package compat.process

import compat.process.ssg.SupersetGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.Serializable

open class GenSuperset : DefaultTask() {

    var input: Iterable<InputRoots> = mutableListOf()

    lateinit var versionHandler: VersionHandler

    fun root(files: Iterable<File>, version: Version): InputRoots {
        return InputRoots(files, version)
    }

    @Input
    fun getInputVersions(): Iterable<Version> {
        return input.map { it.version }
    }

    @InputFiles
    fun getInputFiles(): Iterable<File> {
        return input.flatMap { it.files }.flatMap {
            if (it.isDirectory) project.fileTree(it) else project.files(it)
        }
    }


    @OutputDirectory
    val output = project.file("${project.buildDir}/superset")


    @TaskAction
    fun doGenerate() {

        val generator = SupersetGenerator(logger, versionHandler)

        input.forEach {
            generator.appendClasses(project.classpathToClassFiles(it.files), it.version)
        }
        generator.doOutput(output)
    }
}

interface VersionHandler : Serializable {
    fun plus(t: Version?, other: Version?): Version?
    fun contains(t: Version?, other: Version?): Boolean
}


interface Version : Serializable {
    fun asLiteralValue(): String
    override fun equals(other: Any?): Boolean
}


data class InputRoots(val files: Iterable<File>, val version: Version) : Serializable
*/
