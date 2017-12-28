package compat.process.ssg

import compat.process.Version
import org.objectweb.asm.Opcodes.*

private const val VISIBILITY_MASK = ACC_PUBLIC or ACC_PRIVATE or ACC_PUBLIC
private const val KIND_MASK = ACC_INTERFACE or ACC_ABSTRACT or ACC_ANNOTATION or ACC_ENUM

fun Int.sameMasked(other: Int, mask: Int): Boolean = (this and mask) == (other and mask)


fun SSGClass.sameVisibility(other: SSGClass): Boolean = access.sameMasked(other.access, VISIBILITY_MASK)
fun SSGClass.sameKind(other: SSGClass): Boolean = access.sameMasked(other.access, KIND_MASK)
fun SSGMethod.sameVisibility(other: SSGMethod): Boolean = access.sameMasked(other.access, VISIBILITY_MASK)
fun SSGMethod.sameKind(other: SSGMethod): Boolean = access.sameMasked(other.access, KIND_MASK)
fun SSGField.sameVisibility(other: SSGField): Boolean = access.sameMasked(other.access, VISIBILITY_MASK)
fun SSGField.sameKind(other: SSGField): Boolean = access.sameMasked(other.access, KIND_MASK)

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
        val fieldToMergeWith = to.fieldsBySignature[fqd] ?: return to.addField(sourceField)

        if (fieldToMergeWith.sameKind(sourceField) && fieldToMergeWith.sameVisibility(sourceField)) {
            fieldToMergeWith.version = mergeVersions(fieldToMergeWith.version, sourceField.version)

            fMergeSuccess++
            return
        }

        generator.logger.error("Couldn't merge fields")
        generator.logger.error("target: $fieldToMergeWith")
        generator.logger.error("source: $sourceField")
        fMergeFailure++
    }

    fun appendMethod(to: SSGClass, sourceMethod: SSGMethod, source: SSGClass) {
        val fqd = sourceMethod.fqd()
        val methodToMergeWith = to.methodsBySignature[fqd] ?: return to.addMethod(sourceMethod)

        if (methodToMergeWith.sameKind(sourceMethod) && methodToMergeWith.sameVisibility(sourceMethod)) {
            methodToMergeWith.version = mergeVersions(methodToMergeWith.version, sourceMethod.version)

            mMergeSuccess++
            return
        }
        generator.logger.error("Couldn't merge methods")
        generator.logger.error("target: $methodToMergeWith")
        generator.logger.error("source: $sourceMethod")
        mMergeFailure++
    }

    fun mergeClasses(a: SSGClass, b: SSGClass) {
        if (b.version in a.version) {
            generator.logger.info("Duplicated class ${a.fqName}")
            return
        }

        if (a.sameVisibility(b) && a.sameKind(b)) {


            a.version = mergeVersions(a.version, b.version)
            b.methodsBySignature.values.forEach {
                appendMethod(a, it, b)
            }
            b.fieldsBySignature.values.forEach {
                appendField(a, it, b)
            }

            mergeSuccess++
            return
        }

        generator.logger.error("Failed to merge: $a \n with $b")
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