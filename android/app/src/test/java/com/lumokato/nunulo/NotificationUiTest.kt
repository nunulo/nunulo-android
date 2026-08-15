package com.lumokato.nunulo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun `single and all read updates preserve server truth boundaries`() {
        val notifications = listOf(
            NotificationItem("one", "第一条", "", "checkin", "record", null, null),
            NotificationItem("two", "第二条", "", "user", "2", null, null),
            NotificationItem("done", "已读", "", null, null, "server-time", null),
        )

        val oneRead = markNotificationsReadLocally(notifications, "one", "read-one")
        assertEquals("read-one", oneRead.first { it.id == "one" }.readAt)
        assertNull(oneRead.first { it.id == "two" }.readAt)
        assertEquals("server-time", oneRead.first { it.id == "done" }.readAt)

        val allRead = markNotificationsReadLocally(oneRead, readAt = "read-all")
        assertEquals("read-one", allRead.first { it.id == "one" }.readAt)
        assertEquals("read-all", allRead.first { it.id == "two" }.readAt)
        assertEquals("server-time", allRead.first { it.id == "done" }.readAt)
    }
}
