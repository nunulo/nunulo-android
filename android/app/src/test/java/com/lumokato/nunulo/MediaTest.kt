package com.lumokato.nunulo

import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class MediaTest {
    @Test
    fun streamRequestBodyWritesWithoutPreloading() {
        val content = ByteArray(32 * 1024) { (it % 251).toByte() }
        val body = StreamRequestBody(
            mediaType = "image/jpeg".toMediaType(),
            contentLength = content.size.toLong(),
            maxBytes = content.size.toLong(),
            streamProvider = { content.inputStream() },
        )
        val sink = Buffer()

        body.writeTo(sink)

        assertEquals(content.size.toLong(), body.contentLength())
        assertArrayEquals(content, sink.readByteArray())
    }

    @Test(expected = IllegalArgumentException::class)
    fun streamRequestBodyRejectsUnknownLengthPastLimit() {
        val body = StreamRequestBody(
            mediaType = "image/jpeg".toMediaType(),
            contentLength = null,
            maxBytes = 4,
            streamProvider = { byteArrayOf(1, 2, 3, 4, 5).inputStream() },
        )

        body.writeTo(Buffer())
    }

    @Test
    fun normalizesSupportedMimeTypes() {
        assertEquals("image/jpeg", normalizeImageMimeType("image/jpg"))
        assertEquals("image/png", normalizeImageMimeType("IMAGE/PNG"))
        assertEquals("image/webp", normalizeImageMimeType("image/webp"))
    }

    @Test
    fun reportsProgressAtCompletion() {
        val updates = mutableListOf<Pair<Long, Long?>>()
        val content = ByteArray(1024) { 7 }
        val body = StreamRequestBody(
            mediaType = "image/jpeg".toMediaType(),
            contentLength = content.size.toLong(),
            maxBytes = content.size.toLong(),
            streamProvider = { content.inputStream() },
            onProgress = { written, total -> updates += written to total },
        )

        body.writeTo(Buffer())

        assertTrue(updates.isNotEmpty())
        assertEquals(content.size.toLong() to content.size.toLong(), updates.last())
    }

    @Test
    fun pendingUploadFieldsPreserveVisibility() {
        val fields = pendingUploadFields(
            UploadDraft(
                placeName = "人民广场",
                latitude = "39.901568",
                longitude = "116.422600",
                locationPrivacy = "regional",
                visibility = "public",
                worldVisible = true,
                publicShowcase = true,
            )
        )

        assertEquals("public", fields["visibility"])
        assertEquals("true", fields["world_visible"])
        assertEquals("true", fields["public_showcase"])
        assertEquals("人民广场", fields["place_name"])
        assertEquals("regional", fields["location_privacy"])
        assertTrue("tags" !in fields)
    }

    @Test
    fun sha256IsComputedIncrementallyFromStream() {
        val content = "Nunulo multi-photo".toByteArray(StandardCharsets.UTF_8)

        val checksum = sha256Hex(streamProvider = { content.inputStream() })

        assertEquals("118ebe07d2a64417d16816f47fb683a0625e045c7c7362ff7a92e85e7ad9ec75", checksum)
    }
}
