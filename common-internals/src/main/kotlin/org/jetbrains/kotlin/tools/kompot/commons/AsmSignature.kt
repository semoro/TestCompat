package org.jetbrains.kotlin.tools.kompot.commons

import org.jetbrains.kotlin.tools.kompot.commons.TypeArgument.TypeArgumentWithVariance.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureVisitor


abstract class NodeWithTypeVariables : SignatureVisitor(Opcodes.ASM6) {
    var typeVariables: List<TypeVariable>? = null

    override fun visitFormalTypeParameter(name: String) {
        this.typeVariables = (this.typeVariables ?: emptyList()) + TypeVariable(name)
        super.visitFormalTypeParameter(name)
    }

    override fun visitClassBound(): SignatureVisitor {
        return TypeSignatureNodeBuilder {
            typeVariables!!.last().classBound = it
        }
    }

    override fun visitInterfaceBound(): SignatureVisitor {
        return TypeSignatureNodeBuilder {
            val variable = typeVariables!!.last()
            variable.interfaceBounds = (variable.interfaceBounds ?: emptyList()) + it
        }
    }

    open fun accept(sv: SignatureVisitor?) {
        sv ?: return
        typeVariables?.forEach {
            sv.visitFormalTypeParameter(it.name)
            it.classBound?.accept(sv.visitClassBound())
            it.interfaceBounds?.forEach { it.accept(sv.visitInterfaceBound()) }
        }
    }
}

class ClassSignatureNode : NodeWithTypeVariables() {

    var superClass: TypeSignatureNode? = null
    var interfaces: List<TypeSignatureNode>? = null

    override fun visitSuperclass(): SignatureVisitor {
        return TypeSignatureNodeBuilder {
            superClass = it
        }
    }

    override fun visitInterface(): SignatureVisitor {
        return TypeSignatureNodeBuilder {
            interfaces = (interfaces ?: emptyList()) + it
        }
    }
}

class TypeVariable(val name: String) {
    var classBound: TypeSignatureNode? = null
    var interfaceBounds: List<TypeSignatureNode>? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TypeVariable

        if (name != other.name) return false
        if (classBound != other.classBound) return false
        if (interfaceBounds != other.interfaceBounds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (classBound?.hashCode() ?: 0)
        result = 31 * result + (interfaceBounds?.hashCode() ?: 0)
        return result
    }
}


class MethodSignatureNode : NodeWithTypeVariables() {

    var returnType: TypeSignatureNode? = null
    var parameterTypes: List<TypeSignatureNode>? = null
    var exceptionTypes: List<TypeSignatureNode>? = null

    override fun visitParameterType(): SignatureVisitor {
        return TypeSignatureNodeBuilder {
            parameterTypes = (parameterTypes ?: emptyList()) + it
        }
    }

    override fun visitExceptionType(): SignatureVisitor {
        return TypeSignatureNodeBuilder {
            exceptionTypes = (exceptionTypes ?: emptyList()) + it
        }
    }

    override fun visitReturnType(): SignatureVisitor {
        return TypeSignatureNodeBuilder {
            returnType = it
        }
    }

    override fun accept(sv: SignatureVisitor?) {
        super.accept(sv)
        sv ?: return
        parameterTypes?.forEach { it.accept(sv.visitParameterType()) }
        returnType?.accept(sv.visitReturnType())
        exceptionTypes?.forEach { it.accept(sv.visitExceptionType()) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MethodSignatureNode

        if (returnType != other.returnType) return false
        if (parameterTypes != other.parameterTypes) return false
        if (exceptionTypes != other.exceptionTypes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = returnType?.hashCode() ?: 0
        result = 31 * result + (parameterTypes?.hashCode() ?: 0)
        result = 31 * result + (exceptionTypes?.hashCode() ?: 0)
        return result
    }


}


sealed class TypeArgument {
    object Unbounded : TypeArgument()
    sealed class TypeArgumentWithVariance() : TypeArgument() {
        abstract val node: TypeSignatureNode
        abstract fun copy(node: TypeSignatureNode = this.node): TypeArgumentWithVariance
        data class Extends(override val node: TypeSignatureNode) : TypeArgumentWithVariance()
        data class Super(override val node: TypeSignatureNode) : TypeArgumentWithVariance()
        data class Invariant(override val node: TypeSignatureNode) : TypeArgumentWithVariance()
    }

    fun accept(sv: SignatureVisitor?) {
        sv ?: return
        when (this) {
            Unbounded -> sv.visitTypeArgument()
            is Extends -> node.accept(sv.visitTypeArgument(SignatureVisitor.EXTENDS))
            is Super -> node.accept(sv.visitTypeArgument(SignatureVisitor.SUPER))
            is Invariant -> node.accept(sv.visitTypeArgument(SignatureVisitor.INSTANCEOF))
        }
    }
}

sealed class TypeSignatureNode {
    data class ArrayType(val type: TypeSignatureNode) : TypeSignatureNode()
    class Primitive private constructor(val descriptor: Char) : TypeSignatureNode() {
        companion object {
            private val descriptors = listOf('V', 'Z', 'C', 'B', 'S', 'I', 'F', 'J', 'D')
            val primitiveTypes = descriptors.map { Primitive(it) }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Primitive

            if (descriptor != other.descriptor) return false

            return true
        }

        override fun hashCode(): Int {
            return descriptor.hashCode()
        }
    }

    data class TypeVariable(val name: String) : TypeSignatureNode()

    data class ClassType(
        val classTypeName: String,
        val classArgs: List<TypeArgument>?,
        val innerName: String?,
        val innerArgs: List<TypeArgument>?
    ) : TypeSignatureNode()

    companion object {
        val primitiveTypeByDescriptor = Primitive.primitiveTypes.associateBy { it.descriptor }
    }

    fun accept(sv: SignatureVisitor?) {
        sv ?: return

        when (this) {
            is Primitive -> sv.visitBaseType(descriptor)
            is ArrayType -> type.accept(sv.visitArrayType())
            is TypeVariable -> sv.visitTypeVariable(name)
            is ClassType -> {
                sv.visitClassType(classTypeName)
                classArgs?.forEach { it.accept(sv) }
                if (innerName != null) {
                    sv.visitInnerClassType(innerName)
                    innerArgs?.forEach { it.accept(sv) }
                }
                sv.visitEnd()
            }
        }
    }
}

private class TypeSignatureNodeBuilder(val out: (TypeSignatureNode) -> Unit) : SignatureVisitor(Opcodes.ASM6) {


    private var classTypeName: String? = null
    private var innerName: String? = null
    private var classArgs: List<TypeArgument>? = null
    private var innerArgs: List<TypeArgument>? = null


    override fun visitBaseType(descriptor: Char) {
        out(TypeSignatureNode.primitiveTypeByDescriptor[descriptor]!!)
    }

    override fun visitTypeVariable(name: String) {
        out(TypeSignatureNode.TypeVariable(name))
    }

    override fun visitArrayType(): SignatureVisitor {
        return TypeSignatureNodeBuilder {
            out(TypeSignatureNode.ArrayType(it))
        }
    }

    override fun visitClassType(name: String?) {
        classTypeName = name
        super.visitClassType(name)
    }


    private fun addArgument(argument: TypeArgument) {
        if (innerName != null) {
            val list = innerArgs ?: emptyList()
            innerArgs = list + argument
        } else if (classTypeName != null) {
            val list = classArgs ?: emptyList()
            classArgs = list + argument
        }
    }


    override fun visitTypeArgument() {
        addArgument(TypeArgument.Unbounded)
        super.visitTypeArgument()
    }

    override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
        return TypeSignatureNodeBuilder { node ->
            val arg = when (wildcard) {
                SignatureVisitor.EXTENDS -> Extends(node)
                SignatureVisitor.SUPER -> Super(node)
                SignatureVisitor.INSTANCEOF -> Invariant(node)
                else -> error("Unknown wildcard")
            }
            addArgument(arg)
        }
    }

    override fun visitInnerClassType(name: String?) {
        innerName = name
        super.visitInnerClassType(name)
    }

    override fun visitEnd() {
        out(TypeSignatureNode.ClassType(classTypeName!!, classArgs, innerName, innerArgs))
        super.visitEnd()
    }
}

private class TypeVariableRemappingContext(val renameVector: Map<String, String>) {
    fun TypeArgument.remapTypeVariables(): TypeArgument? {
        return when (this) {
            is TypeArgument.TypeArgumentWithVariance -> {
                return node.remapTypeVariables()?.let { copy(it) }
            }
            else -> null
        }
    }

    fun TypeSignatureNode.remapTypeVariables(): TypeSignatureNode? {
        when (this) {
            is TypeSignatureNode.TypeVariable -> {
                return TypeSignatureNode.TypeVariable(renameVector[name]!!)
            }
            is TypeSignatureNode.ClassType -> {
                val newClassArgs = classArgs.remapArguments()
                val newInnerArgs = innerArgs.remapArguments()
                if (newClassArgs != classArgs || newInnerArgs != innerArgs) {
                    return copy(classArgs = newClassArgs, innerArgs = newInnerArgs)
                }
                return null
            }
            is TypeSignatureNode.ArrayType -> {
                return type.remapTypeVariables()?.let { copy(it) }
            }
            else -> return null
        }
    }

    fun List<TypeSignatureNode>?.remapFully() = this?.map { it.remapOrOld() }
    fun List<TypeArgument>?.remapArguments() = this?.map { it.remapTypeVariables() ?: it }
    fun <T: TypeSignatureNode?> T.remapOrOld(): T = (this?.remapTypeVariables() ?: this) as T
}

fun MethodSignatureNode.sanitizeTypeVariables(): MethodSignatureNode {
    val renameVector = mutableMapOf<String, String>()
    typeVariables?.forEachIndexed { index, typeVariable ->
        val newName = "TP$index"
        renameVector[typeVariable.name] = newName
    }

    return with(TypeVariableRemappingContext(renameVector)) {
        val newNode = MethodSignatureNode()
        newNode.typeVariables = typeVariables?.map { oldVariable ->
            TypeVariable(renameVector[oldVariable.name]!!).also {
                it.classBound = oldVariable.classBound.remapOrOld()
                it.interfaceBounds = oldVariable.interfaceBounds.remapFully()
            }
        }
        newNode.returnType = returnType.remapOrOld()
        newNode.parameterTypes = parameterTypes.remapFully()
        newNode.exceptionTypes = exceptionTypes.remapFully()

        newNode
    }
}