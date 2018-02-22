package org.jetbrains.kotlin.tools.kompot.ssg

data class Configuration(
    val loadParameterNamesFromLVT: Boolean = false,
    val writeParametersToLVT: Boolean = loadParameterNamesFromLVT,
    val writeParameters: Boolean = true
)