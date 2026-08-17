package com.lumokato.nunulo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PartnerRelationUiTest {
    @Test
    fun recordAuthorAndPartnerOwnerCanManageButUnrelatedViewerCannot() {
        val partner = partner(ownerUserId = 42)

        assertTrue(partner.relationUiState(recordAuthorUserId = 7, viewerUserId = 7).canManage)
        assertTrue(partner.relationUiState(recordAuthorUserId = 7, viewerUserId = 42).canManage)
        assertFalse(partner.relationUiState(recordAuthorUserId = 7, viewerUserId = 99).canManage)
        assertFalse(partner.relationUiState(recordAuthorUserId = 7, viewerUserId = null).canManage)
    }

    @Test
    fun relationPrivacyExplainsTheNextActionWithoutChangingPartnerPrivacy() {
        val public = partner(ownerUserId = 42, relationVisibility = "public").relationUiState(7, 7)
        val private = partner(ownerUserId = 42, relationVisibility = "private").relationUiState(7, 42)

        assertEquals("公开显示", public.visibilityLabel)
        assertEquals("private", public.nextVisibility)
        assertEquals("改为仅相关成员", public.visibilityActionLabel)
        assertEquals("仅相关成员", private.visibilityLabel)
        assertEquals("public", private.nextVisibility)
        assertEquals("改为公开显示", private.visibilityActionLabel)
    }

    @Test
    fun recordRelationUpdatesPreserveTheRestOfTheRecord() {
        val first = partner(ownerUserId = 42, relationVisibility = "public")
        val second = partner(id = "partner-2", ownerUserId = 9, relationVisibility = "public")
        val record = CheckinItem(
            id = "record-1",
            userId = 7,
            authorName = "记录作者",
            placeName = "",
            note = "保留正文",
            latitude = null,
            longitude = null,
            createdAt = null,
            takenAt = null,
            source = "android_capture",
            partners = listOf(first, second),
            likeCount = 8,
        )

        val hidden = record.withPartnerRelationVisibility(first.id, "private")
        val removed = hidden.withoutPartnerRelation(second.id)

        assertEquals("private", hidden.partners.first().relationVisibility)
        assertEquals("public", hidden.partners.last().relationVisibility)
        assertEquals(listOf(first.id), removed.partners.map(PartnerItem::id))
        assertEquals("保留正文", removed.note)
        assertEquals(8, removed.likeCount)
    }

    @Test
    fun partnerCollectionStatsUseDistinctVisibleRelationships() {
        val target = partner(ownerUserId = 42)
        val meeting = partner(id = "partner-2", ownerUserId = 9)
        val event = EventItem(
            id = "event-1",
            name = "MyGO!!!!! LIVE",
            eventType = "offline_live",
            visibility = "public",
            status = "active",
            official = true,
            place = null,
            series = null,
            startsAt = null,
            endsAt = null,
            description = "",
            recordCount = 2,
            canEdit = false,
        )
        val records = listOf(
            record("record-1", 7, "place-1", listOf(target, meeting), listOf(event)),
            record("record-2", 11, "place-2", listOf(target, meeting), listOf(event)),
        )

        assertEquals(
            PartnerCollectionStats(memberCount = 2, meetingPartnerCount = 1, placeCount = 2, eventCount = 1),
            partnerCollectionStats(target.id, records),
        )
    }

    private fun record(
        id: String,
        userId: Int,
        placeId: String,
        partners: List<PartnerItem>,
        events: List<EventItem>,
    ) = CheckinItem(
        id = id,
        userId = userId,
        placeId = placeId,
        placeName = "演出场地",
        note = "",
        latitude = 39.93,
        longitude = 116.44,
        createdAt = null,
        takenAt = null,
        source = "android_capture",
        partners = partners,
        events = events,
    )

    private fun partner(
        id: String = "partner-1",
        ownerUserId: Int,
        relationVisibility: String = "public",
    ) = PartnerItem(
        id = id,
        publicCode = "N-2026-08-000128",
        ownerUserId = ownerUserId,
        name = "高松灯棉花娃娃",
        visibility = "private",
        moderationStatus = "active",
        itemType = null,
        work = null,
        character = null,
        coverUrl = null,
        recordCount = 1,
        canEdit = false,
        relationVisibility = relationVisibility,
    )
}
