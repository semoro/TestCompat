package api

import compat.rt.ExistsIn

class A {
    fun same() {}
    @ExistsIn("1")
    fun v1() {
    }

    @ExistsIn("2")
    fun v2() {
    }

    @ExistsIn("1")
    fun paramDiff(x: Int) {
    }

    @ExistsIn("2")
    fun paramDiff(x: Int, y: Int) {
    }

    fun callAbstract(impl: Abstract) {}

    fun callRetDiff(impl: ReturnDiff) {}
}