package org.jetbrains.kotlin.tools.kompot.ssg

import jdk.internal.org.objectweb.asm.Opcodes
import org.jetbrains.kotlin.serialization.ClassData
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import org.objectweb.asm.AnnotationVisitor

private const val metadataClassKind = "k"
private const val metadataData = "d1"
private const val metadataStrings = "d2"

class KotlinMetadataAnnotationReader(val done: (ClassData) -> Unit) : AnnotationVisitor(Opcodes.ASM5) {

    var k: Int? = null
    lateinit var d1: Array<String>
    lateinit var d2: Array<String>

    override fun visit(name: String?, value: Any?) {
        if (name == metadataClassKind) {
            k = value as? Int
        }
    }

    override fun visitArray(name: String?): AnnotationVisitor {
        return object : AnnotationVisitor(Opcodes.ASM5) {
            val result = mutableListOf<String>()
            override fun visit(name: String?, value: Any?) {
                result += value as String
            }

            override fun visitEnd() {
                when (name) {
                    metadataData -> d1
                    metadataStrings -> d2
                }
            }
        }
    }

    override fun visitEnd() {
        if (k != 1) {
            error("Kind")
        }
        val data = JvmProtoBufUtil.readClassDataFrom(d1, d2)
        done(data)
    }
}