package compat.process.ssg

import compat.process.Version
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ACC_PRIVATE

class SSGClassReadVisitor : ClassVisitor(Opcodes.ASM5) {

    lateinit var rootVersion: Version
    var result: SSGClass? = null

    var ame = 0
    var afe = 0

    override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<String>?) {
        super.visit(version, access, name, signature, superName, interfaces)

        if (access hasFlag ACC_PRIVATE) return
        result = SSGClass(access, name!!, superName, interfaces, rootVersion)
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

}