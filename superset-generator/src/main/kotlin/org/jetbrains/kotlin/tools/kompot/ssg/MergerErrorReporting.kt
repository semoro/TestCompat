package org.jetbrains.kotlin.tools.kompot.ssg


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
}

open class MergeProtectionScope<in T : SSGNode<*>>(val name: String) {
    val reports = mutableListOf<MergeFailedException>()
    open fun onFail(mfe: MergeFailedException, a: T, b: T) {
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

fun MergeProtectionScope<*>.statistics(): String = buildString {
    appendln("$name:")
    reports.groupBy { it.key }.forEach { (key, values) ->
        appendln("$key - ${values.size}")
    }
    appendln("s:$success f:$fails t:$total")
}

inline fun <T : SSGNode<T>> SSGMerger.tryMerge(scope: MergeProtectionScope<T>, a: T, b: T, l: () -> Unit) {
    try {
        l()
        scope.success++
    } catch (mfe: MergeFailedException) {
        scope.onFail(mfe, a, b)
    }
}

fun SSGMerger.reportMergeFailure(
    key: MergeFailures.MergeFailureKey,
    message: String
): Nothing {
    throw MergeFailedException(key, message)
}