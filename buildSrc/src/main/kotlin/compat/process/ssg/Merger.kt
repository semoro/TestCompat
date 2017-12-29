package compat.process.ssg

import compat.process.Version
import org.objectweb.asm.Opcodes.*

private const val VISIBILITY_MASK = ACC_PUBLIC or ACC_PRIVATE or ACC_PUBLIC or ACC_PROTECTED
private const val KIND_MASK = ACC_INTERFACE or ACC_ABSTRACT or ACC_ANNOTATION or ACC_ENUM

fun Int.sameMasked(other: Int, mask: Int): Boolean = (this and mask) == (other and mask)


fun SSGAccess.sameVisibility(other: SSGAccess): Boolean = access.sameMasked(other.access, VISIBILITY_MASK)
fun SSGAccess.sameKind(other: SSGAccess): Boolean = access.sameMasked(other.access, KIND_MASK)

class SSGMerger(val generator: SupersetGenerator) {

    operator fun Version?.plus(other: Version?) = generator.versionHandler.plus(this, other)
    operator fun Version?.contains(other: Version?) = generator.versionHandler.contains(this, other)

    fun both(a: Int, b: Int, c: Int, mask: Int): Int {
        return a xor (b and c and mask).inv()
    }

    var mergeSuccess = 0
    var mergeFailure = 0

    var mMergeFailure = 0
    var mMergeSuccess = 0

    var fMergeFailure = 0
    var fMergeSuccess = 0

    fun appendField(to: SSGClass, sourceField: SSGField, source: SSGClass) {
        val fqd = sourceField.fqd()
        val targetField = to.fieldsBySignature[fqd] ?: return to.addField(sourceField)

        if (targetField.sameKind(sourceField)) {
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

    fun appendMethod(to: SSGClass, sourceMethod: SSGMethod, source: SSGClass) {
        val fqd = sourceMethod.fqd()
        val targetMethod = to.methodsBySignature[fqd] ?: return to.addMethod(sourceMethod)

        if (targetMethod.sameKind(sourceMethod)) {
            mergeVisibilities(targetMethod, sourceMethod)

            targetMethod.version = mergeVersions(targetMethod.version, sourceMethod.version)

            mMergeSuccess++
            return
        }
        generator.logger.error("Couldn't merge methods")
        generator.logger.error("target: ${to.fqName}.$targetMethod")
        generator.logger.error("source: ${source.fqName}.$sourceMethod")
        mMergeFailure++
    }

    fun mergeClassesInternals(a: SSGClass, b: SSGClass) {
        a.version = mergeVersions(a.version, b.version)
        b.methodsBySignature.values.forEach {
            appendMethod(a, it, b)
        }
        b.fieldsBySignature.values.forEach {
            appendField(a, it, b)
        }
    }

    fun <T> mergeVisibilities(into: T, from: T)
            where T : SSGAlternativeVisibilityContainer, T : SSGVersionContainer {
        if (!into.sameVisibility(from)) {
            val intoVis = into.visibility
            val fromVis = from.visibility

            // += workaround KT-21724
            into.alternativeVisibility().let { it[intoVis] = it[intoVis] + into.version }
            into.alternativeVisibility().let { it[fromVis] = it[fromVis] + from.version }

            into.visibility = Visibility.PUBLIC
        }
    }

    fun mergeClasses(a: SSGClass, b: SSGClass) {
        if (b.version in a.version) {
            generator.logger.info("Duplicated class ${a.fqName}")
            return
        }

        if (a.sameKind(b)) {
            mergeVisibilities(a, b)
            mergeClassesInternals(a, b)
            mergeSuccess++
            return
        }

        generator.logger.error("Failed to merge:\n$a \nwith:\n $b")
        generator.logger.error("ASM:")
        generator.logger.error(a.asmText())
        generator.logger.error("VS")
        generator.logger.error(b.asmText())
        mergeFailure++
    }

    fun mergeVersions(a: Version?, b: Version?) = a + b

    fun formatStatistics(): String {
        return "Classes s: $mergeSuccess, f: $mergeFailure, t: ${mergeSuccess + mergeFailure}\n" +
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
        val flag = when(value) {
            Visibility.PUBLIC -> ACC_PUBLIC
            Visibility.PROTECTED -> ACC_PROTECTED
            Visibility.PACKAGE_PRIVATE -> 0
            Visibility.PRIVATE -> ACC_PRIVATE
        }
        access = access and (VISIBILITY_MASK.inv()) or flag
    }

