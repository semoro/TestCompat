package org.jetbrains.kotlin.tools.kompot.api.tool

interface VersionLoader {
    fun load(literalValue: String?): Version
}