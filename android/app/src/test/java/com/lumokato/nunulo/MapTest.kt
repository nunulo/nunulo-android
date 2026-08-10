package com.lumokato.nunulo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class MapTest {
    @Test
    fun convertsStoredWgs84CoordinatesForAmapRendering() {
        val point = toAmapPoint(39.901568, 116.422600)

        assertEquals(39.90297, point.latitude, 0.0001)
        assertEquals(116.42882, point.longitude, 0.0001)
    }

    @Test
    fun runtimeMapDoesNotContainTheRemovedShanghaiDefault() {
        val source = File("src/main/java/com/lumokato/nunulo/MainActivity.kt").readText()

        assertFalse(source.contains("LatLng(31.2304, 121.4737)"))
    }
}
