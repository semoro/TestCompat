package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.commons.getOrInit
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.ACC_PRIVATE

const val kotlinMetadataDesc = "Lkotlin/Metadata;"

class SSGClassReadVisitor(private val rootVersion: Version?) : ClassVisitor(Opcodes.ASM5) {

    lateinit var result: SSGClass

    override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<String>?) {
        super.visit(version, access, name, signature, superName, interfaces)

        if (access hasFlag ACC_PRIVATE) return
        result = SSGClass(
            access,
            name!!,
            signature,
            superName,
            interfaces,
            rootVersion
        )
    }

    override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
        if (desc == kotlinMetadataDesc) {
            result.isKotlin = true
        }
        return super.visitAnnotation(desc, visible)
    }

    override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<String>?): MethodVisitor? {
        if (access hasFlag ACC_PRIVATE) return null
        val method =
            SSGMethod(access, name!!, desc!!, signature, exceptions, rootVersion)
        return object : MethodVisitor(Opcodes.ASM5) {
            override fun visitEnd() {
                super.visitEnd()
                result.addMethod(method)
            }
        }
    }

    override fun visitField(access: Int, name: String?, desc: String?, signature: String?, value: Any?): FieldVisitor? {
        if (access hasFlag ACC_PRIVATE) return null
        val field =
            SSGField(access, name!!, desc!!, signature, value, rootVersion)

        return object : FieldVisitor(Opcodes.ASM5) {
            override fun visitEnd() {
                super.visitEnd()
                result.addField(field)
            }
        }
    }


    override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
        super.visitInnerClass(name, outerName, innerName, access)
        if (access hasFlag ACC_PRIVATE) return

        val innerClassesBySignature = result::innerClassesBySignature.getOrInit { mutableMapOf() }
        innerClassesBySignature[name] = SSGInnerClassRef(access, name, outerName, innerName)
    }


    override fun visitOuterClass(owner: String, name: String?, desc: String?) {
        super.visitOuterClass(owner, name, desc)
        result.ownerInfo = OuterClassInfo(owner, name, desc)
    }
}