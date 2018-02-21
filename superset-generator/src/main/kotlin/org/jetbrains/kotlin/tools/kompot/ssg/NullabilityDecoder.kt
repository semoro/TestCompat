package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.commons.notNullDesc
import org.jetbrains.kotlin.tools.kompot.commons.nullableDesc
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.tree.AnnotationNode

object NullabilityDecoder {

    // TODO: Actually read reason (value) of annotation and store it somewhere
    fun visitAnnotation(desc: String?, visible: Boolean, writeTo: SSGNullabilityContainer): AnnotationVisitor? {
        return when (desc) {
            nullableDesc -> {
                writeTo.nullability = Nullability.NULLABLE
                AnnotationNode(desc)
            }
            notNullDesc -> {
                writeTo.nullability = Nullability.NOT_NULL
                AnnotationNode(desc)
            }
            else -> null
        }
    }
}