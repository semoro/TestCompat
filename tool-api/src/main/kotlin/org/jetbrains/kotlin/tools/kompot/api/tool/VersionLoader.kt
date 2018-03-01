package org.jetbrains.kotlin.tools.kompot.api.tool

import org.jetbrains.kotlin.tools.kompot.api.tool.Version

interface VersionLoader {
    fun load(literalValue: String?): Version
}