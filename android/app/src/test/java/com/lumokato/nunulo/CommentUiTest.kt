package com.lumokato.nunulo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentUiTest {
    @Test
    fun `only comment author or record author gets daily-app delete control`() {
        assertTrue(canDeleteComment(viewerUserId = 2, recordUserId = 1, commentUserId = 2))
        assertTrue(canDeleteComment(viewerUserId = 1, recordUserId = 1, commentUserId = 2))
        assertFalse(canDeleteComment(viewerUserId = 3, recordUserId = 1, commentUserId = 2))
        assertFalse(canDeleteComment(viewerUserId = null, recordUserId = 1, commentUserId = 2))
    }
}
