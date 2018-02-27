package org.jetbrains.kotlin.tools.kompot.verifier

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionCompareHandler
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionInfoProvider
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionLoader
import org.jetbrains.kotlin.tools.kompot.commons.ScopeMarkersDescriptors.*
import org.jetbrains.kotlin.tools.kompot.commons.compatibleWithDesc
import org.jetbrains.kotlin.tools.kompot.commons.forClass
import org.jetbrains.kotlin.tools.kompot.commons.formatForReport
import org.jetbrains.kotlin.tools.kompot.commons.traceUpToVersionConst
import org.objectweb.asm.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import java.util.*

class Verifier(
    val versionInfoProvider: VersionInfoProvider,
    val versionLoader: VersionLoader,
    val versionCompareHandler: VersionCompareHandler
) {
    val problems = mutableListOf<ProblemDescriptor>()

    private fun parseVersionData(literalValue: String): Version {
        return versionLoader.load(literalValue)
    }

    private fun Version?.disallows(other: Version?): Boolean {
        return versionCompareHandler.isSubset(other, this)
    }

    private inner class ClassCompatCheckingVisitor(val problemSink: (ProblemDescriptor) -> Unit) : ClassVisitor(
        Opcodes.ASM5
    ) {

        lateinit var className: String

        override fun visit(
            version: Int,
            access: Int,
            name: String?,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            className = name!!.replace('/', '.')
            super.visit(version, access, name, signature, superName, interfaces)
        }

        var source: String? = null

        override fun visitSource(source: String?, debug: String?) {
            super.visitSource(source, debug)
            this.source = source
        }

        var classLevelVersion: Version? = null
        fun doVisitAnnotation(annDesc: String?, out: (Version) -> Unit): AnnotationVisitor? {
            if (annDesc != compatibleWithDesc) {
                return null
            }
            return object : AnnotationVisitor(Opcodes.ASM5) {
                override fun visit(name: String?, value: Any?) {
                    if (name == "version") {
                        out(parseVersionData(value as String))
                    }
                    super.visit(name, value)
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
            val methodNode = MethodNode(access, name, desc, signature, exceptions)
            return object : MethodVisitor(Opcodes.ASM5, methodNode) {
                var methodLevelVersion: Version? = null
                override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
                    return doVisitAnnotation(desc) { methodLevelVersion = it }
                }

                val versionScopes = ArrayDeque<Version>()

                var currentScope: Version? = null

                override fun visitCode() {
                    currentScope = methodLevelVersion ?: classLevelVersion
                    super.visitCode()
                }

                var firstLine = -1
                var lineNumber = 0
                override fun visitLineNumber(line: Int, start: Label?) {
                    super.visitLineNumber(line, start)
                    lineNumber = line
                    if (firstLine == -1) {
                        firstLine = line
                    }
                }

                fun getSourceInfo(line: Int = lineNumber): ProblemDescriptor.SourceInfo {
                    return ProblemDescriptor.SourceInfo(
                        "$className.$name${Type.getMethodType(desc).formatForReport()}",
                        source ?: "unknown source",
                        "$line",
                        currentScope
                    )
                }

                override fun visitMethodInsn(
                    opcode: Int,
                    callTargetOwner: String?,
                    calleeName: String?,
                    calleeDesc: String?,
                    itf: Boolean
                ) {
                    super.visitMethodInsn(opcode, callTargetOwner, calleeName, calleeDesc, itf)

                    if (callTargetOwner == scopeMarkersContainerInternalName) {
                        if (calleeName == enterVersionScopeName) {
                            val versionString = traceUpToVersionConst(methodNode.instructions.last)
                                    ?: return println("WARN: Unresolved scope")
                            val vd = parseVersionData(versionString)
                            versionScopes.add(vd)
                            currentScope = vd
                        } else if (calleeName == leaveVersionScopeName) {
                            versionScopes.remove()
                            currentScope = if (versionScopes.isEmpty()) {
                                methodLevelVersion ?: classLevelVersion
                            } else {
                                versionScopes.peek()
                            }
                        }
                    } else {
                        val calleeOwnerType = Type.getObjectType(callTargetOwner)
                        val calleeOwnerInfo = versionInfoProvider.forClass(calleeOwnerType)
                        if (currentScope.disallows(calleeOwnerInfo)) {
                            problemSink(
                                ProblemDescriptor.TypeReferenceProblem(
                                    calleeOwnerType,
                                    calleeOwnerInfo,
                                    getSourceInfo()
                                )
                            )
                        }

                        val calleeVersionInfo = versionInfoProvider.forMethod("$callTargetOwner.$calleeName $calleeDesc")
                        val calleeMethodType = Type.getMethodType(calleeDesc)
                        if (currentScope.disallows(calleeVersionInfo)) {
                            problemSink(
                                ProblemDescriptor.MethodReferenceProblem(
                                    calleeOwnerType,
                                    calleeName!!,
                                    calleeMethodType,
                                    calleeVersionInfo,
                                    getSourceInfo()
                                )
                            )
                        }
                    }
                }

                override fun visitFieldInsn(opcode: Int, fowner: String?, fname: String?, fdesc: String?) {
                    super.visitFieldInsn(opcode, fowner, fname, fdesc)
                    val fieldVersionInfo = versionInfoProvider.forField("$fowner.$fname $fdesc")
                    if (currentScope.disallows(fieldVersionInfo)) {
                        val fieldOwnerType = Type.getType(fowner)
                        val fieldType = Type.getType(fdesc)
                        problemSink(
                            ProblemDescriptor.FieldReferenceProblem(
                                fieldOwnerType,
                                fname!!,
                                fieldType,
                                fieldVersionInfo,
                                getSourceInfo()
                            )
                        )
                    }
                }

                override fun visitTypeInsn(opcode: Int, typeName: String?) {
                    super.visitTypeInsn(opcode, typeName)
                    val type = Type.getObjectType(typeName)
                    val restriction = versionInfoProvider.forClass(type)
                    if (currentScope.disallows(restriction)) {
                        problemSink(
                            ProblemDescriptor.TypeReferenceProblem(
                                type,
                                restriction,
                                getSourceInfo()
                            )
                        )
                    }
                }

                override fun visitEnd() {
                    super.visitEnd()

                    val argTypes = Type.getArgumentTypes(desc) + Type.getReturnType(desc)
                    argTypes.forEach {
                        val restriction = versionInfoProvider.forClass(it)
                        if (currentScope.disallows(restriction)) {
                            problemSink(
                                ProblemDescriptor.TypeReferenceProblem(
                                    it,
                                    restriction,
                                    getSourceInfo(firstLine)
                                )
                            )
                        }
                    }
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
                var fieldLevelVersion: Version? = null
                override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
                    return doVisitAnnotation(desc) { fieldLevelVersion = it }
                }

                override fun visitEnd() {
                    val versionRestriction = fieldLevelVersion ?: classLevelVersion
                    val returnType = Type.getType(desc)
                    val restriction = versionInfoProvider.forClass(returnType.className)
                    if (versionRestriction.disallows(restriction)) {
                        problemSink(
                            ProblemDescriptor.TypeReferenceProblem(
                                returnType, restriction,
                                ProblemDescriptor.SourceInfo(
                                    "$className.$name: ${returnType.formatForReport()}",
                                    source!!,
                                    "?",
                                    versionRestriction
                                )
                            )
                        )
                    }
                }
            }
        }

        override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
            return doVisitAnnotation(desc) { classLevelVersion = it }
        }
    }

    val visitor: ClassVisitor = ClassCompatCheckingVisitor { problems += it }
}

fun Version?.formatForReport(ann: String = "@ExistsIn") = "$ann(${this?.asLiteralValue() ?: "*"})"
