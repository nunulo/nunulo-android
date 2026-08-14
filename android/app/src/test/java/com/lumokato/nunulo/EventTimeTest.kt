package com.lumokato.nunulo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventTimeTest {
    @Test
    fun `allows an omitted start or end time`() {
        assertNull(eventTimeRangeError("", "2026-08-14T20:00:00+08:00"))
        assertNull(eventTimeRangeError("2026-08-14T18:00:00+08:00", ""))
    }

    @Test
    fun `accepts an end time after the start time`() {
        assertNull(eventTimeRangeError("2026-08-14T18:00:00+08:00", "2026-08-14T20:00:00+08:00"))
    }

    @Test
    fun `rejects an end time before the start time`() {
        assertEquals(
            "结束时间必须晚于开始时间。",
            eventTimeRangeError("2026-08-14T20:00:00+08:00", "2026-08-14T18:00:00+08:00"),
        )
    }

    @Test
    fun `rejects an end time equal to the start time`() {
        assertEquals(
            "结束时间必须晚于开始时间。",
            eventTimeRangeError("2026-08-14T18:00:00+08:00", "2026-08-14T18:00:00+08:00"),
        )
    }

    @Test
    fun `rejects legacy malformed time values instead of saving them`() {
        assertEquals("开始时间格式无效，请重新选择。", eventTimeRangeError("tomorrow", "2026-08-14T20:00:00+08:00"))
        assertEquals("结束时间格式无效，请重新选择。", eventTimeRangeError("2026-08-14T18:00:00+08:00", "later"))
    }
}
