package com.lumokato.nunulo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickCaptureTest {
    private val readyCameraPhoto = DraftPhotoItem(
        photo = PhotoItem("photo-1"),
        status = "ready",
        captureSource = "camera",
    )

    @Test
    fun onlyReadyNewSingleCameraDraftCanQuickPublish() {
        assertTrue(UploadDraft(photos = listOf(readyCameraPhoto)).canQuickPublishPrivate())
        assertFalse(UploadDraft(editingId = "record-1", photos = listOf(readyCameraPhoto)).canQuickPublishPrivate())
        assertFalse(UploadDraft(photos = listOf(readyCameraPhoto.copy(captureSource = "gallery"))).canQuickPublishPrivate())
        assertFalse(UploadDraft(photos = listOf(readyCameraPhoto, readyCameraPhoto.copy(key = "second"))).canQuickPublishPrivate())
        assertFalse(UploadDraft(photos = listOf(readyCameraPhoto.copy(status = "uploading"))).canQuickPublishPrivate())
    }
}
