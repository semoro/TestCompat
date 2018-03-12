package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.annotations.Modality
import org.jetbrains.kotlin.tools.kompot.api.annotations.Visibility
import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.commons.getOrInit
import org.objectweb.asm.tree.AnnotationNode

//
//class SSGGraph {
//    val classes: List<SSGClass> = emptyList()
//    val groupedClasses: List<GroupedClass>
//        get() =
//            classes.groupBy { it.fqName }.map { (fqName, classes) ->
//                GroupedClass(fqName, classes)
//            }
//
//
//}

class SSGClass(
    override var access: Int,
    override var fqName: String,
    override var signature: String?,
    override var superType: String?,
    interfaces: Array<String>?,
    override var version: Version
) : SSGNode<SSGClass>,
    SSGClassFacade,
    SSGAlternativeVisibilityContainer,
    SSGAlternativeModalityContainer,
    SSGAccess,
    SSGAnnotated {

    override var interfaces: List<String> = interfaces?.toList() ?: emptyList()

    override var annotations: List<AnnotationNode>? = null

    var isKotlin = false

    override var alternativeVisibilityState: MutableMap<Visibility, Version>? = null
    override var alternativeModalityState: MutableMap<Modality, Version>? = null

    var innerClassesBySignature: Map<String, SSGInnerClassRef> = emptyMap()
    var ownerInfo: OuterClassInfo? = null

    val methodsBySignature = mutableMapOf<String, SSGMethodOrGroup>()
    val fieldsBySignature = mutableMapOf<String, SSGField>()

    val isMemberClass: Boolean
        get() = ownerInfo != null

    fun addField(node: SSGField) {
        assert(node.fqd() !in fieldsBySignature)
        fieldsBySignature[node.fqd()] = node
    }

    fun addMethod(node: SSGMethodOrGroup) {
        check(node.fqd() !in methodsBySignature)
        methodsBySignature[node.fqd()] = node
    }
}

class SSGField(
    override var access: Int,
    var name: String,
    var desc: String,
    var signature: String?,
    var value: Any?,
    override var version: Version
) : SSGNode<SSGField>,
    SSGVersionContainer,
    SSGAlternativeVisibilityContainer,
    SSGAccess,
    SSGNullabilityContainer,
    SSGAnnotated {

    override var annotations: List<AnnotationNode>? = null
    override var nullability: Nullability = Nullability.DEFAULT

    override var alternativeVisibilityState: MutableMap<Visibility, Version>? = null

    fun fqd(): String = name + desc
}


interface SSGMethodOrGroup {
    fun fqd(): String
    val methods: List<SSGMethod>
}

class SSGMethodGroup(override val methods: List<SSGMethod> = listOf()) : SSGNode<SSGMethodGroup>, SSGMethodOrGroup {
    override fun fqd(): String {
        return methods.first().fqd()
    }
}

val SSGMethodOrGroup.isConstructor get() = methods.first().name == "<init>"

class SSGMethod(
    override var access: Int,
    var name: String,
    var desc: String,
    var signature: String?,
    var exceptions: Array<String>?,
    override var version: Version
) : SSGNode<SSGMethod>,
    SSGVersionContainer,
    SSGAlternativeVisibilityContainer,
    SSGAlternativeModalityContainer,
    SSGAccess,
    SSGNullabilityContainer,
    SSGAnnotated,
    SSGMethodOrGroup {

    override val methods get() = listOf(this)

    var parameterInfoArray = arrayOfNulls<SSGParameterInfo>(0)

    override var annotations: List<AnnotationNode>? = null
    override var nullability: Nullability = Nullability.DEFAULT

    override var alternativeModalityState: MutableMap<Modality, Version>? = null
    override var alternativeVisibilityState: MutableMap<Visibility, Version>? = null

    var annotationDefaultValue: AnnotationNode? = null

    override fun fqd(): String {
        return name + desc
    }
}

class SSGParameterInfo(
    val number: Int
) : SSGAnnotated, SSGNullabilityContainer {
    var access: Int = 0
    var name: String? = null

    override var annotations: List<AnnotationNode>? = null
    override var nullability: Nullability = Nullability.DEFAULT
}

interface SSGNode<T : SSGNode<T>>

interface SSGVersionContainer {
    val version: Version
}

interface SSGAlternativeVisibilityContainer : SSGAccess, SSGVersionContainer {
    var alternativeVisibilityState: MutableMap<Visibility, Version>?

    val alternativeVisibility
        get() = ::alternativeVisibilityState.getOrInit { mutableMapOf() }
}

interface SSGAlternativeModalityContainer : SSGAccess, SSGVersionContainer {
    var alternativeModalityState: MutableMap<Modality, Version>?

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

interface SSGSignature {
    val signature: String?
}

interface SSGClassFacade: SSGSignature, SSGVersionContainer {
    val superType: String?
    val fqName: String
    val interfaces: List<String>
}

interface SSGNullabilityContainer {
    var nullability: Nullability
}

data class OuterClassInfo(var owner: String, var methodName: String?, var methodDesc: String?)

data class SSGInnerClassRef(
    override var access: Int,
    var name: String,
    var outerName: String?,
    var innerName: String?
) : SSGAccess
