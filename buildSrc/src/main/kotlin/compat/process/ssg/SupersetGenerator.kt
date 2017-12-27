package compat.process.ssg

import compat.process.Version
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.SKIP_FRAMES
import org.objectweb.asm.ClassWriter
import org.slf4j.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class SupersetGenerator(val logger: Logger) {

    val ssgClasses = mutableMapOf<String, SSGClass>()
    val merger = SSGMerger(this)

    fun appendClasses(classes: Sequence<Path>, version: Version) {

        val visitor = SSGClassReadVisitor()
        classes.map {
            //println("R: $it")
            Files.newInputStream(it)
        }.map {
            ClassReader(it)
        }.forEach {
            visitor.rootVersion = version
            it.accept(visitor, SKIP_FRAMES)
            visitor.result?.let {
                appendClassNode(it)
                visitor.result = null
            }
        }
    }

    fun appendClassNode(node: SSGClass) {
        ssgClasses[node.fqName]?.let { merger.mergeClasses(it, node) } ?:
                run { ssgClasses[node.fqName] = node }
    }

    fun cleanupVersions() {
        ssgClasses.values.forEach { clz ->
            clz.methods.values.forEach { meth ->
                if (meth.version == clz.version) {
                    meth.version = null
                }
            }
            clz.fields.values.forEach { field ->
                if (field.version == clz.version) {
                    field.version = null
                }
            }
        }
    }

    fun doOutput() {
        cleanupVersions()
        println(ssgClasses.map { it.value }.filter { it.fqName.startsWith("api") }.joinToString(separator = "\n\n"))

        val outDir = File("classes")
        val writer = SSGClassWriter()
        ssgClasses.values.forEach {
            val sub = File(outDir, it.fqName + ".class")
            sub.parentFile.mkdirs()
            val cw = ClassWriter(0)
            writer.write(it, cw)
            sub.writeBytes(cw.toByteArray())
            //println("W: $sub")
        }
    }
}