package org.jetbrains.kotlin.tools.kompot.verifier

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionInfoProvider
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionLoader
import org.jetbrains.kotlin.tools.kompot.commons.compatibleWithDesc
import org.jetbrains.kotlin.tools.kompot.commons.existsInDesc
import org.jetbrains.kotlin.tools.kompot.commons.readVersionAnnotation
import org.objectweb.asm.*

class ClassReadVersionInfoProvider(val versionLoader: VersionLoader) : VersionInfoProvider {

    override fun forField(fqDescriptor: String): Version? {
        return fieldToVersionInfo[fqDescriptor]
    }

    override fun forClass(fqDescriptor: String): Version? {
        return classToVersionInfo[fqDescriptor]
    }

    override fun forMethod(fqDescriptor: String): Version? {
        return methodToVersionInfo[fqDescriptor]
    }


    val classToVersionInfo = mutableMapOf<String, Version>()
    val fieldToVersionInfo = mutableMapOf<String, Version>()
    val methodToVersionInfo = mutableMapOf<String, Version>()

    private inner class ClassCompatReadVisitor : ClassVisitor(Opcodes.ASM5) {

        fun doVisitAnnotation(desc: String?, out: (Version) -> Unit): AnnotationVisitor? {
            return readVersionAnnotation(versionLoader, desc, null, out)
        }

        lateinit var classFqName: String
        override fun visit(
            version: Int,
            access: Int,
            name: String?,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            classFqName = name!!
            super.visit(version, access, name, signature, superName, interfaces)
        }

        override fun visitMethod(
            access: Int,
            name: String?,
            desc: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor? {
            return object : MethodVisitor(Opcodes.ASM5) {
                override fun visitAnnotation(annotationDesc: String?, visible: Boolean): AnnotationVisitor? {
                    return doVisitAnnotation(annotationDesc) { methodToVersionInfo["$classFqName.$name $desc"] = it }
                }
            }
        }


        override fun visitField(
            access: Int,
            name: String?,
            desc: String?,
            signature: String?,
            value: Any?
        ): FieldVisitor {
            return object : FieldVisitor(Opcodes.ASM5) {
                override fun visitAnnotation(annotationDesc: String?, visible: Boolean): AnnotationVisitor? {
                    return doVisitAnnotation(annotationDesc) { fieldToVersionInfo["$classFqName.$name $desc"] = it }
                }
            }
        }

        override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
            return doVisitAnnotation(desc) { classToVersionInfo[classFqName.replace('/', '.')] = it }
        }
    }

    fun createVisitor(): ClassVisitor {
        return ClassCompatReadVisitor()
    }
}