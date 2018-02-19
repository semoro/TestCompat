package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.annotations.Modality
import org.jetbrains.kotlin.tools.kompot.api.annotations.Visibility
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AnnotationNode

const val VISIBILITY_MASK = ACC_PUBLIC or ACC_PRIVATE or ACC_PUBLIC or ACC_PROTECTED
const val KIND_MASK = ACC_INTERFACE or ACC_ANNOTATION or ACC_ENUM
const val MODALITY_MASK = ACC_ABSTRACT or ACC_FINAL
const val METHOD_KIND_MASK =
    ACC_BRIDGE or ACC_SYNTHETIC or ACC_STATIC or ACC_NATIVE or ACC_SYNCHRONIZED or ACC_STRICT or ACC_VARARGS

var SSGAccess.visibility: Visibility
    get() {
        return with(access) {
            when {
                hasFlag(ACC_PUBLIC) -> Visibility.PUBLIC
                hasFlag(ACC_PROTECTED) -> Visibility.PROTECTED
                hasFlag(ACC_PRIVATE) -> Visibility.PRIVATE
                else -> Visibility.PACKAGE_PRIVATE
            }
        }
    }
    set(value) {
        val flag = when (value) {
            Visibility.PUBLIC -> ACC_PUBLIC
            Visibility.PROTECTED -> ACC_PROTECTED
            Visibility.PACKAGE_PRIVATE -> 0
            Visibility.PRIVATE -> ACC_PRIVATE
        }
        access = access and (VISIBILITY_MASK.inv()) or flag
    }


var SSGAccess.modality: Modality
    get() {
        return when {
            access.hasFlag(ACC_ABSTRACT) -> Modality.ABSTRACT
            access.hasFlag(ACC_FINAL) -> Modality.FINAL
            else -> Modality.OPEN
        }
    }
    set(value) {
        val flag = when (value) {
            Modality.FINAL -> ACC_FINAL
            Modality.ABSTRACT -> ACC_ABSTRACT
            Modality.OPEN -> 0
        }
        access = access and (MODALITY_MASK.inv()) or flag
    }


fun AnnotationNode.flattenedValues(): List<Any?> {
    val result = mutableListOf<Any?>()
    fun Iterable<Any?>.recurse() {
        for (el in this) {
            when (el) {
                is Iterable<*> -> el.recurse()
                is Array<*> -> el.asIterable().recurse()
                else -> result += el
            }
        }
    }
    values?.recurse()
    return result
}