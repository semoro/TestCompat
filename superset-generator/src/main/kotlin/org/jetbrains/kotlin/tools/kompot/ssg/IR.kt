package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.annotations.Modality
import org.jetbrains.kotlin.tools.kompot.api.annotations.Visibility
import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.commons.formatForReport
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type

class SSGClass(
    override var access: Int,
    var fqName: String,
    var signature: String?,
    var superType: String?,
    var interfaces: Array<String>?,
    override var version: Version?
) : SSGNode<SSGClass>,
    SSGVersionContainer,
    SSGAlternativeVisibilityContainer,
    SSGAlternativeModalityContainer,
    SSGAccess {

    var isKotlin = false

    override var alternativeVisibility: MutableMap<Visibility, Version?>? = null
    override var alternativeModalityState: MutableMap<Modality, Version?>? = null

    var innerClassesBySignature: MutableMap<String, SSGInnerClassRef>? = null
    var ownerInfo: OuterClassInfo? = null

    val methodsBySignature = mutableMapOf<String, SSGMethod>()
    val fieldsBySignature = mutableMapOf<String, SSGField>()

    val isMemberClass: Boolean
        get() = ownerInfo != null

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

            append(access.presentableVisibility + " ")

            if (access hasFlag ACC_FINAL) {
                append("final ")
            }

            appendln(access.presentableKind + " $fqName {")

            if (methodsBySignature.isNotEmpty()) {
                methodsBySignature.values.joinTo(this, separator = "\n\t", prefix = "\t", postfix = "\n")
            }

            if (fieldsBySignature.isNotEmpty()) {
                fieldsBySignature.values.joinTo(this, separator = "\n\t", prefix = "\t", postfix = "\n")
            }
            appendln("}")
        }
    }
}

class SSGField(
    override var access: Int,
    var name: String,
    var desc: String,
    var signature: String?,
    var value: Any?,
    override var version: Version?
) : SSGNode<SSGField>,
    SSGVersionContainer,
    SSGAlternativeVisibilityContainer,
    SSGAccess {

    override var alternativeVisibility: MutableMap<Visibility, Version?>? = null

    fun fqd(): String = name + desc


    override fun toString(): String {
        return buildString {
            append(version.forDisplay())

            append(access.presentableVisibility + " ")

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
    override var access: Int,
    var name: String,
    var desc: String,
    var signature: String?,
    var exceptions: Array<String>?,
    override var version: Version?
) : SSGNode<SSGMethod>,
    SSGVersionContainer,
    SSGAlternativeVisibilityContainer,
    SSGAlternativeModalityContainer,
    SSGAccess {

    override var alternativeModalityState: MutableMap<Modality, Version?>? = null
    override var alternativeVisibility: MutableMap<Visibility, Version?>? = null

    fun fqd(): String {
        return name + desc
    }

    override fun toString(): String {
        return buildString {
            append(version.forDisplay())

            append(access.presentableVisibility + " ")
            if (access hasFlag ACC_ABSTRACT) {
                append("abstract ")
            } else if (!(access hasFlag ACC_FINAL)) {
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

interface SSGNode<T : SSGNode<T>>

interface SSGVersionContainer {
    var version: Version?
}

interface SSGAlternativeVisibilityContainer : SSGAccess, SSGVersionContainer {
    var alternativeVisibility: MutableMap<Visibility, Version?>?

    fun alternativeVisibility() = (alternativeVisibility ?: mutableMapOf()).also { alternativeVisibility = it }
}

interface SSGAlternativeModalityContainer : SSGAccess, SSGVersionContainer {
    var alternativeModalityState: MutableMap<Modality, Version?>?

    val alternativeModality
        get() = (alternativeModalityState ?: mutableMapOf()).also { alternativeModalityState = it }
}

interface SSGAccess {
    var access: Int
}

data class OuterClassInfo(var owner: String, var methodName: String?, var methodDesc: String?)

data class SSGInnerClassRef(
    override var access: Int,
    var name: String,
    var outerName: String?,
    var innerName: String?
) : SSGAccess