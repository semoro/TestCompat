package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.annotations.Modality
import org.jetbrains.kotlin.tools.kompot.api.annotations.Visibility
import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.commons.getOrInit
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.classKindMismatch
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.fieldModalityMismatch
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.genericsMismatch
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.kotlinMismatch
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.methodKindMismatch
import org.jetbrains.kotlin.tools.kompot.ssg.MergeFailures.ownerClassMismatch


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


class SSGMerger(val generator: SupersetGenerator) {

    operator fun Version?.plus(other: Version?) = generator.versionHandler.plus(this, other)
    operator fun Version?.contains(other: Version?) = generator.versionHandler.contains(this, other)

    val Classes = object : MergeProtectionScope<SSGClass>("Classes") {
        override fun onFail(mfe: MergeFailedException, a: SSGClass, b: SSGClass) {
            generator.logger.error(
                "Failed to merge methods: ${mfe.key.message}: ${mfe.fmessage}\n" +
                        "Target: $a\n" +
                        "Source: $b\n"
            )
            super.onFail(mfe, a, b)
        }
    }
    val Fields = object : MergeProtectionScope<SSGField>("Fields") {
        override fun onFail(mfe: MergeFailedException, a: SSGField, b: SSGField) {
            generator.logger.error(
                "Failed to merge methods: ${mfe.key.message}: ${mfe.fmessage}\n" +
                        "Target: $a\n" +
                        "Source: $b\n"
            )
            super.onFail(mfe, a, b)
        }
    }
    val Methods = object : MergeProtectionScope<SSGMethod>("Methods") {
        override fun onFail(mfe: MergeFailedException, a: SSGMethod, b: SSGMethod) {
            generator.logger.error(
                "Failed to merge methods: ${mfe.key.message}: ${mfe.fmessage}\n" +
                        "Target: ${a.debugText()}\n" +
                        "Source: ${b.debugText()}\n"
            )
            super.onFail(mfe, a, b)
        }
    }

    private fun appendField(to: SSGClass, sourceField: SSGField, source: SSGClass) {
        val fqd = sourceField.fqd()
        val targetField = to.fieldsBySignature[fqd] ?: return to.addField(sourceField)
        mergeFields(targetField, sourceField)
    }

    private fun mergeFields(targetField: SSGField, sourceField: SSGField) = tryMerge(Fields, targetField, sourceField) {
        if (targetField.signature != sourceField.signature) {
            reportMergeFailure(genericsMismatch, "${targetField.signature} != ${sourceField.desc}")
        }

        if (targetField.modality != sourceField.modality) {
            reportMergeFailure(fieldModalityMismatch, "$")
        }

        if (targetField.modality == sourceField.modality) {
            mergeVisibilities(targetField, sourceField)

            targetField.version = targetField.version + sourceField.version
        }
    }

    private fun appendMethod(to: SSGClass, sourceMethod: SSGMethod, source: SSGClass) {
        val fqd = sourceMethod.fqd()
        val targetMethod = to.methodsBySignature[fqd] ?: return to.addMethod(sourceMethod)
        tryMerge(Methods, targetMethod, sourceMethod) {
            if (!targetMethod.access.sameMasked(sourceMethod.access, METHOD_KIND_MASK)) {
                reportMergeFailure(methodKindMismatch, "")
            }

            if (targetMethod.signature != sourceMethod.signature) {
                reportMergeFailure(genericsMismatch, "${targetMethod.signature} != ${sourceMethod.signature}")
            }

            mergeModalities(targetMethod, sourceMethod)
            mergeVisibilities(targetMethod, sourceMethod)

            targetMethod.version = targetMethod.version + sourceMethod.version
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

        if (b.innerClassesBySignature != null) {
            val target = a::innerClassesBySignature.getOrInit { mutableMapOf() }
            b.innerClassesBySignature?.forEach { (signature, value) ->
                val sameSignatureRef = target[signature]
                if (sameSignatureRef != null && sameSignatureRef != value) {
                    generator.logger.error("Different inner classes with same signature: $sameSignatureRef != $value")
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
            into.modality = into.alternativeModality.keys.max() ?: Modality.OPEN
        }
    }

    fun mergeClasses(a: SSGClass, b: SSGClass) {
        if (b.version in a.version) {
            generator.logger.info("Duplicated class ${a.fqName}")
            return
        }

        tryMerge(Classes, a, b) {
            if (a.isKotlin != b.isKotlin) {
                reportMergeFailure(kotlinMismatch, "Kotlin mismatch ${a.isKotlin} != ${b.isKotlin}")
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

    fun formatStatistics(): String = buildString {
        append(Classes.statistics())
        append(Fields.statistics())
        append(Methods.statistics())
    }
}

