package compat.process.ssg

import compat.process.Version
import compat.process.existsInDesc
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

class SSGClassWriter {

    fun writeVersion(version: Version?, getVisitor: () -> AnnotationVisitor?) {
        if (version == null) return
        getVisitor()?.apply {
            visit("version", version.asLiteralValue())
            visitEnd()
        }
    }

    fun write(node: SSGClass, classWriter: ClassVisitor) {

        classWriter.visit(Opcodes.V1_8, node.access, node.fqName, null, node.superType, node.interfaces)
        writeVersion(node.version) { classWriter.visitAnnotation(existsInDesc, true) }

        node.fieldsBySignature.values.forEach {
            classWriter.visitField(Opcodes.ACC_PUBLIC, it.name, it.desc, it.signature, it.value)?.apply {
                writeVersion(it.version) { visitAnnotation(existsInDesc, true) }
            }
        }

        node.methodsBySignature.values.forEach {
            classWriter.visitMethod(Opcodes.ACC_PUBLIC, it.name, it.desc, it.signature, it.exceptions)?.apply {
                writeVersion(it.version) { visitAnnotation(existsInDesc, true) }
            }
        }
        classWriter.visitEnd()
    }
}