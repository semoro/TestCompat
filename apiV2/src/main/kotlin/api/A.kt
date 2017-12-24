package api

class A {
    fun same() {
        println("Same V2")
    }
    fun v2() {
        println("v2()")
    }
    fun paramDiff(x: Int, y: Int) {
        println("V2 paramDiff(x = $x, y = $y)")
    }

    fun callAbstract(impl: Abstract) {
        impl.g(1, 2)
    }
}