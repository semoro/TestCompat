package org.jetbrains.kotlin.tools.kompot.ssg

infix fun Int.hasFlag(i: Int) = this and i != 0
infix fun Int.noFlag(i: Int) = this and i == 0
