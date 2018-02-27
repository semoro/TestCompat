package p

import org.jetbrains.kotlin.tools.kompot.api.source.forVersion

class Scopes {
    fun f() {
        forVersion("2") {
            println("2")
        } ?: forVersion("1") {
            println("1")
        }
    }

    fun nestedScopes() {
        forVersion("1") {
            forVersion("2") {
                println("2 in 1")
            }
        }
    }

    fun varAssignment() {
        val v = forVersion("1") {
            "1"
        }
        println(v)
    }
}