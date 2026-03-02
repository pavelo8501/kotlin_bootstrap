package io.github.pavelo8501.bootstrap.hocon.setup

import io.github.pavelo8501.bootstrap.hocon.HoconResolvable
import io.github.pavelo8501.bootstrap.hocon.HoconResolver
import io.github.pavelo8501.bootstrap.hocon.builders.resolver
import io.github.pavelo8501.bootstrap.hocon.entry.HoconList
import io.github.pavelo8501.bootstrap.hocon.entry.HoconObject
import io.github.pavelo8501.bootstrap.hocon.entry.HoconProperty
import io.github.pavelo8501.bootstrap.hocon.primitives.HoconInt

abstract class HoconTestSuite {

    class IntData(): HoconResolvable<IntData>{
        override val resolver: HoconResolver<IntData> = HoconResolver(this)
        val intProperty by HoconProperty(HoconInt)
    }

    class Data(): HoconResolvable<Data>{
        override val resolver = resolver()
        val property by HoconProperty(HoconInt)
        val nested by HoconObject(IntData())
    }

    class ListData(): HoconResolvable<ListData>{
        override val resolver = resolver()
        val records : List<String> by HoconList()
    }
}