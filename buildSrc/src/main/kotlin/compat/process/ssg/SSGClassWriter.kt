package compat.process.ssg

import compat.process.altVisDesc
import compat.process.existsInDesc
import compat.process.visEnumDesc
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

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

    fun write(node: SSGClass, classWriter: ClassVisitor) {

        classWriter.visit(Opcodes.V1_8, node.access, node.fqName, null, node.superType, node.interfaces)

        classWriter.writeOuterClassInfo(node.ownerInfo)

        node.writeVersion { classWriter.visitAnnotation(existsInDesc, true) }
        node.writeAlternativeVisibility { classWriter.visitAnnotation(altVisDesc, true) }

        node.fieldsBySignature.values.forEach {
            classWriter.visitField(Opcodes.ACC_PUBLIC, it.name, it.desc, it.signature, it.value)?.apply {
                it.writeVersion { visitAnnotation(existsInDesc, true) }
            }
        }

        node.methodsBySignature.values.forEach {
            classWriter.visitMethod(Opcodes.ACC_PUBLIC, it.name, it.desc, it.signature, it.exceptions)?.apply {
                it.writeVersion { visitAnnotation(existsInDesc, true) }
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