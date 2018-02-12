package org.jetbrains.kotlin.tools.kompot.ssg

import org.jetbrains.kotlin.tools.kompot.api.annotations.Modality
import org.jetbrains.kotlin.tools.kompot.api.annotations.Visibility
import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.objectweb.asm.Opcodes.*

private const val VISIBILITY_MASK = ACC_PUBLIC or ACC_PRIVATE or ACC_PUBLIC or ACC_PROTECTED
private const val KIND_MASK = ACC_INTERFACE or ACC_ANNOTATION or ACC_ENUM
private const val MODALITY_MASK = ACC_ABSTRACT or ACC_FINAL

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

infix fun Int.hasFlag(i: Int) = this and i != 0
infix fun Int.noFlag(i: Int) = this and i == 0

class SSGMerger(val generator: SupersetGenerator) {

    operator fun Version?.plus(other: Version?) = generator.versionHandler.plus(this, other)
    operator fun Version?.contains(other: Version?) = generator.versionHandler.contains(this, other)

    fun both(a: Int, b: Int, c: Int, mask: Int): Int {
        return a xor (b and c and mask).inv()
    }

    var mergeSuccess = 0
    var mergeFailure = 0
    var kMergeFailure = 0

    var mMergeFailure = 0
    var mMergeSuccess = 0

    var fMergeFailure = 0
    var fMergeSuccess = 0

    private fun appendField(to: SSGClass, sourceField: SSGField, source: SSGClass) {
        val fqd = sourceField.fqd()
        val targetField = to.fieldsBySignature[fqd] ?: return to.addField(sourceField)

        if (targetField.modality == sourceField.modality) {
            mergeVisibilities(targetField, sourceField)

            targetField.version = mergeVersions(targetField.version, sourceField.version)

            fMergeSuccess++
            return
        }

        generator.logger.error("Couldn't merge fields")
        generator.logger.error("target: ${to.fqName}.$targetField")
        generator.logger.error("source: ${source.fqName}.$sourceField")
        fMergeFailure++
    }

    private fun appendMethod(to: SSGClass, sourceMethod: SSGMethod, source: SSGClass) {
        val fqd = sourceMethod.fqd()
        val targetMethod = to.methodsBySignature[fqd] ?: return to.addMethod(sourceMethod)

        fun reportMergeFailure(message: String) {
            generator.logger.error("Couldn't merge methods: $message")
            generator.logger.error("target: ${to.fqName}.$targetMethod")
            generator.logger.error("source: ${source.fqName}.$sourceMethod")
            mMergeFailure++
        }


        mergeModalities(targetMethod, sourceMethod)
        mergeVisibilities(targetMethod, sourceMethod)

        targetMethod.version = mergeVersions(targetMethod.version, sourceMethod.version)

        mMergeSuccess++
    }

    private fun mergeClassesInternals(a: SSGClass, b: SSGClass) {
        a.version = mergeVersions(a.version, b.version)
        b.methodsBySignature.values.forEach {
            appendMethod(a, it, b)
        }
        b.fieldsBySignature.values.forEach {
            appendField(a, it, b)
        }
    }

    private fun <T> mergeVisibilities(into: T, from: T)
            where T : SSGAlternativeVisibilityContainer, T : SSGVersionContainer {
        if (!into.sameVisibility(from)) {
            val intoVis = into.visibility
            val fromVis = from.visibility

            // += workaround KT-21724
            into.alternativeVisibility().let { it[intoVis] = it[intoVis] + into.version }
            into.alternativeVisibility().let { it[fromVis] = it[fromVis] + from.version }

            into.visibility = into.alternativeVisibility().keys.max() ?: Visibility.PUBLIC
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

        fun reportMergeFailure(message: String) {
            generator.logger.error("""
                |$message
                |Failed to merge:
                |$a
                |with:
                |$b
                |
                |ASM:
                |${a.asmText()}
                |VS
                |${b.asmText()}
                |""".trimMargin())

        }
        if (a.isKotlin != b.isKotlin) {
            reportMergeFailure("Kotlin mismatch ${a.isKotlin} != ${b.isKotlin}")
            kMergeFailure++
            return
        }
        if (!a.sameKind(b)) {
            reportMergeFailure("Kind mismatch")
            mergeFailure++
            return
        }
        if (a.ownerInfo != b.ownerInfo) {
            reportMergeFailure("Owner mismatch ${a.ownerInfo} != ${b.ownerInfo}")
            mergeFailure++
            return
        }
        mergeVisibilities(a, b)
        mergeModalities(a, b)
        mergeClassesInternals(a, b)
        mergeSuccess++
    }

    fun mergeVersions(a: Version?, b: Version?) = a + b

    fun formatStatistics(): String {
        return "Classes s: $mergeSuccess, f: $mergeFailure, kf: $kMergeFailure, t: ${mergeSuccess + mergeFailure + kMergeFailure}\n" +
                "Fields s: $fMergeSuccess, f: $fMergeFailure, t: ${fMergeSuccess + fMergeFailure}\n" +
                "Methods s: $mMergeSuccess, f: $mMergeFailure, t: ${mMergeSuccess + mMergeFailure}\n"
    }
}

var SSGAccess.visibility: Visibility
    get() {
        return with(access) {
            when {
                hasFlag(ACC_PUBLIC) -> Visibility.PUBLIC
                hasFlag(ACC_PROTECTED) -> Visibility.PROTECTED
                hasFlag(ACC_PRIVATE) -> Visibility.PRIVATE
                else -> Visibility.PACKAGE_PRIVATE
            }
        }
    }
    set(value) {
        val flag = when (value) {
            Visibility.PUBLIC -> ACC_PUBLIC
            Visibility.PROTECTED -> ACC_PROTECTED
            Visibility.PACKAGE_PRIVATE -> 0
            Visibility.PRIVATE -> ACC_PRIVATE
        }
        access = access and (VISIBILITY_MASK.inv()) or flag
    }


var SSGAccess.modality: Modality
    get() {
        return when {
            access.hasFlag(ACC_ABSTRACT) -> Modality.ABSTRACT
            access.hasFlag(ACC_FINAL) -> Modality.FINAL
            else -> Modality.OPEN
        }
    }
    set(value) {
        val flag = when(value) {
            Modality.FINAL -> ACC_FINAL
            Modality.ABSTRACT -> ACC_ABSTRACT
            Modality.OPEN -> 0
        }
        access = access and (MODALITY_MASK.inv()) or flag
    }

