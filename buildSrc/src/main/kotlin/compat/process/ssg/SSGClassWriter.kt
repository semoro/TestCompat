package compat.process.ssg

import compat.process.altVisDesc
import compat.process.existsInDesc
import compat.process.visEnumDesc
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.ACC_ABSTRACT

class SSGClassWriter {

    fun SSGVersionContainer.writeVersion(getVisitor: () -> AnnotationVisitor?) {
        val version = version ?: return
        getVisitor()?.apply {
            visit("version", version.asLiteralValue())
            visitEnd()
        }
    }

    fun SSGAlternativeVisibilityContainer.writeAlternativeVisibility(getVisitor: () -> AnnotationVisitor?) {
        val alternativeVisibility = alternativeVisibility ?: return
        getVisitor()?.apply {
            val (visibilities, versions) = alternativeVisibility.toList().filter { it.second != null }.unzip()
            visitArray("version")?.apply {
                versions.forEach { visit(null, it!!.asLiteralValue()) }
                visitEnd()
            }
            visitArray("visibility")?.apply {
                for (visibility in visibilities) {
                    visitEnum("visibility", visEnumDesc, visibility.name)
                }
                visitEnd()
            }
            visitEnd()
        }
    }

    private fun ClassVisitor.writeOuterClassInfo(info: OuterClassInfo?) {
        if (info == null) return
        visitOuterClass(info.owner, info.methodName, info.methodDesc)
    }

    private fun MethodVisitor.writeStubBody() {
        visitCode()
        visitTypeInsn(Opcodes.NEW, "java/lang/Exception")
        visitInsn(Opcodes.DUP)
        visitLdcInsn("Superset stub body should never be called!")
        visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Exception", "<init>", "(Ljava/lang/String;)V", false)
        visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Throwable")
        visitInsn(Opcodes.ATHROW)
    }

    fun write(node: SSGClass, classWriter: ClassVisitor) {

        classWriter.visit(Opcodes.V1_8, node.access, node.fqName, node.signature, node.superType, node.interfaces)

        classWriter.writeOuterClassInfo(node.ownerInfo)

        node.writeVersion { classWriter.visitAnnotation(existsInDesc, true) }
        node.writeAlternativeVisibility { classWriter.visitAnnotation(altVisDesc, true) }

        node.fieldsBySignature.values.forEach {
            classWriter.visitField(it.access, it.name, it.desc, it.signature, it.value)?.apply {
                    it.writeVersion { visitAnnotation(existsInDesc, true) }
                    visitEnd()
                }
        }

        node.methodsBySignature.values.forEach {
            classWriter.visitMethod(it.access, it.name, it.desc, it.signature, it.exceptions)?.apply {
                    it.writeVersion { visitAnnotation(existsInDesc, true) }
                    if (it.access noFlag ACC_ABSTRACT) {
                        writeStubBody()
                    }
                    visitMaxs(-1, -1)
                    visitEnd()
                }
        }

        node.innerClassesBySignature?.let {
            it.values.forEach {
                classWriter.visitInnerClass(it.name, it.outerName, it.innerName, it.access)
            }
        }

        classWriter.visitEnd()
    }
}