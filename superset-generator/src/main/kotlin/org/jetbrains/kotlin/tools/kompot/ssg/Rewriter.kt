package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.commons.ClassSignatureNode
import org.jetbrains.kotlin.tools.kompot.commons.TypeArgument
import org.jetbrains.kotlin.tools.kompot.commons.TypeSignatureNode
import org.jetbrains.kotlin.tools.kompot.commons.TypeVariable
import org.objectweb.asm.Type

class Rewriter(
    val rewriteMap: Map<GroupedClass, String>,
    val groupedClassMerger: GroupedClassMerger
) : SignatureLoader by groupedClassMerger.signatureLoader {


    fun <T : String?> (T).rewrite(version: Version): T = with(groupedClassMerger) {
        if (this@rewrite == null) return null as T
        return rewriteMap[version.lookupGroupedClass(this@rewrite)] as T ?: this@rewrite
    }

    fun Type.rewrite(version: Version): Type {
        if (sort != Type.OBJECT && sort != Type.METHOD) return this

        return when (sort) {
            Type.OBJECT -> {
                Type.getObjectType(internalName.rewrite(version))
            }
            Type.METHOD -> {
                Type.getMethodType(
                    returnType.rewrite(version),
                    *argumentTypes.map { it.rewrite(version) }.toTypedArray()
                )
            }
            else -> this
        }
    }

    fun String.rewriteDescriptor(version: Version): String {
        return Type.getType(this).rewrite(version).descriptor
    }

    fun SSGMethodOrGroup.rewrite(version: Version): SSGMethodOrGroup {
        this.methods.forEach {
            it.desc = it.desc.rewriteDescriptor(version)
            it.exceptions = it.exceptions?.map { it.rewrite(version) }?.toTypedArray()
        }
        return this
    }

    fun SSGField.rewrite(version: Version): SSGField {
        desc = desc.rewriteDescriptor(version)
        return this
    }


    fun TypeVariable.rewrite(version: Version): TypeVariable {
        return this.copy(
            classBound = classBound?.rewrite(version),
            interfaceBounds = interfaceBounds.map { it.rewrite(version) }
        )
    }

    fun TypeArgument.rewrite(version: Version): TypeArgument {
        return when(this) {
            is TypeArgument.Unbounded -> this
            is TypeArgument.Bounded -> this.copy(node = node.rewrite(version))
        }
    }

    fun <T : TypeSignatureNode> T.rewrite(version: Version): T {
        return when (this) {
            is TypeSignatureNode.ClassType -> {
                val classArgs = classArgs.map { it.rewrite(version) }
                val classTypeName = classTypeName.rewrite(version)


                val inners = inners.map {
                    it.first.rewrite(version) to it.second.map { it.rewrite(version) }
                }
                TypeSignatureNode.ClassType(classTypeName, classArgs, inners) as T
            }
            is TypeSignatureNode.ArrayType -> {
                this.copy(type = type.rewrite(version)) as T
            }
            else -> this
        }
    }

    fun ClassSignatureNode.rewrite(version: Version): ClassSignatureNode {

        return ClassSignatureNode().also {
            it.interfaces = interfaces.map { it.rewrite(version) }
            it.superClass = superClass?.rewrite(version)
            it.typeVariables = typeVariables.map { it.rewrite(version) }
        }
    }

    fun SSGSignature.rewrite(version: Version): String? {
        if (signature == null) return null
        return loadedSignature.rewrite(version).toSignature()
    }


    fun GroupedClass.rewrite(): List<SSGClass> {
        return classes.onEach { clz ->
            clz.signature = (this as SSGSignature).rewrite(version)
            clz.fqName = clz.fqName.rewrite(clz.version)
            clz.superType = clz.superType.rewrite(clz.version)
            clz.interfaces.forEach { it.rewrite(clz.version) }
            clz.ownerInfo?.run { owner = owner.rewrite(clz.version) }

            clz.innerClassesBySignature.values.forEach {
                it.name = it.name.rewrite(version)
            }

            val methods = clz.methodsBySignature.values.map { it.rewrite(clz.version) }
            clz.methodsBySignature.clear()
            methods.forEach { clz.addMethod(it) }

            val fields = clz.fieldsBySignature.values.map { it.rewrite(clz.version) }
            clz.fieldsBySignature.clear()
            fields.forEach { clz.addField(it) }

        }
    }

}