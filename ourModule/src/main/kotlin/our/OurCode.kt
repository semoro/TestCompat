package our

import api.A
import api.Abstract
import api.V1
import api.V2
import compat.rt.CompatibleWith
import compat.rt.ExistsIn
import compat.rt.VersionConst
import compat.rt.forVersion


@CompatibleWith("1")
class ClassForV1 {
    constructor() {
        println("constructing for V1")
    }
}

@CompatibleWith("2")
class ClassForV2 {
    constructor() {
        println("constructing for V2")
    }
}

class ImplCommonAbstract: Abstract() {
    @CompatibleWith("1")
    override fun g(x: Int) {
        println("g(x = $x)")
    }

    @CompatibleWith("1")
    fun usesV1(v: V1): V1 {
        return V1()
    }

    @CompatibleWith("2")
    override fun g(x: Int, y: Int) {
        println("g(x = $x, y = $y)")
    }
}

fun main(args: Array<String>) {
    val ourA = A()

    ourA.same()

    forVersion("1") {
        ourA.v1()
        ourA.paramDiff(1)
        val v1 = V1()
        val cv1 = ClassForV1()
    } ?: forVersion("2") {
        ourA.v2()
        ourA.paramDiff(1, 2)
        val v2 = V2()
        val cv2 = ClassForV2()
    }

    val abstractImpl = ImplCommonAbstract()
    ourA.callAbstract(abstractImpl)
}