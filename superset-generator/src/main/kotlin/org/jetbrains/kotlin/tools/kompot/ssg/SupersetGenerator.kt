package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.annotations.Visibility
import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionHandler
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.SKIP_FRAMES
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ClassWriter.COMPUTE_FRAMES
import org.objectweb.asm.ClassWriter.COMPUTE_MAXS
import org.slf4j.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class SupersetGenerator(val logger: Logger, val versionHandler: VersionHandler) {

    val classesByFqName = mutableMapOf<String, SSGClass>()
    val merger = SSGMerger(this)

    fun appendClasses(classes: Sequence<Path>, version: Version) {
        var re = 0
        classes.map {
            //println("R: $it")
            Files.newInputStream(it)
        }.mapNotNull {
            try {
                ClassReader(it)
            } catch (e: Throwable) {
                //logger.error("Error while reading class", e)
                re++
                null
            }
        }.forEach {
            val visitor = SSGClassReadVisitor(version)
            it.accept(visitor, SKIP_FRAMES)
            val result = visitor.result
            if (!(result.isMemberClass && result.visibility == Visibility.PACKAGE_PRIVATE)) {
                appendClassNode(result)
            }
        }
        println("Read stats:\nre: $re")
    }

    fun appendClassNode(node: SSGClass) {
        classesByFqName[node.fqName]?.let { merger.mergeClasses(it, node) } ?:
                run { classesByFqName[node.fqName] = node }
    }

    fun cleanupVersions() {
        classesByFqName.values.forEach { clz ->
            clz.methodsBySignature.values.forEach { meth ->
                if (meth.version == clz.version) {
                    meth.version = null
                }
            }
            clz.fieldsBySignature.values.forEach { field ->
                if (field.version == clz.version) {
                    field.version = null
                }
            }
        }
    }

    fun doOutput(outDir: File) {
        cleanupVersions()
        println(classesByFqName.map { it.value }.filter { it.fqName.startsWith("api") }.joinToString(separator = "\n\n"))
        println("Stats: ")
        println(merger.formatStatistics())
        val writer = SSGClassWriter()
        classesByFqName.values.forEach {
            val sub = File(outDir, it.fqName + ".class")
            sub.parentFile.mkdirs()
            val cw = ClassWriter(COMPUTE_FRAMES or COMPUTE_MAXS)
            writer.write(it, cw)
            sub.writeBytes(cw.toByteArray())
            //println("W: $sub")
        }

    }
}