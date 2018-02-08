package org.jetbrains.kotlin.tools.kompat.api.tool


interface VersionInfoProvider {

    fun forMethod(fqDescriptor: String): Version?
    fun forField(fqDescriptor: String): Version?
    fun forClass(fqDescriptor: String): Version?
}