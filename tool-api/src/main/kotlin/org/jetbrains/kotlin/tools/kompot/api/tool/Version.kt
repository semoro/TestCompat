package org.jetbrains.kotlin.tools.kompot.api.tool

import java.io.Serializable

interface VersionHandler : Serializable {
    fun plus(t: Version?, other: Version?): Version?
    fun contains(t: Version?, other: Version?): Boolean
}


interface Version : Serializable {
    fun asLiteralValue(): String
    override fun equals(other: Any?): Boolean
}
