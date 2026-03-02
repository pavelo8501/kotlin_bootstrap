package io.github.pavelo8501.bootstrap.hocon.primitives

import com.typesafe.config.ConfigValueType
import io.github.pavelo8501.bootstrap.hocon.HoconResolvable
import io.github.pavelo8501.types.token.TypeToken


class HoconClass<T>(
  override val typeToken: TypeToken<T>
) : HoconPrimitives<T>{
    override val hoconType:  ConfigValueType = ConfigValueType.OBJECT
}


class HoconPrimitiveList<E, V: List<E>>(
    override val typeToken: TypeToken<E>
) : HoconPrimitives<E>{

    val primitive: HoconPrimitives<E>  = HoconPrimitives.typeToPrimitive(typeToken)
    override val hoconType:  ConfigValueType = ConfigValueType.LIST

}