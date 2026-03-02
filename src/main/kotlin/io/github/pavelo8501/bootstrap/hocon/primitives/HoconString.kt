package io.github.pavelo8501.bootstrap.hocon.primitives

import com.typesafe.config.ConfigValueType
import io.github.pavelo8501.types.token.TypeToken


object HoconString: HoconPrimitives<String> {
    override val typeToken:TypeToken<String> = TypeToken()
    override val hoconType:  ConfigValueType = ConfigValueType.STRING
}