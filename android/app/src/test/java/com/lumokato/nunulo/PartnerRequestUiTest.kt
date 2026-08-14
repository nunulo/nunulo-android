package com.lumokato.nunulo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PartnerRequestUiTest {
    @Test
    fun partnerOwnerSeesWhoRequestedAndMustDecide() {
        val state = request(authorApproved = true, ownerApproved = false).uiState(viewerUserId = 42)

        assertTrue(state.needsDecision)
        assertEquals("需要你确认", state.statusLabel)
        assertEquals("旅行收藏者 希望在记录中登记你的伙伴 高松灯棉花娃娃。", state.description)
    }

    @Test
    fun recordAuthorWhoAlreadyApprovedWaitsWithoutDuplicateActions() {
        val state = request(authorApproved = true, ownerApproved = false).uiState(viewerUserId = 7)

        assertFalse(state.needsDecision)
        assertEquals("等待对方确认", state.statusLabel)
        assertEquals("你已确认把 高松灯棉花娃娃 登记到这条记录，正在等待 小灯的主人 确认。", state.description)
    }

    @Test
    fun unrelatedViewerCannotResolveRequest() {
        val state = request(authorApproved = false, ownerApproved = false).uiState(viewerUserId = 99)

        assertFalse(state.needsDecision)
        assertEquals("等待相关成员确认", state.statusLabel)
    }

    private fun request(authorApproved: Boolean, ownerApproved: Boolean) = PartnerRequestItem(
        checkinId = "checkin-1",
        partnerId = "partner-1",
        partnerName = "高松灯棉花娃娃",
        partnerCode = "N-2026-08-000128",
        recordAuthorUserId = 7,
        recordAuthorDisplayName = "旅行收藏者",
        partnerOwnerUserId = 42,
        partnerOwnerDisplayName = "小灯的主人",
        authorApproved = authorApproved,
        ownerApproved = ownerApproved,
    )
}
