package api

import compat.rt.ExistsIn

abstract class Abstract {

    @ExistsIn("1")
    abstract fun g(x: Int)

    @ExistsIn("2")
    abstract fun g(x: Int, y: Int)
}