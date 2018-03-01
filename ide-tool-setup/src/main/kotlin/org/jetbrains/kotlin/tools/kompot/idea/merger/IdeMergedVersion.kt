package org.jetbrains.kotlin.tools.kompot.idea.merger

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.kotlin.tools.kompot.api.tool.Version

data class IdeMergedVersion(private val ideVersions: List<IdeVersion>) : Version {
    object Default : Version {
        override fun asLiteralValue(): String? = null

        override fun plus(other: Version) = other

        override fun contains(other: Version) = true

        override fun equals(other: Any?) = other == this
    }

    override operator fun plus(other: Version): Version {
        return if (other == Default) {
            this
        } else {
            val mergedVersions = ideVersions + (other as IdeMergedVersion).ideVersions
            IdeMergedVersion(mergedVersions)
        }
    }

    override operator fun contains(other: Version): Boolean {
        return if (other == Default) {
            false
        } else {
            ideVersions.containsAll((other as IdeMergedVersion).ideVersions)
        }
    }

    override fun asLiteralValue(): String = ideVersions.joinToString { it.asString() }
}