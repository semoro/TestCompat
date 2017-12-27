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

    override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<String>?) {
        if (access and ACC_PRIVATE == 0) {
            result = SSGClass(access, name!!, superName, interfaces, rootVersion)
        }
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<String>?): MethodVisitor? {
        if (result == null) return null

        val method = SSGMethod(name!!, desc!!, signature, exceptions, rootVersion)
        return object : MethodVisitor(Opcodes.ASM5) {
            override fun visitEnd() {
                super.visitEnd()
                result!!.appendMethod(method)
            }
        }
    }

    override fun visitField(access: Int, name: String?, desc: String?, signature: String?, value: Any?): FieldVisitor? {
        if (result == null) return null
        val field = SSGField(name!!, desc!!, signature, value, rootVersion)

        return object : FieldVisitor(Opcodes.ASM5) {
            override fun visitEnd() {
                super.visitEnd()
                result!!.appendField(field)
            }
        }
    }

    override fun visitEnd() {
        super.visitEnd()
    }

}