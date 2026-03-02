package io.github.pavelo8501.bootstrap.hocon.models

import kotlin.reflect.KProperty

data class EntryKey(
    val property: KProperty<*>,
){

    constructor(property: KProperty<*>, opts: Options<*>?): this(property){
        opts?.let {
            alias = it.alias
        }
    }
    val name: String = property.name
    var alias: String? = null

}