package org.jetbrains.kotlin.tools.kompot.postprocess

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionLoader
import org.jetbrains.kotlin.tools.kompot.commons.ScopeMarkersDescriptors.*
import org.jetbrains.kotlin.tools.kompot.commons.readVersionAnnotation
import org.jetbrains.kotlin.tools.kompot.commons.traceUpToVersionConstNode
import org.objectweb.asm.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode

class StripPostProcessor(
    val versionLoader: VersionLoader,
    val predicate: (Version?) -> Boolean
) {

    private inner class Visitor(val sub: ClassVisitor) : ClassNode(Opcodes.ASM5) {

        private var skipClass = false

        override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
            val superVisitor = super.visitAnnotation(desc, visible)

            return readVersionAnnotation(versionLoader, desc, superVisitor) { version ->
                if (!predicate(version)) {
                    skipClass = true
                }
            }
        }

        override fun visitMethod(
            access: Int,
            name: String?,
            desc: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            val mn = super.visitMethod(access, name, desc, signature, exceptions)
            mn as MethodNode

            val codeVisitor = MethodCodeVisitor(mn)

            return object : MethodVisitor(Opcodes.ASM5, codeVisitor) {
                override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
                    return readVersionAnnotation(versionLoader, desc, super.visitAnnotation(desc, visible)) { version ->
                        if (!predicate(version)) {
                            methods.remove(mn)
                            mv = null // Disconnect code visitor, optimisation
                        }
                    }
                }
            }
        }

        override fun visitEnd() {
            super.visitEnd()
            if (!skipClass) {
                accept(sub)
            }
        }

        private inner class MethodCodeVisitor(val methodNode: MethodNode) : MethodVisitor(Opcodes.ASM5, methodNode) {

            var eraseAfterNextJump = false
            var eraseBeforeLabel: Label? = null
            var erasedSomething = false

            override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, desc: String?, itf: Boolean) {
                if (owner == scopeMarkersContainerInternalName && mv != null) {
                    if (name == enterVersionScopeName) {
                        val versionLiteralNode = traceUpToVersionConstNode(methodNode.instructions.last)!!

                        val versionLiteral = versionLiteralNode.cst as String
                        val version = versionLoader.load(versionLiteral)
                        val keep = predicate(version)

                        if (keep) {
                            visitInsn(Opcodes.POP)
                            visitLdcInsn(true)
                        } else {

                            eraseVersionCheck(versionLiteralNode)
                            erasedSomething = true
                            eraseAfterNextJump = true
                            mv = null
                        }
                        return
                    } else if (name == leaveVersionScopeName) {
                        return
                    }
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf)
            }

            fun eraseVersionCheck(versionLiteralNode: LdcInsnNode) {
                //Erase version constant for discarded version block
                val labelOfVersionConst =
                    generateSequence(versionLiteralNode.previous) { it.previous }
                        .filterIsInstance<LabelNode>()
                        .first()

                generateSequence(labelOfVersionConst.next) { it.next }
                    .toList()
                    .reversed()
                    .forEach { methodNode.instructions.remove(it) }
            }

            override fun visitLabel(label: Label) {
                if (label == eraseBeforeLabel) {
                    mv = methodNode
                    eraseBeforeLabel = null
                }
                super.visitLabel(label)
            }

            override fun visitJumpInsn(opcode: Int, label: Label?) {
                super.visitJumpInsn(opcode, label)
                if (eraseAfterNextJump && opcode == Opcodes.IFEQ) {
                    eraseAfterNextJump = false
                    eraseBeforeLabel = label
                }
            }

            override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                if (erasedSomething) {
                    super.visitMaxs(-1, -1)
                } else {
                    super.visitMaxs(maxStack, maxLocals)
                }
            }


        }
    }

    fun createVisitor(sub: ClassVisitor): ClassVisitor {
        return Visitor(sub)
    }
}