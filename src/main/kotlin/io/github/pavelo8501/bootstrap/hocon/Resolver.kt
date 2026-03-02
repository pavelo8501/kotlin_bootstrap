package io.github.pavelo8501.bootstrap.hocon

import com.typesafe.config.Config
import io.github.pavelo8501.bootstrap.hocon.entry.HoconCommon
import io.github.pavelo8501.bootstrap.hocon.entry.HoconList
import io.github.pavelo8501.bootstrap.hocon.entry.HoconObject
import io.github.pavelo8501.bootstrap.hocon.entry.HoconProperty
import io.github.pavelo8501.bootstrap.hocon.models.EntryKey
import io.github.pavelo8501.bootstrap.hocon.models.ParseMetrics
import io.github.pavelo8501.bootstrap.hocon.models.ParserResult
import io.github.pavelo8501.component.Component
import io.github.pavelo8501.lognotify.extensions.warn
import io.github.pavelo8501.style.Colour
import io.github.pavelo8501.style.colorize
import io.github.pavelo8501.text.SpanBuilder
import io.github.pavelo8501.text.TextSpan
import io.github.pavelo8501.text.appendLineStyling
import io.github.pavelo8501.types.token.TypeToken
import kotlin.reflect.KProperty


/**
 * Resolves a HOCON configuration into a pre-constructed receiver instance.
 *
 * `HoconResolver` is a small registry + execution engine:
 * - You create it for a specific [receiver] instance and its [typeToken].
 * - Individual entries (properties/objects/lists) are registered into [entryMap].
 * - When [readConfig] is invoked, all registered entries are parsed from the provided [Config]
 *   and applied to the receiver via the entries' own `readConfig(...)` implementations.
 *
 * The resolver also tracks parsing progress and diagnostics via [nowParsing] ([ParseMetrics]):
 * - Captures config origin information
 * - Collects per-entry status text
 * - Registers and outputs errors on failure
 *
 * ### Registration model
 * Entries are stored by [EntryKey] in [entryMap]. Registration is typically performed by the
 * entry objects themselves once their [EntryKey] becomes available.
 *
 * ### Supported entry kinds
 * At runtime, entries are expected to be one of:
 * - [HoconProperty] – scalar or simple values
 * - [HoconObject]   – nested object binding
 * - [HoconList]     – list binding with element parsing
 *
 * ### Lookup helpers
 * Convenience `get(...)` operators allow retrieving a registered entry by:
 * - property name ([get] with `String`)
 * - [EntryKey]
 * - Kotlin reflection property ([KProperty])
 *
 * @param receiver the target instance that will be populated by parsing
 * @param typeToken runtime type information for the receiver used for logging/diagnostics
 */
class HoconResolver<T>(
   internal val receiver:T,
   internal val typeToken: TypeToken<T>,
): Component{

    override val name = "HoconResolver<${typeToken.typeName}>"
    private var nowParsing: ParseMetrics = ParseMetrics(this)
    internal val entryMap: MutableMap<EntryKey, HoconCommon<T>> = mutableMapOf()

    private val parsingSubject: (TypeToken<*>) -> String = { "Parsing ${it.typeName}" }

    private val propertyStatus : String get() {
        val stringBuilder = StringBuilder()
        entryMap.forEach { (key, entry) ->
            stringBuilder.append("Property $key :")
        }
        return stringBuilder.toString()
    }

    fun receiverStatus(): TextSpan{
        return buildSpan {
            appendLineStyling("Receiver: ", typeToken.typeName)
            appendLine(propertyStatuses())
        }
    }
    fun propertyStatuses(): TextSpan{
        val spanBuilder = SpanBuilder()
        entryMap.forEach { (key, entry) ->
            val styledKey = key.name.colorize(Colour.Magenta)
            spanBuilder.append("$styledKey ")
            if(key.alias != null){
                spanBuilder.append(key.alias)
            }
        }
        return spanBuilder.toSpan()
    }

    internal fun register(hoconEntry: HoconCommon<T>): Boolean {
        val key = hoconEntry.entryKey
        if(key != null){
            entryMap[key] = hoconEntry
            return true
        }
        warn("Key not ready $hoconEntry")
        return false
    }

    fun readConfig(hoconFactory: Config): ParserResult<T> {
        nowParsing.initialize(hoconFactory.origin())
        try {
            for (hoconEntry in entryMap.values) {
                when (hoconEntry) {
                    is HoconProperty<*, *> -> hoconEntry.readConfig(hoconFactory)
                    is HoconObject<*, *> -> hoconEntry.readConfig(hoconFactory)
                    is HoconList<*, *, *> -> hoconEntry.readConfig(hoconFactory)
                }
            }
            nowParsing.applyPropertyStatus(propertyStatuses())
        } catch (th: Throwable) {
            nowParsing.registerError(hoconFactory.origin(), th)
            nowParsing.output()
            throw th
        }
        return ParserResult(receiver, nowParsing)
    }

    operator fun get(propertyName: String): HoconCommon<T> {
        return  entryMap.keys.firstOrNull { it.name == propertyName }?.let {
            entryMap.getValue(it)
        }?:run {
            throw IllegalArgumentException("No enty with name $propertyName registered")
        }
    }
    operator fun get(entryKey: EntryKey): HoconCommon<T> {
        return entryMap.getValue(entryKey)
    }

    operator fun get(property: KProperty<*>): HoconCommon<T> {
        return  entryMap.keys.firstOrNull { it.property.name == property.name }?.let {
            entryMap.getValue(it)
        }?:run {
            throw IllegalArgumentException("No enty with property $property registered")
        }
    }

    companion object {
        inline operator fun <reified T> invoke(receiver :T): HoconResolver<T> {
          return  HoconResolver(receiver, TypeToken())
        }
    }
}



