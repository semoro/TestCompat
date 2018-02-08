package org.jetbrains.kotlin.tools.kompat.api.annotations

annotation class ExistsIn(val version: String)

annotation class CompatibleWith(val version: String)


enum class Visibility {
    PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE
}

annotation class AlternativeVisibility(val version: Array<String>, val visibility: Array<Visibility>)

enum class Modality {
    FINAL, OPEN, ABSTRACT
}

annotation class AlternativeModality(val version: Array<String>, val modality: Array<Modality>)


annotation class MangledName(val original: String)