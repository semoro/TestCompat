package org.jetbrains.kotlin.tools.kompat.commons

import org.jetbrains.kotlin.tools.kompat.api.annotations.AlternativeVisibility
import org.jetbrains.kotlin.tools.kompat.api.annotations.CompatibleWith
import org.jetbrains.kotlin.tools.kompat.api.annotations.ExistsIn
import org.jetbrains.kotlin.tools.kompat.api.annotations.Visibility
import org.objectweb.asm.Type
import kotlin.reflect.KClass

val existsInDesc = ExistsIn::class.asmType().descriptor
val compatibleWithDesc = CompatibleWith::class.asmType().descriptor
val altVisDesc = AlternativeVisibility::class.asmType().descriptor
val visEnumDesc = Visibility::class.asmType().descriptor


fun KClass<*>.asmType() = Type.getType(java)