package api

class A {
    fun same() {
        println("Same V1")
    }
    fun v1() {
        println("v1()")
    }
    fun paramDiff(x: Int) {
        println("V1 paramDiff(x = $x)")
    }
    fun callAbstract(impl: Abstract) {
        impl.g(1)
    }

    fun callRetDiff(impl: ReturnDiff) {
        println("Ret ${impl.a()::class}")
    }
}