package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.annotations.Modality
import org.jetbrains.kotlin.tools.kompot.api.annotations.Visibility
import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.commons.formatForReport
import org.jetbrains.kotlin.tools.kompot.commons.getOrInit
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode

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
    SSGAccess,
    SSGAnnotated {

    override var annotations: List<AnnotationNode>? = null

    var isKotlin = false

    override var alternativeVisibilityState: MutableMap<Visibility, Version?>? = null
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
                methodsBySignature.values.joinTo(this, separator = "\n\t", prefix = "\t", postfix = "\n") { it.debugText() }
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

    override var alternativeVisibilityState: MutableMap<Visibility, Version?>? = null

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
    override var alternativeVisibilityState: MutableMap<Visibility, Version?>? = null

    fun fqd(): String {
        return name + desc
    }
}

interface SSGNode<T : SSGNode<T>>

interface SSGVersionContainer {
    var version: Version?
}

interface SSGAlternativeVisibilityContainer : SSGAccess, SSGVersionContainer {
    var alternativeVisibilityState: MutableMap<Visibility, Version?>?

    val alternativeVisibility
        get() = ::alternativeVisibilityState.getOrInit { mutableMapOf() }
}

interface SSGAlternativeModalityContainer : SSGAccess, SSGVersionContainer {
    var alternativeModalityState: MutableMap<Modality, Version?>?

    val alternativeModality
        get() = ::alternativeModalityState.getOrInit { mutableMapOf() }
}

interface SSGAccess {
    var access: Int
}

interface SSGAnnotated {
    var annotations: List<AnnotationNode>?

    fun addAnnotation(node: AnnotationNode) {
        annotations = ::annotations.getOrInit { listOf() } + node
    }
}

data class OuterClassInfo(var owner: String, var methodName: String?, var methodDesc: String?)

data class SSGInnerClassRef(
    override var access: Int,
    var name: String,
    var outerName: String?,
    var innerName: String?
) : SSGAccess