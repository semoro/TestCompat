package compat.process.ssg

import compat.process.Version
import compat.process.VersionHandler
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.SKIP_FRAMES
import org.objectweb.asm.ClassWriter
import org.slf4j.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class SupersetGenerator(val logger: Logger, val versionHandler: VersionHandler) {

    val classesByFqName = mutableMapOf<String, SSGClass>()
    val merger = SSGMerger(this)

    fun appendClasses(classes: Sequence<Path>, version: Version) {

        val visitor = SSGClassReadVisitor()
        var re = 0
        classes.map {
            //println("R: $it")
            Files.newInputStream(it)
        }.mapNotNull {
            try {
                ClassReader(it)
            } catch (e: Throwable) {
                logger.error("Error while reading class", e)
                re++
                null
            }
        }.forEach {
            visitor.rootVersion = version
            it.accept(visitor, SKIP_FRAMES)
            visitor.result?.let {
                appendClassNode(it)
                visitor.result = null
            }
        }

        println("Read stats: ")
        println("re: $re, ame: ${visitor.ame}, afe: ${visitor.afe}")
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
            val cw = ClassWriter(0)
            writer.write(it, cw)
            sub.writeBytes(cw.toByteArray())
            //println("W: $sub")
        }

    }
}