package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.annotations.Visibility
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionHandler
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionLoader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ClassWriter.COMPUTE_FRAMES
import org.objectweb.asm.ClassWriter.COMPUTE_MAXS
import org.slf4j.Logger
import java.io.File
import kotlin.system.measureTimeMillis

class SupersetGenerator(
    val logger: Logger,
    val versionHandler: VersionHandler,
    val versionLoader: VersionLoader,
    val configuration: Configuration
) {

    val classesByFqName = mutableMapOf<String, List<SSGClass>>()
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
            classesByFqName.merge(node.fqName, listOf(node)) { all, new ->
                all + new.filter { newClass ->
                    all.none { merger.mergeClasses(it, newClass) }
                }
            }
        }
    }

    fun doOutput(outDir: File) {
        logger.info("\nStats: ${merger.S.formatStatistics()}")
        logger.info("Merge time: $mergeTime ms")


        val writeTime = measureTimeMillis {
            val writer = SSGClassWriter(configuration)
            classesByFqName.values.forEach { classes ->
                val clazz = classes.singleOrNull() ?: run {
                    logger.warn("Multiple classes with same name:\n ${classes.joinToString { it.debugText() }}")
                    classes.first()
                }
                val sub = File(outDir, clazz.fqName + ".class")
                sub.parentFile.mkdirs()
                val cw = ClassWriter(COMPUTE_FRAMES or COMPUTE_MAXS)
                writer.write(clazz, cw)
                sub.writeBytes(cw.toByteArray())
                logger.debug("W: $sub")
            }
        }
        logger.info("Write time: $writeTime ms")


    }
}