package com.lumokato.nunulo

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceSelectionTest {
    private val stadium = PlaceItem(
        id = "stadium",
        name = "北京工人体育场",
        province = "北京",
        city = "北京市",
        district = "朝阳区",
        latitude = 39.9305,
        longitude = 116.4469,
        address = "工人体育场北路",
    )

    @Test
    fun placeSearchMatchesNameRegionAndAddress() {
        assertEquals(listOf("stadium"), placeSelectionItems(listOf(stadium), "工人体育场").map(PlaceItem::id))
        assertEquals(listOf("stadium"), placeSelectionItems(listOf(stadium), "朝阳区").map(PlaceItem::id))
        assertEquals(listOf("stadium"), placeSelectionItems(listOf(stadium), "北路").map(PlaceItem::id))
    }

    @Test
    fun exactDuplicatePlacesDoNotCreateDuplicateChoices() {
        val duplicate = stadium.copy(id = "duplicate")

        assertEquals(listOf("stadium"), placeSelectionItems(listOf(stadium, duplicate), "").map(PlaceItem::id))
    }
}
