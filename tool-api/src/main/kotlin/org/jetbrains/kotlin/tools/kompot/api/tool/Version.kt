package org.jetbrains.kotlin.tools.kompot.api.tool

import java.io.Serializable

interface VersionHandler : VersionCompareHandler, Serializable {

    override fun isSubset(a: Version, b: Version): Boolean {
        return a != b && b.contains(a)
    }
}


interface Version : Serializable {
    fun asLiteralValue(): String?
    operator fun plus(other: Version): Version
    operator fun contains(other: Version): Boolean
    override fun equals(other: Any?): Boolean
}
