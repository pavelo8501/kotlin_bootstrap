package io.github.pavelo8501.bootstrap.hocon.primitives

import com.typesafe.config.ConfigValueType
import io.github.pavelo8501.component.Component
import io.github.pavelo8501.types.token.TypeToken

sealed interface HoconPrimitives<V>{
    val typeToken: TypeToken<V>
    val hoconType: ConfigValueType
    companion object: Component {
        fun <V> typeToPrimitive(typeToken: TypeToken<V>): HoconPrimitives<V> {
            @Suppress("UNCHECKED_CAST")
            return when (typeToken.kClass) {
                String::class -> HoconString as HoconPrimitives<V>
                Long::class -> HoconLong as HoconPrimitives<V>
                Int::class -> HoconInt as HoconPrimitives<V>
                Boolean::class -> HoconBoolean as HoconPrimitives<V>
                else -> throw IllegalArgumentException("Unable to find hocon primitive for type ${typeToken.typeName}")
            }
        }
    }
}