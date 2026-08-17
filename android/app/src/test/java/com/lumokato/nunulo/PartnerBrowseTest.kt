package com.lumokato.nunulo

import org.junit.Assert.assertEquals
import org.junit.Test

class PartnerBrowseTest {
    @Test
    fun searchMatchesNameCodeAndCatalogRelations() {
        val partners = listOf(
            partner("灯", "N-000002", "BanG Dream!", "高松灯", 12),
            partner("小彩", "N-000001", "BanG Dream!", "丸山彩", 4),
        )

        assertEquals(listOf("灯"), filterPartners(partners, "n000002", PartnerVisibilityFilter.All).map(PartnerItem::name))
        assertEquals(listOf("小彩"), filterPartners(partners, "丸山彩", PartnerVisibilityFilter.All).map(PartnerItem::name))
        assertEquals(listOf("灯", "小彩"), filterPartners(partners, "bangdream", PartnerVisibilityFilter.All).map(PartnerItem::name))
    }

    @Test
    fun visibilityFilterAndRecordCountControlTheDirectoryOrder() {
        val partners = listOf(
            partner("私人伙伴", "N-3", "作品", "角色", 20, visibility = "private"),
            partner("常出镜", "N-2", "作品", "角色", 12),
            partner("偶尔出镜", "N-1", "作品", "角色", 2),
        )

        assertEquals(listOf("常出镜", "偶尔出镜"), filterPartners(partners, "", PartnerVisibilityFilter.Public).map(PartnerItem::name))
        assertEquals(listOf("私人伙伴"), filterPartners(partners, "", PartnerVisibilityFilter.Private).map(PartnerItem::name))
    }

    private fun partner(
        name: String,
        code: String,
        work: String,
        character: String,
        recordCount: Int,
        visibility: String = "public",
    ) = PartnerItem(
        id = code,
        publicCode = code,
        ownerUserId = 7,
        name = name,
        visibility = visibility,
        moderationStatus = "active",
        itemType = CatalogRef("plush", "棉花娃娃"),
        work = CatalogRef("work", work),
        character = CatalogRef("character", character),
        coverUrl = null,
        recordCount = recordCount,
        canEdit = true,
    )
}
