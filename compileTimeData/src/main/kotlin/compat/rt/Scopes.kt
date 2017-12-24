package compat.rt


object VersionChecker {
    val selectorClass = Class.forName("compat.rt.VersionSelector")
    val selector = selectorClass.newInstance() as VersionSwitch
    fun accepts(v: String): Boolean = selector.accepts(v)
}

fun enterVersionScope(v: String): Boolean { return VersionChecker.accepts(v) }
fun leaveVersionScope() {}

inline fun <T> forVersion(v: String, l: () -> T): T? =
        if (enterVersionScope(v)) {
            val res: T = l()
            leaveVersionScope()
            res
        } else
            null