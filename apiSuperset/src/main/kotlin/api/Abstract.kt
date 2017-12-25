package api

import compat.rt.ExistsIn

abstract class Abstract {

    @ExistsIn("1")
    abstract fun g(x: Int)

    @ExistsIn("2")
    abstract fun g(x: Int, y: Int)
}

abstract class ReturnDiff {

    @ExistsIn("1")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("a")
    abstract fun aInt(): Int

    @ExistsIn("2")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("a")
    abstract fun aString(): String
}