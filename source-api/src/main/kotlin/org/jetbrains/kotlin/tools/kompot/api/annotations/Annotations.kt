package org.jetbrains.kotlin.tools.kompot.api.annotations

annotation class ExistsIn(val version: String)

annotation class CompatibleWith(val version: String)


enum class Visibility {
    PRIVATE, PROTECTED, PACKAGE_PRIVATE, PUBLIC
}

annotation class AlternativeVisibility(val version: Array<String>, val visibility: Array<Visibility>)

enum class Modality {
    FINAL, OPEN, ABSTRACT
}

annotation class AlternativeModality(val version: Array<String>, val modality: Array<Modality>)


annotation class MangledName(val original: String)