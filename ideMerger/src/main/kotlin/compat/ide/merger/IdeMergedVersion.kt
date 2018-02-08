package compat.ide.merger

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.kotlin.tools.kompat.api.tool.Version

data class IdeMergedVersion(val ideVersions: List<IdeVersion>) : Version {
    override fun asLiteralValue(): String = ideVersions.joinToString { it.asString() }
}