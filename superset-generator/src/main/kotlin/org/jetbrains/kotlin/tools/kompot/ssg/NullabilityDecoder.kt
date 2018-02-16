package org.jetbrains.kotlin.tools.kompot.ssg

import org.objectweb.asm.AnnotationVisitor

object NullabilityDecoder {

    fun visitAnnotation(desc: String?, visible: Boolean, after: () -> Unit): AnnotationVisitor? {
        return null
    }
}