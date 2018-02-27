package org.jetbrains.kotlin.tools.kompot.postprocess

interface MangledNamesInfoProvider {
    fun forMethod(owner: String, name: String, desc: String): String?
}