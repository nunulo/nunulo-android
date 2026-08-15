package com.lumokato.nunulo

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogBrowseTest {
    private val work = CatalogEntityItem(
        id = "bandori",
        entityType = "work",
        canonicalName = "BanG Dream!",
        aliases = listOf("BanG Dream! 少女乐团派对！"),
        followed = true,
        recordCount = 128,
    )
    private val group = CatalogEntityItem(
        id = "mygo",
        entityType = "group",
        canonicalName = "MyGO!!!!!",
        aliases = listOf("迷子"),
        recordCount = 42,
        work = CatalogRef("bandori", "BanG Dream!"),
    )
    private val character = CatalogEntityItem(
        id = "tomori",
        entityType = "character",
        canonicalName = "高松灯",
        aliases = listOf("高松 燈", "Takamatsu Tomori"),
        recordCount = 18,
        work = CatalogRef("bandori", "BanG Dream!"),
        group = CatalogRef("mygo", "MyGO!!!!!"),
    )

    @Test
    fun browseSearchesHierarchyAndAppliesTypeAndFollowFilters() {
        val catalog = mapOf("work" to listOf(work), "group" to listOf(group), "character" to listOf(character))

        assertEquals(
            listOf("bandori", "mygo", "tomori"),
            catalogBrowseItems(catalog, "BanG Dream", CatalogTypeFilter.All, CatalogFollowFilter.All).map(CatalogEntityItem::id),
        )
        assertEquals(
            listOf("tomori"),
            catalogBrowseItems(catalog, "Takamatsu", CatalogTypeFilter.Character, CatalogFollowFilter.All).map(CatalogEntityItem::id),
        )
        assertEquals(
            listOf("bandori"),
            catalogBrowseItems(catalog, "", CatalogTypeFilter.All, CatalogFollowFilter.Followed).map(CatalogEntityItem::id),
        )
    }

    @Test
    fun browseDeduplicatesEntriesAndKeepsFollowedThenPopularOrder() {
        val catalog = mapOf("work" to listOf(work, work), "group" to listOf(group), "character" to listOf(character))

        assertEquals(
            listOf("bandori", "mygo", "tomori"),
            catalogBrowseItems(catalog, "", CatalogTypeFilter.All, CatalogFollowFilter.All).map(CatalogEntityItem::id),
        )
    }

    @Test
    fun collectionStatsUseOnlyDistinctVisibleRelationships() {
        val partner = PartnerItem("partner-1", "N-1", 7, "灯", "public", "active", null, null, null, null, 2, false)
        val event = EventItem("event-1", "MyGO!!!!! LIVE", "offline_live", "public", "active", true, null, null, null, null, "", 2, false)
        val first = record("record-1", 7, "北京工人体育场", 39.93, 116.44, listOf(partner), listOf(event))
        val second = record("record-2", 7, "北京工人体育场", 39.93, 116.44, listOf(partner), listOf(event))
        val third = record("record-3", 9, "", 31.23, 121.47, emptyList(), emptyList())

        assertEquals(
            CatalogCollectionStats(memberCount = 2, partnerCount = 1, placeCount = 2, eventCount = 1),
            catalogCollectionStats(listOf(first, second, third)),
        )
    }

    @Test
    fun recordDetailCatalogLinksReuseTheFormalEntityPageWhenAvailable() {
        val catalog = mapOf("work" to listOf(work), "group" to listOf(group), "character" to listOf(character))

        val collection = catalogCollectionForRef(catalog, "character", CatalogRef("tomori", "高松灯"))

        assertEquals(character, collection.catalogEntity)
        assertEquals("高松灯", collection.title)
        assertEquals(mapOf("character_id" to "tomori"), collection.filters)
    }

    @Test
    fun recordDetailCatalogLinksKeepAUsableFallbackForOlderCatalogResponses() {
        val collection = catalogCollectionForRef(emptyMap(), "work", CatalogRef("legacy-work", "旧作品"))

        assertEquals(null, collection.catalogEntity)
        assertEquals("旧作品", collection.title)
        assertEquals("作品", collection.subtitle)
        assertEquals(mapOf("work_id" to "legacy-work"), collection.filters)
    }

    private fun record(
        id: String,
        userId: Int,
        placeName: String,
        latitude: Double?,
        longitude: Double?,
        partners: List<PartnerItem>,
        events: List<EventItem>,
    ) = CheckinItem(
        id = id,
        userId = userId,
        placeName = placeName,
        note = "",
        latitude = latitude,
        longitude = longitude,
        createdAt = null,
        takenAt = null,
        source = "android_capture",
        partners = partners,
        events = events,
    )
}
