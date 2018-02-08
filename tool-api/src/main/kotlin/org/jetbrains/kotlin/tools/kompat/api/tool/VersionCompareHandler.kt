package org.jetbrains.kotlin.tools.kompat.api.tool

import org.jetbrains.kotlin.tools.kompat.api.tool.Version

interface VersionCompareHandler {

    /**
     * Checks that [a] is subset of [b]
     * Should be false, if [a] equals [b]
     */
    fun isSubset(a: Version?, b: Version?): Boolean
}