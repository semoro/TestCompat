package org.jetbrains.kotlin.tools.kompot.commons

import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureVisitor


abstract class NodeWithTypeVariablesBuilder : SignatureVisitor(Opcodes.ASM6) {
    var typeVariables: List<TypeVariable> = emptyList()
    //private set

    override fun visitFormalTypeParameter(name: String) {
        this.typeVariables = this.typeVariables + TypeVariable(name)
        super.visitFormalTypeParameter(name)
    }

    override fun visitClassBound(): SignatureVisitor {
        return TypeSignatureNodeBuilder {
            typeVariables.last().classBound = it
        }
    }

    override fun visitInterfaceBound(): SignatureVisitor {
        return TypeSignatureNodeBuilder {
            val variable = typeVariables.last()
            variable.interfaceBounds += it
        }
    }
}

class ClassSignatureNodeBuilder : NodeWithTypeVariablesBuilder() {

    var superClass: TypeSignatureNode.ClassType? = null
        private set
    var interfaces: List<TypeSignatureNode.ClassType> = emptyList()
        private set


    override fun visitSuperclass(): SignatureVisitor {
        return TypeSignatureNodeBuilder {
            superClass = it as TypeSignatureNode.ClassType
        }
    }

    override fun visitInterface(): SignatureVisitor {
        return TypeSignatureNodeBuilder {
            interfaces += it as TypeSignatureNode.ClassType
        }
    }


    val node get() = ClassSignatureNode(typeVariables, superClass, interfaces)
}


abstract class NodeWithTypeVariables {
    abstract val typeVariables: List<TypeVariable>

    open fun accept(sv: SignatureVisitor) {
        typeVariables.forEach {
            sv.visitFormalTypeParameter(it.name)
            it.classBound?.accept(sv.visitClassBound())
            it.interfaceBounds.forEach { it.accept(sv.visitInterfaceBound()) }
        }
    }
}

data class ClassSignatureNode(
    override val typeVariables: List<TypeVariable>,
    val superClass: TypeSignatureNode.ClassType?,
    val interfaces: List<TypeSignatureNode.ClassType>
) : NodeWithTypeVariables() {

    override fun accept(sv: SignatureVisitor) {
        super.accept(sv)
        superClass?.accept(sv.visitSuperclass())
        interfaces.forEach { it.accept(sv.visitInterface()) }
    }
}

data class MethodSignatureNode(
    override val typeVariables: List<TypeVariable>,
    val parameterTypes: List<TypeSignatureNode>,
    val returnType: TypeSignatureNode?,
    val exceptionTypes: List<TypeSignatureNode>
) : NodeWithTypeVariables() {
    override fun accept(sv: SignatureVisitor) {
        super.accept(sv)
        parameterTypes.forEach { it.accept(sv.visitParameterType()) }
        returnType?.accept(sv.visitReturnType())
        exceptionTypes.forEach { it.accept(sv.visitExceptionType()) }
    }
}


data class TypeVariable(
    val name: String,
    var classBound: TypeSignatureNode? = null,
    var interfaceBounds: List<TypeSignatureNode> = emptyList()
)


class MethodSignatureNodeBuilder : NodeWithTypeVariablesBuilder() {

    var returnType: TypeSignatureNode? = null
    var parameterTypes: List<TypeSignatureNode> = emptyList()
    var exceptionTypes: List<TypeSignatureNode> = emptyList()

    override fun visitParameterType(): SignatureVisitor {
        return TypeSignatureNodeBuilder {
            parameterTypes += it
        }
    }

    override fun visitExceptionType(): SignatureVisitor {
        return TypeSignatureNodeBuilder {
            exceptionTypes += it
        }
    }

    override fun visitReturnType(): SignatureVisitor {
        return TypeSignatureNodeBuilder {
            returnType = it
        }
    }


    val node get() = MethodSignatureNode(typeVariables, parameterTypes, returnType, exceptionTypes)
}


sealed class TypeArgument {
    object Unbounded : TypeArgument()
    data class Bounded(val variance: Variance, val node: TypeSignatureNode) : TypeArgument() {

        val isInvariant = variance == Variance.INVARIANT
    }

    enum class Variance(val descriptor: Char) {
        INVARIANT(SignatureVisitor.INSTANCEOF),
        SUPER(SignatureVisitor.SUPER),
        EXTENDS(SignatureVisitor.EXTENDS)
    }

    fun accept(sv: SignatureVisitor) {
        when (this) {
            Unbounded -> sv.visitTypeArgument()
            is Bounded -> node.accept(sv.visitTypeArgument(variance.descriptor))
        }
    }
}


sealed class TypeSignatureNode {
    data class ArrayType(val type: TypeSignatureNode) : TypeSignatureNode()
    data class Primitive private constructor(val descriptor: Char) : TypeSignatureNode() {
        companion object {
            private val descriptors = listOf('V', 'Z', 'C', 'B', 'S', 'I', 'F', 'J', 'D')
            val primitiveTypes = descriptors.map { Primitive(it) }
        }
    }

    data class TypeVariable(val name: String) : TypeSignatureNode()

    data class ClassType(
        val classTypeName: String,
        val classArgs: List<TypeArgument>,
        val inners: List<Pair<String, List<TypeArgument>>>
    ) : TypeSignatureNode()

    companion object {
        val primitiveTypeByDescriptor = Primitive.primitiveTypes.associateBy { it.descriptor }
    }

    fun accept(sv: SignatureVisitor) {
        when (this) {
            is Primitive -> sv.visitBaseType(descriptor)
            is ArrayType -> type.accept(sv.visitArrayType())
            is TypeVariable -> sv.visitTypeVariable(name)
            is ClassType -> {
                sv.visitClassType(classTypeName)
                classArgs.forEach { it.accept(sv) }
                inners.forEach { (innerName, innerArgs) ->
                    sv.visitInnerClassType(innerName)
                    innerArgs.forEach { it.accept(sv) }
                }
                sv.visitEnd()
            }
        }
    }
}

private class TypeSignatureNodeBuilder(val out: (TypeSignatureNode) -> Unit) : SignatureVisitor(Opcodes.ASM6) {


    private var classTypeName: String? = null
    private var innerNames: List<String> = emptyList()
    private var allInnerArgs: List<List<TypeArgument>> = emptyList()
    private var classArgs: List<TypeArgument> = emptyList()
    private var innerArgs: List<TypeArgument> = emptyList()


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
    }


    private fun addArgument(argument: TypeArgument) {
        if (innerNames.isNotEmpty()) {
            innerArgs += argument
        } else if (classTypeName != null) {
            val list = classArgs
            classArgs = list + argument
        }
    }


    override fun visitTypeArgument() {
        addArgument(TypeArgument.Unbounded)
    }

    override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
        return TypeSignatureNodeBuilder { node ->
            val variance = when (wildcard) {
                SignatureVisitor.EXTENDS -> TypeArgument.Variance.EXTENDS
                SignatureVisitor.SUPER -> TypeArgument.Variance.SUPER
                SignatureVisitor.INSTANCEOF -> TypeArgument.Variance.INVARIANT
                else -> error("Unknown wildcard")
            }


            addArgument(TypeArgument.Bounded(variance, node))
        }
    }

    override fun visitInnerClassType(name: String) {
        beginInnerType()
        innerNames += name
    }

    override fun visitEnd() {
        beginInnerType()
        out(TypeSignatureNode.ClassType(classTypeName!!, classArgs, innerNames zip allInnerArgs))
    }

    private fun beginInnerType() {
        if (!innerNames.isEmpty()) {
            allInnerArgs += listOf(innerArgs)
            innerArgs = emptyList()
        }
    }
}

private class TypeVariableRemappingContext(val renameVector: Map<String, String>) {
    fun TypeArgument.remapTypeVariables(): TypeArgument? {
        return when (this) {
            is TypeArgument.Bounded -> node.remapTypeVariables()?.let { copy(node = it) }
            else -> null
        }
    }

    fun TypeSignatureNode.remapTypeVariables(): TypeSignatureNode? {
        when (this) {
            is TypeSignatureNode.TypeVariable -> {
                return renameVector[name]?.let { TypeSignatureNode.TypeVariable(it) }
            }
            is TypeSignatureNode.ClassType -> {
                val newClassArgs = classArgs.remapArguments()
                val (innerNames, innerArgs) = inners.unzip()

                val newInnerArgs = innerArgs.map { it.remapArguments() }
                if (newClassArgs != classArgs || newInnerArgs != innerArgs) {
                    return copy(classArgs = newClassArgs, inners = innerNames zip newInnerArgs)
                }
                return null
            }
            is TypeSignatureNode.ArrayType -> {
                return type.remapTypeVariables()?.let { copy(it) }
            }
            else -> return null
        }
    }

    fun <T : TypeSignatureNode> List<T>.remapFully() = this.map { it.remapOrOld() }
    fun List<TypeArgument>.remapArguments() = this.map { it.remapTypeVariables() ?: it }
    fun <T : TypeSignatureNode?> T.remapOrOld(): T = (this?.remapTypeVariables() ?: this) as T
}

fun MethodSignatureNode.sanitizeTypeVariables(): Pair<MethodSignatureNode, Map<String, String>> {
    if (typeVariables.isEmpty()) return this to emptyMap()

    val renameVector = mutableMapOf<String, String>()
    typeVariables.forEachIndexed { index, typeVariable ->
        val newName = "TP$index"
        renameVector[typeVariable.name] = newName
    }

    return renameTypeVariables(renameVector) to renameVector
}

fun MethodSignatureNode.renameTypeVariables(renameVector: Map<String, String>): MethodSignatureNode {
    return with(TypeVariableRemappingContext(renameVector)) {
        MethodSignatureNode(
            typeVariables.map { oldVariable ->
                TypeVariable(renameVector[oldVariable.name]!!).also {
                    it.classBound = oldVariable.classBound.remapOrOld()
                    it.interfaceBounds = oldVariable.interfaceBounds.remapFully()
                }
            },
            parameterTypes.remapFully(),
            returnType.remapOrOld(),
            exceptionTypes.remapFully()
        )
    }
}