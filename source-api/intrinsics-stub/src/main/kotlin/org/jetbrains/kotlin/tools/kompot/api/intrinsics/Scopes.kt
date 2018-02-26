package org.jetbrains.kotlin.tools.kompot.api.intrinsics

object ScopeMarkers {

    @JvmStatic
    fun enterVersionScope(v: String): Boolean {
        return false
    }

    @JvmStatic
    fun leaveVersionScope() {}

}