package org.jetbrains.kotlin.tools.kompot.ssg

import org.objectweb.asm.Opcodes

val Int.presentableKind: String
    get() = when {
        this hasFlag Opcodes.ACC_ENUM -> "enum class"
        this hasFlag Opcodes.ACC_ANNOTATION -> "annotation class"
        this hasFlag Opcodes.ACC_INTERFACE -> "interface"
        this hasFlag Opcodes.ACC_ABSTRACT -> "abstract class"
        else -> "class"
    }

val Int.presentableVisibility: String
    get() = when {
        this hasFlag Opcodes.ACC_PUBLIC -> "public"
        this hasFlag Opcodes.ACC_PRIVATE -> "private"
        this hasFlag Opcodes.ACC_PROTECTED -> "protected"
        else -> "package-private"
    }