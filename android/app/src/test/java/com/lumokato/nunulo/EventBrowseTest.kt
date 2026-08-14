package com.lumokato.nunulo

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventBrowseTest {
    private val now = Instant.parse("2026-08-14T12:00:00Z")

    @Test
    fun searchMatchesEventPlaceSeriesAndDescription() {
        val live = event(name = "MyGO!!!!! 7th LIVE", placeName = "北京工人体育场", seriesName = "MyGO!!!!! 巡演", description = "和伙伴看现场")

        assertEquals(listOf(live.id), eventBrowseItems(listOf(live), "工人体育场", EventPeriodFilter.All, EventKindFilter.All, now).map(EventItem::id))
        assertEquals(listOf(live.id), eventBrowseItems(listOf(live), "巡演", EventPeriodFilter.All, EventKindFilter.All, now).map(EventItem::id))
        assertEquals(listOf(live.id), eventBrowseItems(listOf(live), "伙伴", EventPeriodFilter.All, EventKindFilter.All, now).map(EventItem::id))
    }

    @Test
    fun periodAndKindFiltersKeepArchivesSeparate() {
        val upcoming = event(id = "upcoming", startsAt = "2026-08-16T10:00:00Z")
        val past = event(id = "past", startsAt = "2026-08-01T10:00:00Z", endsAt = "2026-08-01T12:00:00Z")
        val online = event(id = "online", type = "online_birthday", startsAt = null)
        val events = listOf(past, online, upcoming)

        assertEquals(listOf(upcoming.id, online.id), eventBrowseItems(events, "", EventPeriodFilter.Upcoming, EventKindFilter.All, now).map(EventItem::id))
        assertEquals(listOf(past.id), eventBrowseItems(events, "", EventPeriodFilter.Past, EventKindFilter.All, now).map(EventItem::id))
        assertEquals(listOf(online.id), eventBrowseItems(events, "", EventPeriodFilter.All, EventKindFilter.Online, now).map(EventItem::id))
        assertEquals(listOf(upcoming.id, past.id), eventBrowseItems(events, "", EventPeriodFilter.All, EventKindFilter.Offline, now).map(EventItem::id))
    }

    @Test
    fun periodLabelsDistinguishUpcomingOngoingPastAndUndated() {
        assertEquals("即将开始", event(startsAt = "2026-08-16T10:00:00Z").periodLabel(now))
        assertEquals("进行中", event(startsAt = "2026-08-14T10:00:00Z", endsAt = "2026-08-14T14:00:00Z").periodLabel(now))
        assertEquals("往期", event(startsAt = "2026-08-01T10:00:00Z").periodLabel(now))
        assertEquals("日期待定", event(startsAt = null).periodLabel(now))
        assertTrue(event(startsAt = "2026-08-01T10:00:00Z").isPast(now))
        assertFalse(event(startsAt = null).isPast(now))
    }

    @Test
    fun eventCollectionStatsDeduplicateMembersPartnersAndCharacters() {
        val partner = PartnerItem("partner-1", "N-1", 7, "灯", "public", "active", null, null, null, null, 2, false)
        val first = record("record-1", 7, listOf(partner), listOf(CatalogRef("tomori", "高松灯")))
        val second = record("record-2", 7, listOf(partner), listOf(CatalogRef("tomori", "高松灯")))
        val third = record("record-3", 9, emptyList(), listOf(CatalogRef("anon", "千早爱音")))

        assertEquals(EventCollectionStats(partnerCount = 1, characterCount = 2, memberCount = 2), eventCollectionStats(listOf(first, second, third)))
    }

    private fun event(
        id: String = "event-1",
        name: String = "线下 Live",
        type: String = "offline_live",
        placeName: String? = null,
        seriesName: String? = null,
        startsAt: String? = "2026-08-16T10:00:00Z",
        endsAt: String? = null,
        description: String = "",
    ) = EventItem(
        id = id,
        name = name,
        eventType = type,
        visibility = "public",
        status = "active",
        official = false,
        place = placeName?.let { PlaceItem("place-1", it, latitude = 39.9, longitude = 116.4) },
        series = seriesName?.let { EventSeriesItem("series-1", it, "active") },
        startsAt = startsAt,
        endsAt = endsAt,
        description = description,
        recordCount = 0,
        canEdit = false,
    )

    private fun record(id: String, userId: Int, partners: List<PartnerItem>, characters: List<CatalogRef>) = CheckinItem(
        id = id,
        userId = userId,
        placeName = "",
        note = "",
        latitude = null,
        longitude = null,
        createdAt = null,
        takenAt = null,
        source = "android_capture",
        partners = partners,
        characters = characters,
    )
}
