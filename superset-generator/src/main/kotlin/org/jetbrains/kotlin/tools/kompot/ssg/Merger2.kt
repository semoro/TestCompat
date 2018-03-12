package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.commons.*

data class GroupingContext(val a: GroupedClass, val b: SSGClass)

data class TypeSignatureMergingContext(
    val groupingContext: GroupingContext,
    val eraseToUnbounded: Boolean,
    val fullErasure: Boolean
) {
    val a get() = groupingContext.a
    val b get() = groupingContext.b
}

class GroupedClass(
    val classes: List<SSGClass>,
    override val signature: String?
) : SSGClassFacade {

    constructor(c: SSGClass) : this(listOf(c), c.signature)

    override val fqName get() = classes.first().fqName
    val ownerInfo get() = classes.first().ownerInfo
    override val version = classes.map { it.version }.reduce { acc, version -> acc + version }
    override val superType: String? = classes.map { it.superType }.distinct().singleOrNull()

    override val interfaces: List<String> = classes.map { it.interfaces }.flatten().distinct()

    override fun toString(): String {
        return "GroupedClass(classes=$classes, signature=$signature, version=$version, superType=$superType)"
    }

    override fun equals(other: Any?): Boolean {
        return classes == (other as? GroupedClass)?.classes
    }

    override fun hashCode(): Int {
        return classes.hashCode()
    }
}

val TypeSignatureNode.ClassType.qualifiedName get() = classTypeName + inners.joinToString(separator = "$") { it.first }

class GroupedClassMerger(val signatureLoader: SignatureLoader) : SignatureLoader by signatureLoader {

    fun String?.lookupClass(c: SSGClassFacade): Pair<String?, GroupedClass?> {
        return this to this?.let { c.lookupClassGroup(it) }
    }


    fun merge(a: TypeArgument, b: TypeArgument, ctx: TypeSignatureMergingContext): TypeArgument? {
        return when {
            a === TypeArgument.Unbounded && b === TypeArgument.Unbounded -> a
            a is TypeArgument.Bounded && b is TypeArgument.Bounded && a.variance == b.variance -> {
                a.copy(node = merge(a.node, b.node, ctx) ?: return null)
            }
            else -> null
        }
    }

    fun merge(
        a: TypeSignatureNode,
        b: TypeSignatureNode,
        ctx: TypeSignatureMergingContext
    ): TypeSignatureNode? {
        return if (a::class == b::class) {
            when {
                a is TypeSignatureNode.ClassType && b is TypeSignatureNode.ClassType -> {
                    merge(a, b, ctx)
                }
                a is TypeSignatureNode.ArrayType && b is TypeSignatureNode.ArrayType -> {
                    a.copy(
                        type = merge(
                            a.type,
                            b.type,
                            ctx.copy(eraseToUnbounded = false, fullErasure = false)
                        ) ?: return null
                    )
                }
                a is TypeSignatureNode.Primitive && b is TypeSignatureNode.Primitive -> {
                    if (a.descriptor != b.descriptor) return null
                    a
                }
                a is TypeSignatureNode.TypeVariable && b is TypeSignatureNode.TypeVariable -> {
                    if (a.name != b.name) return null
                    a
                }
                else -> null
            }
        } else {
            null
        }
    }

    fun merge(
        a: TypeSignatureNode.ClassType,
        b: TypeSignatureNode.ClassType,
        ctx: TypeSignatureMergingContext
    ): TypeSignatureNode.ClassType? {
        if (a.qualifiedName.lookupClass(ctx.a) != b.qualifiedName.lookupClass(ctx.b)) return null
        val classArgs = a.classArgs.zip(b.classArgs).map { (a, b) ->
            merge(a, b, ctx) ?: return null
        }
        val inners = a.inners.zip(b.inners).map { (a, b) ->
            val args = a.second.zip(b.second).map { (a, b) -> merge(a, b, ctx) ?: return null }
            a.first to args
        }

        return TypeSignatureNode.ClassType(a.classTypeName, classArgs, inners)
    }

    fun merge(a: TypeVariable, b: TypeVariable, ctx: TypeSignatureMergingContext): TypeVariable? {
        val bClassBound = b.classBound
        val aClassBound = a.classBound
        val classBound = when {
            aClassBound != null && bClassBound != null -> merge(aClassBound, bClassBound, ctx) ?: return null
            aClassBound == null && bClassBound == null -> null
            else -> return null
        }

        if (a.interfaceBounds.size != b.interfaceBounds.size) return null
        val interfaceBounds = a.interfaceBounds.zip(b.interfaceBounds).map { (a, b) ->
            merge(a, b, ctx) ?: return null
        }
        return a.copy(classBound = classBound, interfaceBounds = interfaceBounds)
    }

    fun merge(a: ClassSignatureNode, b: ClassSignatureNode, ctx: GroupingContext): ClassSignatureNode? {

        val context = TypeSignatureMergingContext(ctx, false, false)

        if (a.typeVariables.size != b.typeVariables.size) return null

        val typeVariables = (a.typeVariables + b.typeVariables).groupBy { it.name }.map { (_, value) ->
            if (value.size != 2) return null
            val (a, b) = value
            merge(a, b, context) ?: return null
        }

        val aInterfaces = a.interfaces.associateBy { it.qualifiedName.lookupClass(ctx.a) }
        val bInterfaces = b.interfaces.associateBy { it.qualifiedName.lookupClass(ctx.b) }

        val aSuperClass = a.superClass
        val bSuperClass = b.superClass

        val superType: TypeSignatureNode.ClassType? =
            if (aSuperClass != null && bSuperClass != null) {
                merge(aSuperClass, bSuperClass, TypeSignatureMergingContext(ctx, false, false)) ?: return null
            } else null

        val allInterfaces = aInterfaces.mergeToMultiMapWith(bInterfaces).map {
            it.value.singleOrNull() ?: run {
                val (a, b) = it.value
                merge(a, b, TypeSignatureMergingContext(ctx, false, false)) ?: return null
            }
        }

        return ClassSignatureNode().apply {
            this.interfaces = allInterfaces
            this.superClass = superType
            this.typeVariables = typeVariables
        }
    }

    fun merge(a: GroupedClass, b: SSGClass, ctx: GroupingContext): GroupedClass? {
        if (a.ownerInfo != b.ownerInfo) return null
        if (a.ownerInfo?.owner.lookupClass(a) != b.ownerInfo?.owner.lookupClass(b)) return null
        if (a.exactSuperType != b.exactSuperType) return null

        val signature: String? = if (a.signature != b.signature) {
            merge(a.loadedSignature, b.loadedSignature, ctx)?.toSignature() ?: return null
        } else a.signature


        return GroupedClass(a.classes + b, signature)
    }


    val lookupCache: MutableMap<String, List<String>> = mutableMapOf()
    val lookup: MutableMap<String, List<GroupedClass>> = mutableMapOf()

    fun SSGClassFacade.lookupClassGroup(fqName: String): GroupedClass? {
        lookupCache.merge(fqName, mutableListOf(this.fqName)) { a, b -> a + b }
        return version.lookupGroupedClass(fqName)
    }

    fun Version.lookupGroupedClass(fqName: String): GroupedClass? {
        return lookup[fqName]?.find { this in it.version } ?: return null
    }


    val SSGClassFacade.exactSuperType get() = superType?.lookupClass(this)


    // assume all of c has same fqName
    fun group(c: List<SSGClass>) {

        val groups = mutableListOf<GroupedClass>()

        outer@ for (clz in c) {
            for ((index, group) in groups.withIndex()) {
                groups[index] = merge(group, clz, GroupingContext(group, clz)) ?: continue
                continue@outer
            }
            groups += GroupedClass(clz)
        }

        val fqName = groups.first().fqName

        recordGroup(fqName, groups)
    }

    fun recordGroup(fqName: String, groups: List<GroupedClass>) {

        if (lookup[fqName]?.toSet() != groups.toSet()) {
            lookup[fqName] = groups
            regroupDependents(fqName)
        }
    }

    fun regroupDependents(fqName: String) {
        lookupCache[fqName]
            .orEmpty()
            .mapNotNull { lookup[it] }
            .forEach {
                group(it.map { it.classes }.flatten())
            }
    }

    fun prepareRewrite(): Rewriter {
        val rewriteMap = mutableMapOf<GroupedClass, String>()

        lookup.values.forEach {
            if (it.size > 1) {
                it.forEachIndexed { index, groupedClass ->
                    rewriteMap[groupedClass] = groupedClass.fqName + "V$index"
                }
            }
        }

        return Rewriter(rewriteMap, this)
    }

    fun rewriteAll(r: Rewriter) {

        lookup.values.forEach {
            it.forEach {
                with(r) {
                    it.rewrite()
                }
            }
        }
    }
}
