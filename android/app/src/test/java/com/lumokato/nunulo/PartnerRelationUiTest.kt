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
