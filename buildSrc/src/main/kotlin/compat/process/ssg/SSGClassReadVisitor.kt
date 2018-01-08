package compat.process.ssg

import compat.process.Version
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.ACC_PRIVATE

const val kotlinMetadataDesc = "Lkotlin/Metadata;"

class SSGClassReadVisitor : ClassVisitor(Opcodes.ASM5) {

    lateinit var rootVersion: Version
    var result: SSGClass? = null
    var innerClasses = mutableMapOf<String, SSGInnerClassRef>()

    var ame = 0
    var afe = 0

    override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<String>?) {
        super.visit(version, access, name, signature, superName, interfaces)

        if (access hasFlag ACC_PRIVATE) return
        innerClasses.clear()
        result = SSGClass(access, name!!, signature, superName, interfaces, rootVersion)
    }

    override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<String>?): MethodVisitor? {
        if (result == null) return null
        if (access hasFlag ACC_PRIVATE) return null
        val method = SSGMethod(access, name!!, desc!!, signature, exceptions, rootVersion)
        return object : MethodVisitor(Opcodes.ASM5) {
            override fun visitEnd() {
                super.visitEnd()
                ame++
                result!!.addMethod(method)
                ame--
            }
        }
    }

    override fun visitField(access: Int, name: String?, desc: String?, signature: String?, value: Any?): FieldVisitor? {
        if (result == null) return null
        if (access hasFlag ACC_PRIVATE) return null
        val field = SSGField(access, name!!, desc!!, signature, value, rootVersion)

        return object : FieldVisitor(Opcodes.ASM5) {
            override fun visitEnd() {
                super.visitEnd()
                afe++
                result!!.addField(field)
                afe--
            }
        }
    }

    override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
        if (desc == kotlinMetadataDesc) {
            result?.isKotlin = true
        }
        return super.visitAnnotation(desc, visible)
    }

    override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
        super.visitInnerClass(name, outerName, innerName, access)
        if (result == null) return
        if (access hasFlag ACC_PRIVATE) return
        innerClasses[name] = SSGInnerClassRef(access, name, outerName, innerName)
    }

    override fun visitOuterClass(owner: String, name: String?, desc: String?) {
        super.visitOuterClass(owner, name, desc)
        if (result == null) return
        result!!.ownerInfo = OuterClassInfo(owner, name, desc)
    }

    override fun visitEnd() {
        super.visitEnd()
        val result = result ?: return
        if (innerClasses.isNotEmpty()) {
            result.innerClassesBySignature = mutableMapOf()
            result.innerClassesBySignature!!.putAll(innerClasses)
        }
        if (result.ownerInfo != null && result.visibility == Visibility.PACKAGE_PRIVATE) {
            this.result = null
        }
    }
}