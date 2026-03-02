package io.github.pavelo8501.bootstrap.hocon.builders

import io.github.pavelo8501.bootstrap.hocon.HoconResolvable
import io.github.pavelo8501.bootstrap.hocon.HoconResolver
import io.github.pavelo8501.bootstrap.hocon.entry.HoconProperty
import io.github.pavelo8501.bootstrap.hocon.primitives.HoconPrimitives
import io.github.pavelo8501.component.Component
import io.github.pavelo8501.types.token.TypeToken

/**
 * Creates a [HoconResolver] bound to this instance using a reified runtime type token.
 *
 * Intended to reduce boilerplate in [HoconResolvable] implementations:
 *
 * ```
 * class Data : HoconResolvable<Data> {
 *     override val resolver = resolver()
 * }
 * ```
 *
 * @return a new [HoconResolver] bound to this receiver.
 */
inline fun <reified T> T.resolver(): HoconResolver<T> = HoconResolver(this)

/**
 * Creates a type-safe [HoconProperty] delegate for the reified value type [V].
 *
 * The primitive reader is resolved via [HoconPrimitives.typeToPrimitive] using [TypeToken] for [V].
 * This helper is intended for ergonomic property declarations inside [HoconResolvable] receivers.
 *
 * Example:
 * ```
 * class Data : HoconResolvable<Data> {
 *     override val resolver = resolver()
 *     val port by hoconProperty<Data, Int>()
 * }
 * ```
 *
 * @return a [HoconProperty] configured with the appropriate primitive parser for [V].
 * @throws IllegalStateException if [V] cannot be mapped to a supported HOCON primitive reader.
 */
inline fun <T, reified V>  T.hoconProperty(): HoconProperty<T, V> where T:  HoconResolvable<T>{
    val property = HoconProperty<T, V>(HoconPrimitives.typeToPrimitive(TypeToken<V>()))
    return property
}