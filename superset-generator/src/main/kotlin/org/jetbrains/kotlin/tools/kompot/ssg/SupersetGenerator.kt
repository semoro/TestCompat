package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.annotations.Visibility
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionHandler
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ClassWriter.COMPUTE_FRAMES
import org.objectweb.asm.ClassWriter.COMPUTE_MAXS
import org.slf4j.Logger
import java.io.File
import kotlin.system.measureTimeMillis

class SupersetGenerator(val logger: Logger, val versionHandler: VersionHandler, val configuration: Configuration) {

    val classesByFqName = mutableMapOf<String, SSGClass>()
    val merger = SSGMerger(logger, versionHandler)

    fun appendClasses(classes: Sequence<SSGClass>) {
        classes.filter {
            !it.isMemberClass || it.visibility != Visibility.PACKAGE_PRIVATE
        }.forEach {
            appendClassNode(it)
        }
    }

    var mergeTime = 0L

    fun appendClassNode(node: SSGClass) {
        mergeTime += measureTimeMillis {
            classesByFqName[node.fqName]?.let { merger.mergeClasses(it, node) } ?: run {
                classesByFqName[node.fqName] = node
            }
        }
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
        println(merger.S.formatStatistics())
        println("Merge time: $mergeTime ms")
        val writeTime = measureTimeMillis {
            val writer = SSGClassWriter(configuration)
            classesByFqName.values.forEach {
                val sub = File(outDir, it.fqName + ".class")
                sub.parentFile.mkdirs()
                val cw = ClassWriter(COMPUTE_FRAMES or COMPUTE_MAXS)
                writer.write(it, cw)
                sub.writeBytes(cw.toByteArray())
                logger.debug("W: $sub")
            }
        }
        println("Write time: $writeTime ms")




    }
}