package compat.ide.merger

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionHandler

class IdeMergedVersionHandler : VersionHandler {
    override fun plus(t: Version?, other: Version?): Version? {
        return when {
            other == null -> t
            t == null -> other
            else -> {
                val mergedVersions = (t as IdeMergedVersion).ideVersions + (other as IdeMergedVersion).ideVersions
                IdeMergedVersion(mergedVersions)
            }
        }
    }

    override fun contains(t: Version?, other: Version?): Boolean {
        return when {
            t == null -> false
            other == null -> true
            else -> {
                (t as IdeMergedVersion).ideVersions.containsAll((other as IdeMergedVersion).ideVersions)
            }
        }
    }
}