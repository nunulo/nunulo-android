package com.lumokato.nunulo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogSearchTest {
    private val aya = CatalogEntityItem(
        id = "aya",
        entityType = "character",
        canonicalName = "丸山彩",
        aliases = listOf("まるやま あや", "Aya Maruyama"),
        work = CatalogRef("bandori", "BanG Dream!"),
        group = CatalogRef("pastel-palettes", "Pastel＊Palettes"),
    )

    @Test fun searchesCanonicalJapaneseRomanizedAliasAndWork() {
        assertTrue(aya.matchesCatalogQuery("丸山"))
        assertTrue(aya.matchesCatalogQuery("まるやま"))
        assertTrue(aya.matchesCatalogQuery("aya"))
        assertTrue(aya.matchesCatalogQuery("bang dream"))
        assertTrue(aya.matchesCatalogQuery("pastel palettes"))
        assertFalse(aya.matchesCatalogQuery("Roselia"))
    }
}
