package io.github.pavelo8501.bootstrap.hocon

import com.typesafe.config.ConfigFactory
import io.github.pavelo8501.bootstrap.hocon.builders.hoconProperty
import io.github.pavelo8501.bootstrap.hocon.builders.resolver
import io.github.pavelo8501.bootstrap.hocon.entry.HoconObject
import io.github.pavelo8501.bootstrap.hocon.entry.HoconProperty
import io.github.pavelo8501.bootstrap.hocon.extensions.applyConfig
import io.github.pavelo8501.bootstrap.hocon.primitives.HoconInt
import io.github.pavelo8501.bootstrap.hocon.primitives.HoconString
import io.github.pavelo8501.types.token.TypeToken
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HoconParserTest {

    private class StringData(): HoconResolvable<StringData>{
        override val resolver: HoconResolver<StringData> = HoconResolver(this)
        val property by HoconProperty(HoconString)
    }
    private class IntData(): HoconResolvable<IntData>{
        override val resolver: HoconResolver<IntData> = HoconResolver(this)
        val intProperty by HoconProperty(HoconInt)
    }

    private class Data(): HoconResolvable<Data>{
        override val resolver = resolver()
        val property by HoconProperty(HoconInt)
        val nested by HoconObject(IntData())
    }

    @Test
    fun `Properties are registered as expected`(){
        val data = StringData()
        assertNotNull(data.resolver["property"])
    }

    @Test
    fun `Properties of type string are parsing as expected`(){
        val factory = ConfigFactory.load().getConfig("property_string")
        val data = StringData()
        data.applyConfig(factory)
        assertNotNull(data.property)
    }

    @Test
    fun `Properties of type int are parsing as expected`(){
        val factory = ConfigFactory.load().getConfig("property_int")
        val data = IntData()
        data.applyConfig(factory)
        assertNotNull(data.intProperty)
    }

    @Test
    fun `Properties of composite type are parsing as expected`(){
        val factory = ConfigFactory.load().getConfig("nested_config")
        val data = Data()
        data.applyConfig(factory)
        assertEquals(20, data.property)
        val nestedConfig = assertDoesNotThrow { data.nested }
        assertEquals(10, nestedConfig.intProperty)
    }

    @Test
    fun `Parse metrics`(){
        val factory = ConfigFactory.load().getConfig("nested_config")
        val data = Data()
        val result =  data.applyConfig(factory)
        result.metrics.output()
    }
}