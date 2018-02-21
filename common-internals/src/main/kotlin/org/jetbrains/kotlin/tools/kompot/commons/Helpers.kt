package org.jetbrains.kotlin.tools.kompot.commons

import kotlin.reflect.KMutableProperty0


inline fun <T, reified U : T> KMutableProperty0<T>.getOrInit(v: () -> U): U {
    return (get() ?: v().also { set(it) }) as U
}

inline fun <T> Array<T?>.getOrInit(index: Int, factory: () -> T): T {
    return this[index] ?: factory().also { this[index] = it }
}