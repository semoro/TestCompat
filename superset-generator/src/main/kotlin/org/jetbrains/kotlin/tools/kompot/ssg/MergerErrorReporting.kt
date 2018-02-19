package org.jetbrains.kotlin.tools.kompot.ssg

import org.objectweb.asm.tree.AnnotationNode
import org.slf4j.Logger


object MergeFailures {
    class MergeFailureKey(val message: String) {
        override fun toString(): String {
            return message
        }
    }

    val genericsMismatch = MergeFailureKey("Generics mismatch")

    val fieldModalityMismatch = MergeFailureKey("Field modality mismatch")

    val methodKindMismatch = MergeFailureKey("Method kind mismatch")

    val classKindMismatch = MergeFailureKey("Method kind mismatch")
    val ownerClassMismatch = MergeFailureKey("Owner mismatch")
    val kotlinMismatch = MergeFailureKey("Kotlin mismatch")

    val differentAnnotationsWithSameDesc = MergeFailureKey("Different annotations with same desc")

    val differentInnersWithSameDesc = MergeFailureKey("Different inner class references with same desc")
}

open class MergeProtectionScope<in T>(val name: String) {
    val reports = mutableListOf<MergeFailedException>()
    open fun onFail(mfe: MergeFailedException, a: T, b: T) {
        onFail(mfe)
    }

    open fun onFail(mfe: MergeFailedException, multiple: List<T>) {
        onFail(mfe)
    }

    fun onFail(mfe: MergeFailedException) {
        reports += mfe
    }

    var success = 0
    val total get() = reports.size + success
    val fails get() = reports.size
}

class MergeFailedException(
    val key: MergeFailures.MergeFailureKey,
    val fmessage: String
) : Exception()

fun MergeProtectionScope<*>.statistics(): String {
    val reportsCount = reports.groupingBy { it.key }.eachCount()
    return """
        |$name:
        |   Reports:
        |    ${reportsCount.entries.joinToString("\n|    ") { (key, count) -> "$key - $count" }}
        |   s:$success f:$fails t:$total
        |
    """.trimMargin()
}

inline fun <T> SSGMerger.tryMerge(scope: MergeProtectionScope<T>, a: T, b: T, l: () -> Unit) {
    try {
        l()
        scope.success++
    } catch (mfe: MergeFailedException) {
        scope.onFail(mfe, a, b)
    }
}

inline fun <T, R> SSGMerger.tryMerge(scope: MergeProtectionScope<T>, multiple: List<T>, l: () -> R): R? = try {
    l().also { scope.success++ }
} catch (mfe: MergeFailedException) {
    scope.onFail(mfe, multiple)
    null
}

fun SSGMerger.reportMergeFailure(
    key: MergeFailures.MergeFailureKey,
    message: String
): Nothing {
    throw MergeFailedException(key, message)
}

class MergeScopes(val logger: Logger) {
    val classes = object : MergeProtectionScope<SSGClass>("Classes") {
        override fun onFail(mfe: MergeFailedException, a: SSGClass, b: SSGClass) {
            logger.error(
                "Failed to merge classes: ${mfe.key.message}: ${mfe.fmessage}\n" +
                        "Target: $a\n" +
                        "Source: $b\n"
            )
            super.onFail(mfe, a, b)
        }
    }
    val fields = object : MergeProtectionScope<SSGField>("Fields") {
        override fun onFail(mfe: MergeFailedException, a: SSGField, b: SSGField) {
            logger.error(
                "Failed to merge fields: ${mfe.key.message}: ${mfe.fmessage}\n" +
                        "Target: $a\n" +
                        "Source: $b\n"
            )
            super.onFail(mfe, a, b)
        }
    }
    val methods = object : MergeProtectionScope<SSGMethod>("Methods") {
        override fun onFail(mfe: MergeFailedException, a: SSGMethod, b: SSGMethod) {
            logger.error(
                "Failed to merge methods: ${mfe.key.message}: ${mfe.fmessage}\n" +
                        "Target: ${a.debugText()}\n" +
                        "Source: ${b.debugText()}\n"
            )
            super.onFail(mfe, a, b)
        }
    }

    val annotations = object : MergeProtectionScope<AnnotationNode>("Annotations") {
        override fun onFail(mfe: MergeFailedException, multiple: List<AnnotationNode>) {
            logger.error(
                "Failed to merge annotations: ${mfe.key.message}: ${mfe.fmessage}\n" +
                        "Nodes: $multiple\n"
            )
            super.onFail(mfe, multiple)
        }
    }

    val innerClassReferences = object : MergeProtectionScope<SSGInnerClassRef>("Inner Refs") {
        override fun onFail(mfe: MergeFailedException, multiple: List<SSGInnerClassRef>) {
            logger.error(
                "Failed to merge inner-class references: ${mfe.key.message} :${mfe.fmessage}\n" +
                        "Nodes: $multiple\n"
            )
            super.onFail(mfe, multiple)
        }
    }

    fun formatStatistics(): String = buildString {
        append(classes.statistics())
        append(fields.statistics())
        append(methods.statistics())
        append(innerClassReferences.statistics())
        append(annotations.statistics())
    }
}