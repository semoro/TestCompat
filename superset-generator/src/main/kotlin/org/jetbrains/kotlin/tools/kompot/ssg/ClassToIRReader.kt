package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.objectweb.asm.ClassReader
import java.nio.file.Files
import java.nio.file.Path

class ClassToIRReader(classFiles: Sequence<Path>, version: Version?, configuration: Configuration) {
    var re = 0
    val classes =
        classFiles.map {
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
        }.map {
            val visitor =
                SSGClassReadVisitor(version, loadParameterNamesFromLVT = configuration.loadParameterNamesFromLVT)
            it.accept(visitor, ClassReader.SKIP_FRAMES)
            visitor.result
        }

}