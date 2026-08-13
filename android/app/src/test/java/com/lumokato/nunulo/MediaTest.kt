package com.lumokato.nunulo

import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
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

    @Test
    fun pendingUploadRoundTripPreservesRecoveryAndIdempotencyState() {
        val readyPhoto = PhotoItem(
            id = "photo-ready",
            contentSha256 = "sha-ready",
            takenAt = "2026-08-13T08:00:00Z",
            exifTakenAt = "2026-08-13T07:58:00Z",
            exifLatitude = 39.901568,
            exifLongitude = 116.422600,
            capturedAtSource = "exif",
            gpsSource = "exif",
            metadataStatus = "ready",
            metadataWarnings = listOf("timezone_missing"),
            thumbUrl = "/media/photo-ready/thumb",
            displayUrl = "/media/photo-ready/display",
            originalUrl = "/media/photo-ready/original",
        )
        val pending = PendingUpload(
            requestId = "request-stays-stable",
            attempted = true,
            draft = UploadDraft(
                editingId = "record-being-edited",
                photos = listOf(
                    DraftPhotoItem(key = "ready", photo = readyPhoto, status = "ready", checksum = "sha-ready", progress = 100, captureSource = "gallery"),
                    DraftPhotoItem(key = "interrupted", status = "uploading", checksum = "sha-interrupted", progress = 63, captureSource = "camera"),
                    DraftPhotoItem(key = "failed", status = "error", checksum = "sha-failed", error = "网络已断开", captureSource = "gallery"),
                ),
                placeName = "北京工人体育场",
                latitude = "39.930500",
                longitude = "116.446900",
                locationSource = "device_current",
                locationProvider = "gps",
                locationCapturedAtMillis = 1_786_608_000_000,
                locationAccuracyMeters = 8.5f,
                locationPrivacy = "regional",
                note = "很长的现场记录也必须在进程被杀后完整恢复",
                takenAt = "2026-08-13T08:00:00Z",
                source = "android_gallery",
                visibility = "public",
                worldVisible = true,
                publicShowcase = true,
                partnerIds = listOf("partner-2", "partner-1"),
                itemTypeIds = listOf("item-type-1"),
                workIds = listOf("work-bandori"),
                characterIds = listOf("character-aya", "character-chisato"),
                eventIds = listOf("event-live", "event-birthday"),
            ),
        )
        val paths = mapOf("interrupted" to "/pending/interrupted.jpg", "failed" to "/pending/failed.jpg")

        val raw = encodePendingUpload(pending, localPathFor = { paths[it.key] })
        val restored = decodePendingUpload(
            raw,
            localPathExists = { it in paths.values },
            localUriFromPath = { null },
        )!!

        assertEquals("request-stays-stable", restored.requestId)
        assertTrue(restored.attempted)
        assertEquals(listOf("ready", "interrupted", "failed"), restored.draft.photos.map { it.key })
        assertEquals("ready", restored.draft.photos[0].status)
        assertEquals(100, restored.draft.photos[0].progress)
        assertEquals("photo-ready", restored.draft.photos[0].photo?.id)
        assertEquals("sha-ready", restored.draft.photos[0].photo?.contentSha256)
        assertEquals(listOf("timezone_missing"), restored.draft.photos[0].photo?.metadataWarnings)
        assertEquals("error", restored.draft.photos[1].status)
        assertEquals(0, restored.draft.photos[1].progress)
        assertEquals("上次上传被中断，请重试", restored.draft.photos[1].error)
        assertEquals("sha-interrupted", restored.draft.photos[1].checksum)
        assertEquals("error", restored.draft.photos[2].status)
        assertEquals("网络已断开", restored.draft.photos[2].error)
        assertEquals("record-being-edited", restored.draft.editingId)
        assertEquals("北京工人体育场", restored.draft.placeName)
        assertEquals("39.930500", restored.draft.latitude)
        assertEquals("116.446900", restored.draft.longitude)
        assertEquals("device_current", restored.draft.locationSource)
        assertEquals("gps", restored.draft.locationProvider)
        assertEquals(1_786_608_000_000, restored.draft.locationCapturedAtMillis)
        assertEquals(8.5f, restored.draft.locationAccuracyMeters)
        assertEquals("regional", restored.draft.locationPrivacy)
        assertEquals("很长的现场记录也必须在进程被杀后完整恢复", restored.draft.note)
        assertEquals("2026-08-13T08:00:00Z", restored.draft.takenAt)
        assertEquals("android_gallery", restored.draft.source)
        assertEquals("public", restored.draft.visibility)
        assertTrue(restored.draft.worldVisible)
        assertTrue(restored.draft.publicShowcase)
        assertEquals(listOf("partner-2", "partner-1"), restored.draft.partnerIds)
        assertEquals(listOf("item-type-1"), restored.draft.itemTypeIds)
        assertEquals(listOf("work-bandori"), restored.draft.workIds)
        assertEquals(listOf("character-aya", "character-chisato"), restored.draft.characterIds)
        assertEquals(listOf("event-live", "event-birthday"), restored.draft.eventIds)
    }

    @Test
    fun pendingUploadDropsUnrecoverableLocalPhotosButKeepsUploadedOnes() {
        val pending = PendingUpload(
            requestId = "request-1",
            draft = UploadDraft(
                photos = listOf(
                    DraftPhotoItem(key = "missing", status = "error"),
                    DraftPhotoItem(key = "ready", photo = PhotoItem("photo-1"), status = "ready"),
                )
            ),
        )
        val raw = encodePendingUpload(pending, localPathFor = { if (it.key == "missing") "/gone/photo.jpg" else null })

        val restored = decodePendingUpload(raw, localPathExists = { false }, localUriFromPath = { null })

        assertEquals(listOf("ready"), restored?.draft?.photos?.map { it.key })
    }

    @Test
    fun pendingUploadRejectsCorruptOrEntirelyUnrecoverablePayload() {
        assertNull(decodePendingUpload("not-json"))
        val raw = encodePendingUpload(
            PendingUpload("request-1", UploadDraft(photos = listOf(DraftPhotoItem(key = "missing")))),
            localPathFor = { "/gone/photo.jpg" },
        )
        assertNull(decodePendingUpload(raw, localPathExists = { false }, localUriFromPath = { null }))
    }
}
