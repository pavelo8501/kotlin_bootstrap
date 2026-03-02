package io.github.pavelo8501.bootstrap.hocon.primitives

import com.typesafe.config.ConfigValueType
import io.github.pavelo8501.types.token.TypeToken


object HoconInt : HoconPrimitives<Int> {
    override val typeToken:TypeToken<Int> = TypeToken()
    override val hoconType:  ConfigValueType = ConfigValueType.NUMBER
}