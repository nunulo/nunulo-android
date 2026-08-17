package com.lumokato.nunulo

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogCandidateSelectionTest {
    private val work = CatalogEntityItem(id = "work-1", entityType = "work", canonicalName = "BanG Dream!")
    private val group = CatalogEntityItem(id = "group-1", entityType = "group", canonicalName = "Ave Mujica", work = CatalogRef("work-1", "BanG Dream!"))
    private val character = CatalogEntityItem(
        id = "character-1",
        entityType = "character",
        canonicalName = "三角初华",
        status = "pending",
        work = CatalogRef("work-1", "BanG Dream!"),
        group = CatalogRef("group-1", "Ave Mujica"),
    )

    @Test
    fun createdCharacterIsSelectedWithItsHierarchy() {
        val result = selectCatalogCandidate(UploadDraft(), "", "character", character)

        assertEquals(listOf("work-1"), result.draft.workIds)
        assertEquals(listOf("character-1"), result.draft.characterIds)
        assertEquals("group-1", result.groupId)
    }

    @Test
    fun createdGroupSelectsItsWorkAndBecomesTheCharacterFilter() {
        val result = selectCatalogCandidate(UploadDraft(), "old-group", "group", group)

        assertEquals(listOf("work-1"), result.draft.workIds)
        assertEquals("group-1", result.groupId)
    }

    @Test
    fun pendingCandidateLabelIsExplicit() {
        assertEquals("三角初华 · 待审核", catalogSelectionLabel(character))
        assertEquals("BanG Dream!", catalogSelectionLabel(work))
    }
}
