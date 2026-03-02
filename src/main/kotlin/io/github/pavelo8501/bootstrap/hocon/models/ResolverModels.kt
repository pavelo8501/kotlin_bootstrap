package io.github.pavelo8501.bootstrap.hocon.models

import com.typesafe.config.ConfigOrigin
import io.github.pavelo8501.bootstrap.hocon.HoconResolver
import io.github.pavelo8501.console.output
import io.github.pavelo8501.text.TextSpan


class ParserResult<T>(
    val receiver: T,
    val metrics: ParseMetrics
)

data class ParseMetrics(
    val parser: HoconResolver<*>,
){
    var resourceName: String = ""
    var url: java.net.URL? = null
    var description: String = ""
    var lineNumber: Int = 0

    val parsing: String get() = "$resourceName Line: $lineNumber"
    var properties: TextSpan? = null
    var errorMessage:String? = null
    val subEntries = mutableListOf<ParseMetrics>()

    val outputText: String get() {
       return buildString {
            appendLine(parser.name)
            errorMessage?.let {
                append("Parse error ")
                appendLine(it)
            }?:run {
                append("Parsing ")
                appendLine(parsing)
            }
            properties?.let {
                appendLine(it.styled)
            }
        }
    }

    fun initialize(conf: ConfigOrigin){
        resourceName = conf.resource()
        url = conf.url()
        description = conf.description()
        lineNumber = conf.lineNumber()
    }
    fun registerError(conf: ConfigOrigin, th: Throwable){
        errorMessage = "Error in  $resourceName Line: $lineNumber" + th.message
    }
    fun applyPropertyStatus(span: TextSpan){
        properties = span
    }
    fun add(metrics: ParseMetrics){
        subEntries.add(metrics)
    }

    fun output(){
        outputText.output()
    }
}

