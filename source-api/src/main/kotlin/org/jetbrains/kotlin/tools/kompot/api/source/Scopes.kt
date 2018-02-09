package org.jetbrains.kotlin.tools.kompot.api.source

import org.jetbrains.kotlin.tools.kompot.api.intrinsics.enterVersionScope
import org.jetbrains.kotlin.tools.kompot.api.intrinsics.leaveVersionScope

inline fun <T> forVersion(v: String, l: () -> T): T? =
    if (enterVersionScope(v)) {
        val res: T = l()
        leaveVersionScope()
        res
    } else {
        null
    }