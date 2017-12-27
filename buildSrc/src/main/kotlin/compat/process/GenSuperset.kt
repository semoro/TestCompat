package compat.process

import compat.process.ssg.SupersetGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.Serializable

open class GenSuperset : DefaultTask() {


    var input: Iterable<InputRoots> = mutableListOf()

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


    @OutputDirectories
    val output = project.files("classes")




    @TaskAction
    fun doGenerate() {
        File(project.projectDir, "classes/api").mkdirs()

        val generator = SupersetGenerator(logger)

        input.forEach {
            generator.appendClasses(project.classpathToClassFiles(it.files), it.version)
        }
        generator.doOutput()
    }
}


interface Version : Serializable {
    operator fun plus(other: Version?): Version
    fun asLiteralValue(): String
    override fun equals(other: Any?): Boolean
}


data class InputRoots(val files: Iterable<File>, val version: Version) : Serializable
