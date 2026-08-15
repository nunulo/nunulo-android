package com.lumokato.nunulo

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegionBrowseTest {
    @Test
    fun `region places group by formal place and exclude records without coordinates`() {
        val records = listOf(
            record("record-1", "place-a", "北京工人体育场", 39.9305, 116.4469),
            record("record-2", "place-a", "北京工人体育场北门", 39.9306, 116.4470),
            record("record-3", "place-b", "国家体育馆", 39.9992, 116.3907),
            record("record-4", null, "没有坐标的旧记录", null, null),
        )

        val places = regionCollectionPlaces(records)

        assertEquals(listOf("place-a", "place-b"), places.map(RegionPlaceSummary::id))
        assertEquals(2, places.first().recordCount)
        assertEquals("record-1", places.first().representativeRecordId)
        assertEquals("北京工人体育场", places.first().name)
    }

    @Test
    fun `region places keep backward compatible coordinate fallback without a place id`() {
        val places = regionCollectionPlaces(
            listOf(
                record("record-1", null, "工体北门", 39.93, 116.44),
                record("record-2", null, "工体北门", 39.93, 116.44),
            )
        )

        assertEquals(1, places.size)
        assertEquals(2, places.single().recordCount)
    }

    @Test
    fun `checkin parser keeps formal place identity when the api provides it`() {
        val parsed = parseCheckin(
            JSONObject(
                """{"id":"record-1","user_id":7,"place":{"id":"place-a","name":"工体北门"},"place_name":"工体北门","latitude":39.93,"longitude":116.44,"note":"","source":"android_capture"}"""
            )
        )
        val legacy = parseCheckin(
            JSONObject(
                """{"id":"record-2","user_id":7,"place_name":"旧地点","latitude":39.93,"longitude":116.44,"note":"","source":"android_capture"}"""
            )
        )

        assertEquals("place-a", parsed.placeId)
        assertNull(legacy.placeId)
    }

    private fun record(
        id: String,
        placeId: String?,
        placeName: String,
        latitude: Double?,
        longitude: Double?,
    ) = CheckinItem(
        id = id,
        userId = 7,
        placeId = placeId,
        placeName = placeName,
        note = "",
        latitude = latitude,
        longitude = longitude,
        createdAt = null,
        takenAt = null,
        source = "android_capture",
    )
}
