package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.annotations.Modality
import org.jetbrains.kotlin.tools.kompot.api.annotations.Visibility
import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionHandler
import org.jetbrains.kotlin.tools.kompot.commons.*
import org.jetbrains.kotlin.tools.kompot.commons.TypeArgument.Unbounded
import org.jetbrains.kotlin.tools.kompot.commons.TypeArgument.Variance.INVARIANT
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.classKindMismatch
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.differentAnnotationsWithSameDesc
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.differentInnersWithSameDesc
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.fieldModalityMismatch
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.genericsMismatch
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.methodKindMismatch
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.ownerClassMismatch
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.superTypeMismatch
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureWriter
import org.slf4j.Logger


fun Int.sameMasked(other: Int, mask: Int): Boolean = (this and mask) == (other and mask)

fun SSGAccess.sameVisibility(other: SSGAccess): Boolean = access.sameMasked(
    other.access,
    VISIBILITY_MASK
)

fun SSGAccess.sameModality(other: SSGAccess): Boolean = access.sameMasked(
    other.access,
    MODALITY_MASK
)

fun SSGAccess.sameKind(other: SSGAccess): Boolean = access.sameMasked(
    other.access,
    KIND_MASK
)


class SSGMerger(val logger: Logger, val versionHandler: VersionHandler) {

    val S = MergeScopes(logger)

    private fun appendField(to: SSGClass, sourceField: SSGField, source: SSGClass) {
        val fqd = sourceField.fqd()
        val targetField = to.fieldsBySignature[fqd] ?: return to.addField(sourceField)
        mergeFields(targetField, sourceField)
    }

    private fun mergeFields(targetField: SSGField, sourceField: SSGField) =
        tryMerge(S.fields, targetField, sourceField) {
            if (targetField.signature != sourceField.signature) {
                reportMergeFailure(genericsMismatch, "${targetField.signature} != ${sourceField.signature}")
            }

            if (targetField.modality != sourceField.modality) {
                reportMergeFailure(fieldModalityMismatch, "${targetField.modality} != ${sourceField.modality}")
            }

            if (targetField.modality == sourceField.modality) {
                mergeVisibilities(targetField, sourceField)
                mergeNullability(targetField, sourceField)
                mergeAnnotations(targetField, sourceField)

                targetField.version = targetField.version + sourceField.version
            }
        }

    private fun mergeMethodSignaturesWithErasure(
        targetMethod: SSGMethod,
        source: String?,
        allowDeepMerge: Boolean = false
    ) {

        val target = targetMethod.signature
        if (targetMethod.signature == source) return
        if (target == null || source == null) {
            targetMethod.signature = null
            return
        }

        val tr = SignatureReader(target)
        val sr = SignatureReader(source)

        val sourceSignature = MethodSignatureNode()
        sr.accept(sourceSignature)

        val targetSignatureNode = MethodSignatureNode()
        tr.accept(targetSignatureNode)

        val (sanitizedTargetNode, targetRenameVector) = targetSignatureNode.sanitizeTypeVariables()
        val (sanitizedSourceNode, sourceRenameVector) = sourceSignature.sanitizeTypeVariables()

        if (sanitizedTargetNode == sanitizedSourceNode) {
            return /* keep target */
        }

        if (!allowDeepMerge) reportMergeFailure(genericsMismatch, "non deep check: $target != $source")

        sanitizedTargetNode.returnType = mergeTypeSignatureNodes(
            sanitizedTargetNode.returnType!!,
            sanitizedSourceNode.returnType!!
        ) ?: reportMergeFailure(
            genericsMismatch,
            "Failed to merge return type: ${sanitizedTargetNode.returnType} U ${sanitizedSourceNode.returnType}"
        )

        sanitizedTargetNode.parameterTypes =
                sanitizedTargetNode.parameterTypes?.zip(sanitizedSourceNode.parameterTypes!!)
                    ?.map { (a, b) ->
                        mergeTypeSignatureNodes(a, b) ?: reportMergeFailure(
                            genericsMismatch,
                            "Failed to merge parameter type: $a U $b"
                        )
                    }

        sanitizedTargetNode.exceptionTypes =
                sanitizedTargetNode.exceptionTypes?.zip(sanitizedSourceNode.exceptionTypes!!)
                    ?.map { (a, b) ->
                        mergeTypeSignatureNodes(a, b) ?: reportMergeFailure(
                            genericsMismatch,
                            "Failed to merge exception type: $a U $b"
                        )
                    }

        val compositeRenameVector = sourceRenameVector + targetRenameVector

        sanitizedTargetNode.renameTypeVariables(compositeRenameVector.entries.associate { it.value to it.key })

        val signatureWriter = SignatureWriter()
        sanitizedTargetNode.accept(signatureWriter)
        targetMethod.signature = signatureWriter.toString()
    }


    private fun mergeTypeSignatureNodes(a: TypeSignatureNode, b: TypeSignatureNode): TypeSignatureNode? {
        fun mergeArguments(arguments: Pair<TypeArgument, TypeArgument>): TypeArgument {
            val (a, b) = arguments

            return when {
                a == b -> a
                a === Unbounded -> a
                a is TypeArgument.Bounded && b is TypeArgument.Bounded && a.variance == b.variance -> {
                    val newNode = mergeTypeSignatureNodes(a.node, b.node) ?: return Unbounded
                    b.copy(node = newNode)
                }
                a is TypeArgument.Bounded && b is TypeArgument.Bounded && a.isInvariant != b.isInvariant -> {
                    val newNode = mergeTypeSignatureNodes(a.node, b.node) ?: return Unbounded

                    val argumentToCopy =
                        a.takeIf { it.variance != INVARIANT } ?: b

                    argumentToCopy.copy(node = newNode)
                }
                else -> Unbounded
            }
        }
        when {
            a == b -> return a
            a is TypeSignatureNode.ClassType && b is TypeSignatureNode.ClassType -> {
                if (a.classTypeName != b.classTypeName) return null
                val (aInnerNames, aInnerArgs) = a.inners.unzip()
                val (bInnerNames, bInnerArgs) = b.inners.unzip()
                if (aInnerNames != bInnerNames) return null


                val aClassArgs = a.classArgs
                val bClassArgs = b.classArgs
                if (aClassArgs.size != bClassArgs.size) return null
                if (aInnerArgs.size != bInnerArgs.size) return null


                val newClassArgs = aClassArgs.zip(bClassArgs).map { mergeArguments(it) }
                val newInnerArgs = aInnerArgs.zip(bInnerArgs).map { (a, b) -> a.zip(b).map { mergeArguments(it) } }
                return a.copy(classArgs = newClassArgs, inners = aInnerNames zip newInnerArgs)
            }
            a is TypeSignatureNode.ArrayType && b is TypeSignatureNode.ArrayType -> {
                val newType = mergeTypeSignatureNodes(a.type, b.type) ?: return null
                return a.copy(type = newType)
            }
            else -> return null
        }
    }

    private fun appendMethod(to: SSGClass, sourceMethod: SSGMethodOrGroup, source: SSGClass) {
        val fqd = sourceMethod.fqd()
        val targetMethod = to.methodsBySignature[fqd] ?: return to.addMethod(sourceMethod)

        fun mergeInto(targetMethod: SSGMethod, sourceMethod: SSGMethod): Boolean {
            return tryMerge(S.methods, targetMethod, sourceMethod) {
                if (!targetMethod.access.sameMasked(sourceMethod.access, METHOD_KIND_MASK)) {
                    reportMergeFailure(methodKindMismatch, "")
                }


                mergeMethodSignaturesWithErasure(
                    targetMethod,
                    sourceMethod.signature,
                    allowDeepMerge = targetMethod.isConstructor
                )


                mergeModalities(targetMethod, sourceMethod)
                mergeVisibilities(targetMethod, sourceMethod)
                mergeNullability(targetMethod, sourceMethod)
                mergeAnnotations(targetMethod, sourceMethod)

                for ((i, source) in sourceMethod.parameterInfoArray.withIndex()) {
                    source ?: continue
                    val target = targetMethod.parameterInfoArray.getOrInit(i) { SSGParameterInfo(i) }
                    mergeNullability(target, source)
                    mergeAnnotations(target, source)
                }

                targetMethod.version = targetMethod.version + sourceMethod.version
            }
        }

        val targets = targetMethod.methods

        val sources = sourceMethod.methods

        val unmerged = sources.filter { source ->
            targets.none { target -> mergeInto(target, source) }
        }

        val results = targets + unmerged

        val result: SSGMethodOrGroup = results.singleOrNull() ?: SSGMethodGroup(results)

        to.methodsBySignature[fqd] = result
    }

    private fun <T : SSGAnnotated> mergeAnnotations(a: T, b: T) {
        if (b.annotations != null) {
            val allAnnotations =
                ((a.annotations ?: listOf()) + b.annotations!!)
                    .groupBy { it.desc }
            a.annotations = allAnnotations.mapNotNull { (desc, sameDescAnnotations) ->
                val annotations = sameDescAnnotations.distinctBy { it.flattenedValues() }
                tryMerge(S.annotations, annotations) {
                    annotations.singleOrNull() ?: reportMergeFailure(differentAnnotationsWithSameDesc, desc)
                }
            }
        }
    }

    private fun <T : SSGNullabilityContainer> mergeNullability(a: T, b: T) {
        if (a.nullability != b.nullability) {
            a.nullability = listOf(a.nullability, b.nullability).max()!!
        }
    }

    private fun mergeClassesInternals(a: SSGClass, b: SSGClass) {
        a.version = a.version + b.version
        b.methodsBySignature.values.forEach {
            appendMethod(a, it, b)
        }
        b.fieldsBySignature.values.forEach {
            appendField(a, it, b)
        }

        a.interfaces = (a.interfaces + b.interfaces).distinct()

        mergeAnnotations(a, b)

        if (b.innerClassesBySignature.isNotEmpty()) {
            val allInners = a.innerClassesBySignature mergeToMultiMapWith b.innerClassesBySignature

            a.innerClassesBySignature =
                    allInners.mapNotNull { (desc, refs) ->
                        tryMerge(S.innerClassReferences, refs) {
                            val ref = refs.singleOrNull() ?: reportMergeFailure(differentInnersWithSameDesc, desc)
                            desc to ref
                        }
                    }.toMap()
        }

    }

    private fun <T> mergeVisibilities(into: T, from: T)
            where T : SSGAlternativeVisibilityContainer, T : SSGVersionContainer {
        if (!into.sameVisibility(from)) {
            val intoVis = into.visibility
            val fromVis = from.visibility

            into.alternativeVisibility.merge(intoVis, into.version, Version::plus)
            into.alternativeVisibility.merge(fromVis, from.version, Version::plus)

            into.visibility = into.alternativeVisibility.keys.max() ?: Visibility.PUBLIC
        }
    }

    private fun <T : SSGAlternativeModalityContainer> mergeModalities(into: T, from: T) {
        if (!into.sameModality(from)) {

            val intoModality = into.modality
            val fromModality = from.modality

            into.alternativeModality.merge(intoModality, into.version, Version::plus)
            into.alternativeModality.merge(fromModality, from.version, Version::plus)
            into.modality = Modality.OPEN
        }
    }

    fun mergeClasses(a: SSGClass, b: SSGClass): Boolean {
        if (b.version in a.version) {
            logger.info("Duplicated class ${a.fqName}")
            return true
        }

        tryMerge(S.classes, a, b) {
            if (a.isKotlin != b.isKotlin) {
                //reportMergeFailure(kotlinMismatch, "Kotlin mismatch ${a.isKotlin} != ${b.isKotlin}")
            }
            if (!a.sameKind(b)) {
                reportMergeFailure(classKindMismatch, "")
            }
            if (a.ownerInfo != b.ownerInfo) {
                reportMergeFailure(ownerClassMismatch, "${a.ownerInfo} != ${b.ownerInfo}")
            }
            if (a.superType != b.superType) {
                reportMergeFailure(superTypeMismatch, "${a.superType} != ${b.superType}")
            }
            if (a.signature != b.signature) {
                reportMergeFailure(genericsMismatch, "${a.signature} != ${b.signature}")
            }
            mergeVisibilities(a, b)
            mergeModalities(a, b)
            mergeClassesInternals(a, b)
        }
        return true
    }
}

