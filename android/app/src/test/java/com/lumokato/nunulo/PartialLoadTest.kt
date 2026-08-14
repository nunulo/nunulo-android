package com.lumokato.nunulo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PartialLoadTest {
    @Test
    fun successfulSectionReplacesCachedValue() {
        val loaded = Result.success(listOf("fresh")).withFallback(listOf("cached"), "动态加载失败")

        assertEquals(listOf("fresh"), loaded.value)
        assertNull(loaded.error)
    }

    @Test
    fun failedSectionKeepsCachedValueAndReason() {
        val loaded = Result.failure<List<String>>(IllegalStateException("当前网络不可用"))
            .withFallback(listOf("cached"), "动态加载失败")

        assertEquals(listOf("cached"), loaded.value)
        assertEquals("当前网络不可用", loaded.error)
    }

    @Test
    fun failedSectionUsesSpecificFallbackLabelWhenReasonIsBlank() {
        val loaded = Result.failure<List<String>>(IllegalStateException(""))
            .withFallback(listOf("cached"), "伙伴加载失败")

        assertEquals("伙伴加载失败", loaded.error)
    }

    @Test
    fun aggregateKeepsSuccessfulRecordsAndCountsFailures() {
        val failure = IllegalStateException("记录已隐藏")

        val partial = listOf(Result.success("record-1"), Result.failure(failure), Result.success("record-3"))
            .successfulItems()

        assertEquals(listOf("record-1", "record-3"), partial.items)
        assertEquals(1, partial.failedCount)
        assertEquals(failure, partial.firstError)
    }
}
