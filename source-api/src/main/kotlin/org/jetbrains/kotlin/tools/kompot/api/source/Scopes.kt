package org.jetbrains.kotlin.tools.kompot.api.source

import org.jetbrains.kotlin.tools.kompot.api.intrinsics.ScopeMarkers.enterVersionScope
import org.jetbrains.kotlin.tools.kompot.api.intrinsics.ScopeMarkers.leaveVersionScope

inline fun <T> forVersion(v: String, l: () -> T): T? =
    if (enterVersionScope(v)) {
        val res: T = l()
        leaveVersionScope()
        res
    } else {
        null
    }