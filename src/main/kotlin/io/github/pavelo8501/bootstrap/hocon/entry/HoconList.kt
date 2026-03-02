package io.github.pavelo8501.bootstrap.hocon.entry

import com.typesafe.config.Config
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueType
import io.github.pavelo8501.bootstrap.hocon.HoconResolvable
import io.github.pavelo8501.bootstrap.hocon.HoconResolver
import io.github.pavelo8501.bootstrap.hocon.models.EntryKey
import io.github.pavelo8501.bootstrap.hocon.models.Options
import io.github.pavelo8501.bootstrap.hocon.models.OptionsList
import io.github.pavelo8501.bootstrap.hocon.primitives.HoconPrimitiveList
import io.github.pavelo8501.bootstrap.hocon.primitives.HoconPrimitives
import io.github.pavelo8501.debugging.stacktrace.TraceOptions
import io.github.pavelo8501.delegates.PropertyDelegate
import io.github.pavelo8501.exceptions.error
import io.github.pavelo8501.types.cast.castOrThrow
import io.github.pavelo8501.types.token.TokenFactory
import io.github.pavelo8501.types.token.asEffectiveListType
import io.github.pavelo8501.types.token.asElementType
import io.github.pavelo8501.types.token.tokenOf
import kotlin.reflect.KProperty



class HoconList<T, E, V: List<E>>(
    val  hoconClassList: HoconPrimitiveList<E, V>,
    val options: OptionsList<E>? = null
): PropertyDelegate<HoconResolvable<T>, List<E>>(options?.defaultValue), HoconCommon<T>{

    override var entryKey: EntryKey? = null
    override val resolver: HoconResolver<T> get() = receiver.resolver

 //   override val hoconPrimitive : HoconPrimitives<E> = hoconClassList.primitive

    fun parseValue(rawValue: ConfigValue):E{
        val unwrapped = rawValue.unwrapped()
        val casted = unwrapped.castOrThrow(hoconClassList.typeToken.kClass)
        return casted
    }

    private fun registerResult(parsedResult: List<E>):List<E>{
        valueBacking = parsedResult
        return parsedResult
    }

    override fun readConfig(config: Config){
        checkPath(config)
        val rawValue = config.getValue(name)
        val valueType = rawValue.valueType()
        if(valueType !=  ConfigValueType.LIST){
            error<IllegalStateException>(TraceOptions.This) {
                wrongTypeError(valueType)
            }
        }
        val list =  rawValue as ConfigList
        val values = list.map { elementValue ->
            parseValue(elementValue)
        }
        valueBacking = values
    }

    override fun onProvideDelegate(thisRef: HoconResolvable<T>, property: KProperty<*>) {
        entryKey = EntryKey(property, options)
        resolver.register(this)
    }

    companion object: TokenFactory{

        inline operator fun <T, reified E, V: List<E>> invoke(
            options: OptionsList<E>? = null
        ):HoconList<T, E, V>{
            val token =  tokenOf<E>()
            val listToken =  token.asEffectiveListType()
            val useOpts = options?:run {
                OptionsList<E>(defaultValue = emptyList())
            }
            val primitive = HoconPrimitiveList<E, V>(listToken.asElementType())
            return HoconList(primitive, useOpts)
        }
    }
}