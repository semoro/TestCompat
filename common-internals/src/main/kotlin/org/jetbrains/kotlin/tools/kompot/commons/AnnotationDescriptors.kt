package org.jetbrains.kotlin.tools.kompot.commons

import org.jetbrains.kotlin.tools.kompot.api.annotations.*
import org.objectweb.asm.Type
import kotlin.reflect.KClass

val existsInDesc = ExistsIn::class.asmType().descriptor
val compatibleWithDesc = CompatibleWith::class.asmType().descriptor

val altVisDesc = AlternativeVisibility::class.asmType().descriptor
val visEnumDesc = Visibility::class.asmType().descriptor

val altModDesc = AlternativeModality::class.asmType().descriptor
val modEnumDesc = Modality::class.asmType().descriptor


fun KClass<*>.asmType() = Type.getType(java)