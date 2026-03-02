package io.github.pavelo8501.bootstrap.hocon

import io.github.pavelo8501.bootstrap.hocon.builders.hoconProperty
import io.github.pavelo8501.bootstrap.hocon.entry.HoconObject
import io.github.pavelo8501.bootstrap.hocon.entry.HoconProperty
import io.github.pavelo8501.bootstrap.hocon.extensions.getTypeSafeConfig
import io.github.pavelo8501.bootstrap.hocon.primitives.HoconBoolean
import io.github.pavelo8501.bootstrap.hocon.primitives.HoconString
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class HoconResolutionTest {

    private class Data(): HoconResolvable<Data>{
        override val resolver: HoconResolver<Data> = HoconResolver(this)
        val property by HoconProperty(HoconString)
        val property2 : String by HoconProperty()
        val booleanProperty: Boolean by hoconProperty()
    }

    private class ComplexData(): HoconResolvable<ComplexData>{
        override val resolver: HoconResolver<ComplexData> = HoconResolver(this)
        val property : Int by HoconProperty()
        val nested by HoconObject(Data())
    }

    @Test
    fun `Config helper work as expected`(){
        val config =  getTypeSafeConfig("property_int")
        assertFalse {
            config.isEmpty
        }

    }

    @Test
    fun `Both initializations work the same`(){
        val data = Data()
        assertNotNull(data.resolver["property"])
        assertNotNull(data.resolver["property2"]){entry->
            assertIs<HoconProperty<Data, String>>(entry)
            assertIs<HoconString>(entry.hoconPrimitive)
        }
    }

    @Test
    fun `Properties of complex type are parsing as expected`(){
        val complexData = ComplexData()
        assertNotNull(complexData.resolver["nested"])
    }

    @Test
    fun `Properties by attached builder`(){
        val data = Data()
        assertNotNull(data.resolver["booleanProperty"]) { entry ->
            assertIs<HoconProperty<Data, Boolean>>(entry)
            assertIs<HoconBoolean>(entry.hoconPrimitive)
        }
    }
}