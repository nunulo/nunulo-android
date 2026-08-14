package com.lumokato.nunulo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileDataStateTest {
    @Test
    fun onlyCompletedExportStatesCanBeDownloaded() {
        assertTrue(export("available").canDownload())
        assertTrue(export("ready").canDownload())
        assertFalse(export("processing").canDownload())
        assertFalse(export("failed").canDownload())
    }

    @Test
    fun exportStatesUseUserFacingLabels() {
        assertEquals("可以保存或分享", exportStatusLabel("available"))
        assertEquals("正在生成", exportStatusLabel("processing"))
        assertEquals("生成失败，请重新生成", exportStatusLabel("failed"))
        assertEquals("状态未知", exportStatusLabel(""))
    }

    @Test
    fun exportTimestampKeepsTimeSoSameDayFilesRemainDistinct() {
        assertEquals("2026-08-14 12:00", exportCreatedLabel(export("available")))
        assertEquals("export-1", exportCreatedLabel(export("available").copy(createdAt = null)))
    }

    private fun export(status: String) = ExportItem(
        id = "export-1",
        status = status,
        createdAt = "2026-08-14T12:00:00Z",
        downloadUrl = null,
    )
}
