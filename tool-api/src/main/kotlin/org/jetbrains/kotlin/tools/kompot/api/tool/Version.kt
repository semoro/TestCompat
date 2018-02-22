package org.jetbrains.kotlin.tools.kompot.api.tool

import java.io.Serializable

interface VersionHandler : VersionCompareHandler, Serializable {
    fun plus(t: Version?, other: Version?): Version?
    fun contains(t: Version?, other: Version?): Boolean
    override fun isSubset(a: Version?, b: Version?): Boolean {
        return a != b && contains(b, a)
    }
}


interface Version : Serializable {
    fun asLiteralValue(): String
    override fun equals(other: Any?): Boolean
}
