package io.github.pavelo8501.bootstrap.hocon.models


interface Options<V>{
    val defaultValue: V?
    val alias: String?
}

class OptionsElement<V>(
    override val alias: String? = null,
    override val defaultValue: V? = null,
): Options<V>{

}

class OptionsList<V>(
    override val alias: String? = null,
    override val defaultValue: List<V>? = null,
) : Options<List<V>>{

}