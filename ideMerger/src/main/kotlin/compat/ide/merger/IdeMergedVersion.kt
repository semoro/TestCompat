package compat.ide.merger

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import compat.process.Version

data class IdeMergedVersion(val ideVersions: List<IdeVersion>) : Version {
    override fun asLiteralValue(): String = ideVersions.joinToString { it.asString() }
}