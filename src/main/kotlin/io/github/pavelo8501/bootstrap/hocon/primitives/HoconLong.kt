package io.github.pavelo8501.bootstrap.hocon.primitives

import com.typesafe.config.ConfigValueType
import io.github.pavelo8501.types.token.TypeToken


object HoconLong: HoconPrimitives<Long> {
    override val typeToken:TypeToken<Long> = TypeToken()
    override val hoconType:  ConfigValueType = ConfigValueType.STRING
}