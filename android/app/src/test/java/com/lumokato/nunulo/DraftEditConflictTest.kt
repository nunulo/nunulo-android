package com.lumokato.nunulo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftEditConflictTest {
    @Test
    fun newDraftWithPhotosConflictsWithEditingPublishedRecord() {
        val draft = UploadDraft(photos = listOf(DraftPhotoItem(key = "local-photo")))

        assertTrue(draft.conflictsWithRecordEdit("record-1"))
    }

    @Test
    fun anotherPublishedRecordEditDraftConflicts() {
        val draft = UploadDraft(editingId = "record-1", photos = listOf(DraftPhotoItem(key = "photo-1")))

        assertTrue(draft.conflictsWithRecordEdit("record-2"))
    }

    @Test
    fun emptyDraftOrSameRecordEditDoesNotConflict() {
        assertFalse(UploadDraft().conflictsWithRecordEdit("record-1"))
        assertFalse(
            UploadDraft(
                editingId = "record-1",
                photos = listOf(DraftPhotoItem(key = "photo-1")),
            ).conflictsWithRecordEdit("record-1")
        )
    }
}
