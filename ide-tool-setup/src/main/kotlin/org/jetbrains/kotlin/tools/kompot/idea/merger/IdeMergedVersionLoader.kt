package org.jetbrains.kotlin.tools.kompot.idea.merger

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionLoader

class IdeMergedVersionLoader : VersionLoader {
    override fun load(literalValue: String): Version {
        return IdeMergedVersion(literalValue.split(", ").map {
            IdeVersion.createIdeVersion(
                it
            )
        })
    }
}