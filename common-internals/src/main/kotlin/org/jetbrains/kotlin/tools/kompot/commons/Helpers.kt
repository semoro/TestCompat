package org.jetbrains.kotlin.tools.kompot.commons

import kotlin.reflect.KMutableProperty0


inline fun <T, reified U: T> KMutableProperty0<T>.getOrInit(v: () -> U): U {
    return (get() ?: v().also { set(it) }) as U
}