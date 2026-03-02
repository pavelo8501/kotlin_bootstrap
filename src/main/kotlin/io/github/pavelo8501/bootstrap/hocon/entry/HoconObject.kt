package io.github.pavelo8501.bootstrap.hocon.entry

import com.typesafe.config.Config
import io.github.pavelo8501.bootstrap.hocon.HoconResolvable
import io.github.pavelo8501.bootstrap.hocon.models.Options
import io.github.pavelo8501.bootstrap.hocon.models.OptionsElement
import io.github.pavelo8501.bootstrap.hocon.models.OptionsList
import io.github.pavelo8501.bootstrap.hocon.primitives.HoconClass
import io.github.pavelo8501.bootstrap.hocon.primitives.HoconPrimitives
import io.github.pavelo8501.debugging.requireOrFail
import io.github.pavelo8501.exceptions.error
import io.github.pavelo8501.types.token.TokenFactory
import io.github.pavelo8501.types.token.tokenOf
import kotlin.reflect.KProperty


class HoconObject<T, V>(
    val resolvable: HoconResolvable<V>,
    options: OptionsElement<V>? = null,
): HoconEntry<T, V>(options){

    override val hoconPrimitive: HoconClass<V> = HoconClass(resolvable.resolver.typeToken)

    override fun readConfig(config: Config){
        checkPath(config)
        val rowValue = config.getValue(name)
        val typeChecked = checkType(rowValue)
        requireOrFail(typeChecked){
            error<IllegalStateException>(it) {
                wrongTypeError(rowValue.valueType())
            }
        }
        val nestedConfig = config.getConfig(name)

        val result = resolvable.resolver.readConfig(nestedConfig)
        val receiver = result.receiver
        registerResult(receiver)

    }

    companion object: TokenFactory{
        inline operator fun <T, reified V> invoke(resolvable: HoconResolvable<V>):HoconObject<T, V>{
            return HoconObject(resolvable)
        }
    }
}