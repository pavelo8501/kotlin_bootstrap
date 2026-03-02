package io.github.pavelo8501.bootstrap.hocon

import io.github.pavelo8501.bootstrap.hocon.extensions.applyConfig
import io.github.pavelo8501.bootstrap.hocon.extensions.getTypeSafeConfig
import io.github.pavelo8501.bootstrap.hocon.setup.HoconTestSuite
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HoconListTest : HoconTestSuite(){

    @Test
    fun `Type is inferred correctly`(){
        val listData = ListData()
        assertNotNull(listData.records)
        val entry = assertDoesNotThrow{
            listData.resolver[listData::records]
        }
        assertNotNull(entry)
    }

    @Test
    fun `Config parsed correctly`(){
        val config =  getTypeSafeConfig("list_config")
        val listData = ListData()
        listData.applyConfig(config)
        assertEquals(2, listData.records.size)
        val first =  listData.records.first()
        assertEquals("record1", first)

        val second =  listData.records[1]
        assertEquals("record2", second)

    }
}