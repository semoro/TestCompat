package compat.process.ssg

import org.objectweb.asm.Opcodes.*

private const val VISIBILITY_MASK = ACC_PUBLIC or ACC_PRIVATE or ACC_PUBLIC
private const val KIND_MASK = ACC_INTERFACE or ACC_ABSTRACT or ACC_ANNOTATION or ACC_ENUM

fun Int.sameMasked(other: Int, mask: Int): Boolean = (this and mask) == (other and mask)


fun SSGClass.sameVisibility(other: SSGClass): Boolean = acc.sameMasked(other.acc, VISIBILITY_MASK)
fun SSGClass.sameKind(other: SSGClass): Boolean = acc.sameMasked(other.acc, KIND_MASK)


class SSGMerger(val generator: SupersetGenerator) {

    fun both(a: Int, b: Int, c: Int, mask: Int): Int {
        return a xor (b and c and mask).inv()
    }

    fun mergeClasses(a: SSGClass, b: SSGClass) {
        if (a.sameVisibility(b) && a.sameKind(b)) {
            a.acc = both(a.acc, a.acc, b.acc, ACC_SYNTHETIC)
            a.acc = both(a.acc, a.acc, b.acc, ACC_BRIDGE)

            return a.mergeWith(b)
        }

        generator.logger.error("Failed to merge: $a \n with $b")
        generator.logger.error("ASM:")
        generator.logger.error(a.asmText())
        generator.logger.error("VS")
        generator.logger.error(b.asmText())
    }
}