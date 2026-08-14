package com.lumokato.nunulo

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationUiTest {
    @Test
    fun `notification targets use user-facing categories`() {
        assertEquals("记录互动", notificationKindLabel("checkin"))
        assertEquals("伙伴关系", notificationKindLabel("checkin_partner"))
        assertEquals("账号", notificationKindLabel("user"))
        assertEquals("照片与存储", notificationKindLabel("photo"))
        assertEquals("系统消息", notificationKindLabel("unknown"))
    }
}
