package org.jetbrains.kotlin.tools.kompot.commons

import org.jetbrains.kotlin.tools.kompot.api.tool.Version
import org.jetbrains.kotlin.tools.kompot.api.tool.VersionInfoProvider
import org.objectweb.asm.Type


fun VersionInfoProvider.forClass(type: Type): Version? {
    return this.forClass(type.className)
}
