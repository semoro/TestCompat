package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.commons.getOrInit
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.ACC_PRIVATE
import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.tree.AnnotationNode

const val kotlinMetadataDesc = "Lkotlin/Metadata;"

class SSGClassReadVisitor(private val rootVersion: Version?) : ClassVisitor(Opcodes.ASM6) {

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
            return null
        }

        return readAnnotation(desc, result)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        desc: String?,
        signature: String?,
        exceptions: Array<String>?
    ): MethodVisitor? {
        if (access hasFlag ACC_PRIVATE) return null
        val method =
            SSGMethod(access, name!!, desc!!, signature, exceptions, rootVersion)
        val parameterCount = Type.getMethodType(desc).argumentTypes.size
        return object : MethodVisitor(Opcodes.ASM6) {

            override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
                return NullabilityDecoder.visitAnnotation(desc, visible, method)
                        ?: readAnnotation(desc, method)
                        ?: super.visitAnnotation(desc, visible)
            }

            var parameterPosition = 0
            val parameterInfoList = arrayOfNulls<SSGParameterInfo>(parameterCount)



            override fun visitParameter(name: String?, access: Int) {
                val parameterInfo = parameterInfoList.getOrInit(parameterPosition) { SSGParameterInfo(parameterPosition) }
                parameterInfo.name = name
                parameterInfo.access = access

                parameterPosition++
                super.visitParameter(name, access)
            }

            override fun visitParameterAnnotation(parameter: Int, desc: String?, visible: Boolean): AnnotationVisitor? {
                val parameterInfo = parameterInfoList.getOrInit(parameter) { SSGParameterInfo(parameter) }
                return NullabilityDecoder.visitAnnotation(desc, visible, parameterInfo)
                        ?: readAnnotation(desc, parameterInfo)
                        ?: super.visitParameterAnnotation(parameter, desc, visible)
            }

            override fun visitLocalVariable(
                name: String?,
                desc: String?,
                signature: String?,
                start: Label?,
                end: Label?,
                index: Int
            ) {
                val isStatic = method.access hasFlag ACC_STATIC
                val paramIndex = if (isStatic) index else index - 1
                if (paramIndex in 0 until parameterCount) {
                    val info = parameterInfoList.getOrInit(paramIndex) { SSGParameterInfo(paramIndex) }
                    info.name = name
                }
                super.visitLocalVariable(name, desc, signature, start, end, index)
            }

            override fun visitAnnotationDefault(): AnnotationVisitor {
                return object : AnnotationNode(Opcodes.ASM6, "") {
                    override fun visitEnd() {
                        super.visitEnd()
                        method.annotationDefaultValue = this
                    }
                }
            }

            override fun visitEnd() {
                super.visitEnd()
                method.parameterInfoArray = parameterInfoList
                result.addMethod(method)
            }
        }
    }

    override fun visitField(access: Int, name: String?, desc: String?, signature: String?, value: Any?): FieldVisitor? {
        if (access hasFlag ACC_PRIVATE) return null
        val field =
            SSGField(access, name!!, desc!!, signature, value, rootVersion)

        return object : FieldVisitor(Opcodes.ASM6) {

            override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
                return NullabilityDecoder.visitAnnotation(desc, visible, field)
                        ?: readAnnotation(desc, field)
                        ?: super.visitAnnotation(desc, visible)
            }

            override fun visitEnd() {
                super.visitEnd()
                result.addField(field)
            }
        }
    }


    override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
        super.visitInnerClass(name, outerName, innerName, access)

        val innerClassesBySignature = result::innerClassesBySignature.getOrInit { mutableMapOf() }
        innerClassesBySignature[name] = SSGInnerClassRef(access, name, outerName, innerName)
    }


    override fun visitOuterClass(owner: String, name: String?, desc: String?) {
        super.visitOuterClass(owner, name, desc)
        result.ownerInfo = OuterClassInfo(owner, name, desc)
    }

    fun readAnnotation(desc: String?, to: SSGAnnotated): AnnotationVisitor? {
        return AnnotationNode(desc).also { to.addAnnotation(it) }
    }
}