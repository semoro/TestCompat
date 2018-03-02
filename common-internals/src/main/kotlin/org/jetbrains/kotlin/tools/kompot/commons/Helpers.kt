package org.jetbrains.kotlin.tools.kompot.commons

import kotlin.reflect.KMutableProperty0


inline fun <T, reified U : T> KMutableProperty0<T>.getOrInit(v: () -> U): U {
    return (get() ?: v().also { set(it) }) as U
}

inline fun <T> Array<T?>.getOrInit(index: Int, factory: () -> T): T {
    return this[index] ?: factory().also { this[index] = it }
}

inline fun <K, V> Iterable<Map.Entry<K, V>>.regroup() = groupBy({ (key) -> key }, { (_, value) -> value })

typealias MultiMap<K, V> = Map<K, List<V>>

inline infix fun <K, V> Map<K, V>.mergeToMultiMapWith(other: Map<K, V>): MultiMap<K, V> {
    return (this.entries + other.entries)
        .distinct()
        .regroup()
}
