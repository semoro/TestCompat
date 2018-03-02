package org.jetbrains.kotlin.tools.kompot.api.source


inline fun <T> forVersion(v: String, l: () -> T): T? = l()