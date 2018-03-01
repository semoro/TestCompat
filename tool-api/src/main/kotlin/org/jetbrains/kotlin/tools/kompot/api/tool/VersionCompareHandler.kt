package org.jetbrains.kotlin.tools.kompot.api.tool


interface VersionCompareHandler {

    /**
     * Checks that [a] is subset of [b]
     * Should be false, if [a] equals [b]
     */
    fun isSubset(a: Version, b: Version): Boolean
}