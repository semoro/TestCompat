package org.jetbrains.kotlin.tools.kompot.idea.merger

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionLoader

class IdeMergedVersionLoader(private val default: Version = IdeMergedVersion.Default) : VersionLoader {
    override fun load(literalValue: String?): Version {
        literalValue ?: return default
        return IdeMergedVersion(literalValue.split(", ").map {
            IdeVersion.createIdeVersion(
                it
            )
        })
    }
}