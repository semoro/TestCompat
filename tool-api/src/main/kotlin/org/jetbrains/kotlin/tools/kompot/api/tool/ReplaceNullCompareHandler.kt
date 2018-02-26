package org.jetbrains.kotlin.tools.kompot.api.tool

class ReplaceNullCompareHandler(val default: Version, val delegate: VersionCompareHandler) : VersionCompareHandler {
    override fun isSubset(a: Version?, b: Version?): Boolean {
        return delegate.isSubset(a ?: default, b ?: default)
    }
}