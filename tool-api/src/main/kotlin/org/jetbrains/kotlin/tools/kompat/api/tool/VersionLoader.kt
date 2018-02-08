package org.jetbrains.kotlin.tools.kompat.api.tool

import org.jetbrains.kotlin.tools.kompat.api.tool.Version

interface VersionLoader {
    fun load(literalValue: String): Version
}