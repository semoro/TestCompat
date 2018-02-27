package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.annotations.Modality
import org.jetbrains.kotlin.tools.kompot.api.annotations.Visibility
import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionHandler
import org.jetbrains.kotlin.tools.kompot.commons.getOrInit
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.classKindMismatch
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.differentAnnotationsWithSameDesc
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.differentInnersWithSameDesc
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.fieldModalityMismatch
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.genericsMismatch
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.methodKindMismatch
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.ownerClassMismatch
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

    operator fun Version?.plus(other: Version?) = versionHandler.plus(this, other)
    operator fun Version?.contains(other: Version?) = versionHandler.contains(this, other)

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

    private fun appendMethod(to: SSGClass, sourceMethod: SSGMethodOrGroup, source: SSGClass) {
        val fqd = sourceMethod.fqd()
        val targetMethod = to.methodsBySignature[fqd] ?: return to.addMethod(sourceMethod)


        fun mergeInto(targetMethod: SSGMethod, sourceMethod: SSGMethod): Boolean {
            return tryMerge(S.methods, targetMethod, sourceMethod) {
                if (!targetMethod.access.sameMasked(sourceMethod.access, METHOD_KIND_MASK)) {
                    reportMergeFailure(methodKindMismatch, "")
                }

                if (targetMethod.signature != sourceMethod.signature) {
                    reportMergeFailure(genericsMismatch, "${targetMethod.signature} != ${sourceMethod.signature}")
                }

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

        val targets = allMethods(targetMethod)

        val sources = allMethods(sourceMethod)

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

        mergeAnnotations(a, b)

        if (b.innerClassesBySignature != null) {
            val target = a::innerClassesBySignature.getOrInit { mutableMapOf() }
            val allInners =
                (target.entries + b.innerClassesBySignature!!.entries)
                    .distinct()
                    .groupBy({ it.key }, { it.value })
            a.innerClassesBySignature = mutableMapOf()
            allInners.forEach { (desc, refs) ->
                tryMerge(S.innerClassReferences, refs) {
                    val ref = refs.singleOrNull() ?: reportMergeFailure(differentInnersWithSameDesc, desc)
                    a.innerClassesBySignature!![desc] = ref
                }
            }

        }

    }

    private fun <T> mergeVisibilities(into: T, from: T)
            where T : SSGAlternativeVisibilityContainer, T : SSGVersionContainer {
        if (!into.sameVisibility(from)) {
            val intoVis = into.visibility
            val fromVis = from.visibility

            // += workaround KT-21724
            into.alternativeVisibility[intoVis] = into.alternativeVisibility[intoVis] + into.version
            into.alternativeVisibility[fromVis] = into.alternativeVisibility[fromVis] + from.version

            into.visibility = into.alternativeVisibility.keys.max() ?: Visibility.PUBLIC
        }
    }

    private fun <T : SSGAlternativeModalityContainer> mergeModalities(into: T, from: T) {
        if (!into.sameModality(from)) {

            val intoModality = into.modality
            val fromModality = from.modality

            into.alternativeModality[intoModality] = into.alternativeModality[intoModality] + into.version
            into.alternativeModality[fromModality] = into.alternativeModality[fromModality] + from.version
            into.modality = Modality.OPEN
        }
    }

    fun mergeClasses(a: SSGClass, b: SSGClass) {
        if (b.version in a.version) {
            logger.info("Duplicated class ${a.fqName}")
            return
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
            if (a.signature != b.signature) {
                reportMergeFailure(genericsMismatch, "${a.signature} != ${b.signature}")
            }
            mergeVisibilities(a, b)
            mergeModalities(a, b)
            mergeClassesInternals(a, b)
        }
    }
}

