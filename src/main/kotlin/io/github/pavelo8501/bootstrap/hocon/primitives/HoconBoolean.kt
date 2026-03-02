package io.github.pavelo8501.bootstrap.hocon.primitives

import com.typesafe.config.ConfigValueType
import io.github.pavelo8501.types.token.TypeToken



object HoconBoolean : HoconPrimitives<Boolean> {
    override val typeToken:TypeToken<Boolean> = TypeToken()
    override val hoconType:  ConfigValueType = ConfigValueType.BOOLEAN
}