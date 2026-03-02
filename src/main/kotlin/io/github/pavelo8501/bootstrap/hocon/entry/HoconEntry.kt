package io.github.pavelo8501.bootstrap.hocon.entry

import com.typesafe.config.Config
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueType
import io.github.pavelo8501.bootstrap.hocon.HoconResolvable
import io.github.pavelo8501.bootstrap.hocon.HoconResolver
import io.github.pavelo8501.bootstrap.hocon.exceptions.ParseError
import io.github.pavelo8501.bootstrap.hocon.models.EntryKey
import io.github.pavelo8501.bootstrap.hocon.models.Options
import io.github.pavelo8501.bootstrap.hocon.models.OptionsList
import io.github.pavelo8501.bootstrap.hocon.primitives.HoconPrimitiveList
import io.github.pavelo8501.bootstrap.hocon.primitives.HoconPrimitives
import io.github.pavelo8501.debugging.handler.outputReporting
import io.github.pavelo8501.debugging.requireOrFail
import io.github.pavelo8501.debugging.stacktrace.TraceOptions
import io.github.pavelo8501.delegates.PropertyDelegate
import io.github.pavelo8501.exceptions.error
import io.github.pavelo8501.types.cast.castOrThrow
import io.github.pavelo8501.types.cast.safeCast
import io.github.pavelo8501.types.token.TokenFactory
import io.github.pavelo8501.types.token.TypeToken
import io.github.pavelo8501.types.token.tokenOf
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf


sealed interface  HoconCommon<T>{

    val resolver: HoconResolver<T>
    val name :String
    val isMarkedNullable:Boolean
    var entryKey: EntryKey?


    val errorHeader: String get() {
        return "Resolver: ${resolver.name}"
    }

    val nullabilityErrorMsg: String get() {
        return buildString {
            appendLine(errorHeader)
            appendLine("Config does not contain property $name. Which is marked non nullable ")
        }
    }

    val wrongTypeError : (ConfigValueType)-> String get() =  {
        "Property $name is expected to be of type ${resolver.typeToken.typeName}. Got $it"
    }

    fun readConfig(config: Config)

    fun checkPath(config: Config){
        val hasPath = config.hasPath(name)
        if(!hasPath && !isMarkedNullable){
            val th =  ParseError(nullabilityErrorMsg)
            th.outputReporting(TraceOptions.This)
            throw th
        }
    }
}

sealed class HoconEntry<T, V>(
    val options: Options<V>?
):PropertyDelegate<HoconResolvable<T>, V>(options?.defaultValue), HoconCommon<T>{

    abstract  val hoconPrimitive: HoconPrimitives<V>
    override val resolver: HoconResolver<T> get() = receiver.resolver
    val valueTypeToken: TypeToken<V> get() = hoconPrimitive.typeToken
    override var entryKey: EntryKey? = null

    val value:V? get() = valueBacking

    override fun onProvideDelegate(thisRef:HoconResolvable<T>, property: KProperty<*>){
        entryKey = EntryKey(property, options)
        resolver.register(this)
    }

    protected fun checkType(valueType: ConfigValueType): Boolean{
        return valueType == hoconPrimitive.hoconType
    }

    protected fun parseNumericValue(config: Config, valueType: ConfigValueType): V {
        return when(hoconPrimitive.typeToken.kClass){
            Long::class -> config.getLong(name).castOrThrow(valueTypeToken.kClass)
            Int::class -> config.getInt(name).castOrThrow(hoconPrimitive.typeToken.kClass)
            Double::class -> config.getDouble(name).castOrThrow(hoconPrimitive.typeToken.kClass)
            else -> throw IllegalStateException(wrongTypeError(valueType))
        }
    }

    protected fun parseValue(rawValue: ConfigValue):V {
        val unwrapped = rawValue.unwrapped()
        val casted = unwrapped.castOrThrow(hoconPrimitive.typeToken.kClass)
        return casted
    }

    protected fun checkType(rawValue:  ConfigValue): Boolean{
        return rawValue.valueType() == hoconPrimitive.hoconType
    }

    protected open fun registerResult(parsedResult:V):V{
        valueBacking = parsedResult
        return parsedResult
    }
}

class HoconProperty<T, V>(
    override val  hoconPrimitive: HoconPrimitives<V>,
    options: Options<V>? = null
): HoconEntry<T, V>(options){

    override fun readConfig(config: Config){
        checkPath(config)
        val rawValue = config.getValue(name)
        val valueType = rawValue.valueType()
        val typeChecked = checkType(valueType)
        requireOrFail(typeChecked){
            error<IllegalStateException>(it){
                wrongTypeError(valueType)
            }
        }
       val  parsedValue =  if(valueTypeToken.kClass.isSubclassOf(Number::class)){
            parseNumericValue(config, valueType)
        }else{
            parseValue(rawValue)
        }
        registerResult(parsedValue)
    }

    companion object: TokenFactory{
        inline operator fun <T, reified V> invoke(
            options: Options<V>? = null
        ):HoconProperty<T, V>{
            val token =  tokenOf<V>()
            val primitive = HoconPrimitives.typeToPrimitive(token)
            return HoconProperty<T, V>(primitive, options)
        }
    }
}