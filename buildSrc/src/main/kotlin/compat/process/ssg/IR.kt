package compat.process.ssg

import compat.process.Version
import compat.process.formatForReport
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type

class SSGClass(
        var access: Int,
        var fqName: String,
        var superType: String?,
        var interfaces: Array<String>?,
        var version: Version?
) : SSGNode<SSGClass> {

    val methodsBySignature = mutableMapOf<String, SSGMethod>()
    val fieldsBySignature = mutableMapOf<String, SSGField>()

    fun addField(node: SSGField) {
        assert(node.fqd() !in fieldsBySignature)

        fieldsBySignature[node.fqd()] = node
    }

    fun addMethod(node: SSGMethod) {
        check(node.fqd() !in methodsBySignature)
        methodsBySignature[node.fqd()] = node
    }

    override fun toString(): String {
        return buildString {
            append(version.forDisplay())

            appendVisibility(access)

            if (access hasFlag ACC_FINAL) {
                append("final ")
            }
            if (access hasFlag ACC_ENUM) {
                append("enum ")
            }
            if (access hasFlag ACC_ANNOTATION) {
                append("annotation ")
            }

            if (access hasFlag ACC_INTERFACE) {
                append("interface ")
            } else if (access hasFlag ACC_ABSTRACT) {
                append("abstract ")
            }


            appendln("class $fqName {")
            methodsBySignature.values.joinTo(this, separator = "\n\t", prefix = "\t")
            fieldsBySignature.values.joinTo(this, separator = "\n\t", prefix = "\n\t")
            appendln()
            appendln("}")
        }
    }
}

class SSGField(
        var access: Int,
        var name: String,
        var desc: String,
        var signature: String?,
        var value: Any?,
        var version: Version?
) : SSGNode<SSGField> {

    fun fqd(): String = name + desc


    override fun toString(): String {
        return buildString {
            append(version.forDisplay())

            appendVisibility(access)

            if (access hasFlag ACC_STATIC) {
                append("static ")
            }

            if (access hasFlag ACC_FINAL) {
                append("var")
            } else {
                append("val")
            }

            append(" $name: ")
            append(Type.getType(desc).formatForReport())
        }
    }
}

private fun Version?.forDisplay(): String {
    if (this == null) return ""
    return "@ExistsIn(${this.asLiteralValue()}) "
}

class SSGMethod(
        var access: Int,
        var name: String,
        var desc: String,
        var signature: String?,
        var exceptions: Array<String>?,
        var version: Version?
) : SSGNode<SSGMethod> {


    fun fqd(): String {
        return name + desc
    }

    override fun toString(): String {
        return buildString {
            append(version.forDisplay())

            appendVisibility(access)

            if (!(access hasFlag ACC_FINAL)) {
                append("open ")
            }
            if (access hasFlag ACC_STATIC) {
                append("static ")
            }

            append("fun ")
            append(name)

            append(Type.getMethodType(desc).formatForReport())
        }
    }
}

interface SSGNode<T : SSGNode<T>> {
}