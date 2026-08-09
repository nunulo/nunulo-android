package com.lumokato.nunulo

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import org.json.JSONObject

internal const val MAX_UPLOAD_BYTES = 30L * 1024 * 1024
internal const val MAX_IMAGE_PIXELS = 40_000_000L

internal data class UploadMedia(
    val filename: String,
    val mimeType: String,
    val byteSize: Long?,
    val requestBody: RequestBody,
)

internal class StreamRequestBody(
    private val mediaType: MediaType,
    private val contentLength: Long?,
    private val maxBytes: Long,
    private val streamProvider: () -> InputStream,
    private val onProgress: (Long, Long?) -> Unit = { _, _ -> },
) : RequestBody() {
    override fun contentType(): MediaType = mediaType

    override fun contentLength(): Long = contentLength ?: -1L

    override fun writeTo(sink: BufferedSink) {
        streamProvider().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            var lastReported = 0L
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > maxBytes) throw IllegalArgumentException("图片不能超过 30 MiB")
                sink.write(buffer, 0, count)
                if (total - lastReported >= 256 * 1024 || total == contentLength) {
                    lastReported = total
                    onProgress(total, contentLength)
                }
            }
        }
    }
}

internal fun prepareUploadMedia(
    resolver: ContentResolver,
    uri: Uri,
    onProgress: (Long, Long?) -> Unit = { _, _ -> },
): UploadMedia {
    val metadata = queryUploadMetadata(resolver, uri)
    val filenameHint = metadata?.first ?: uri.lastPathSegment
    val mimeType = normalizeImageMimeType(resolver.getType(uri) ?: mimeTypeFromFilename(filenameHint))
    val byteSize = metadata?.second ?: localFile(uri)?.length() ?: resolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
        descriptor.length.takeIf { it >= 0L }
    }
    if (byteSize != null && byteSize > MAX_UPLOAD_BYTES) throw IllegalArgumentException("图片不能超过 30 MiB")
    validateImageBounds(resolver, uri)
    val filename = filenameHint?.takeIf { it.isNotBlank() } ?: defaultFilename(mimeType)
    return UploadMedia(
        filename = filename,
        mimeType = mimeType,
        byteSize = byteSize,
        requestBody = StreamRequestBody(
            mediaType = mimeType.toMediaType(),
            contentLength = byteSize,
            maxBytes = MAX_UPLOAD_BYTES,
            streamProvider = { openMediaStream(resolver, uri) },
            onProgress = onProgress,
        ),
    )
}

private fun queryUploadMetadata(resolver: ContentResolver, uri: Uri): Pair<String?, Long?>? = runCatching {
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
        ?.use { cursor ->
            if (!cursor.moveToFirst()) null else {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
                name to size
            }
        }
}.getOrNull()

private fun mimeTypeFromFilename(filename: String?): String? = when (filename?.substringAfterLast('.', "")?.lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "webp" -> "image/webp"
    else -> null
}

internal fun cacheSelectedMedia(context: Context, source: Uri): Uri {
    val resolver = context.contentResolver
    val metadata = queryUploadMetadata(resolver, source)
    val mimeType = normalizeImageMimeType(resolver.getType(source) ?: mimeTypeFromFilename(metadata?.first))
    val extension = when (mimeType) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }
    val directory = File(context.filesDir, "pending-upload").apply { mkdirs() }
    val target = File(directory, "selected-${System.currentTimeMillis()}.$extension")
    try {
        resolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_UPLOAD_BYTES) throw IllegalArgumentException("图片不能超过 30 MiB")
                    output.write(buffer, 0, count)
                }
            }
        } ?: throw IllegalArgumentException("无法读取图片")
        validateImageBounds(resolver, Uri.fromFile(target))
    } catch (error: Exception) {
        target.delete()
        throw error
    }
    return Uri.fromFile(target)
}

internal fun deleteCachedMedia(context: Context, uri: Uri?) {
    val file = uri?.let(::localFile) ?: return
    val managedDirectories = listOf(
        File(context.cacheDir, "capture").canonicalFile,
        File(context.filesDir, "pending-upload").canonicalFile,
    )
    val candidate = runCatching { file.canonicalFile }.getOrNull() ?: return
    if (candidate.parentFile?.let { it in managedDirectories } == true) candidate.delete()
}

internal fun decodeSampledBitmap(resolver: ContentResolver, uri: Uri, maxDimension: Int = 1600): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    if (runCatching { openMediaStream(resolver, uri).use { BitmapFactory.decodeStream(it, null, bounds) } }.isFailure) return null
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxDimension * 2 || bounds.outHeight / sampleSize > maxDimension * 2) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return runCatching { openMediaStream(resolver, uri).use { BitmapFactory.decodeStream(it, null, options) } }.getOrNull()
}

internal fun normalizeImageMimeType(raw: String?): String = when (raw?.lowercase()) {
    "image/jpeg", "image/jpg" -> "image/jpeg"
    "image/png" -> "image/png"
    "image/webp" -> "image/webp"
    else -> throw IllegalArgumentException("仅支持 JPEG、PNG 和 WebP 图片")
}

private fun validateImageBounds(resolver: ContentResolver, uri: Uri) {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openMediaStream(resolver, uri).use { BitmapFactory.decodeStream(it, null, options) }
    if (options.outWidth <= 0 || options.outHeight <= 0) throw IllegalArgumentException("无法读取图片")
    if (options.outWidth.toLong() * options.outHeight.toLong() > MAX_IMAGE_PIXELS) {
        throw IllegalArgumentException("图片像素过大，请缩小后重试")
    }
}

private fun openMediaStream(resolver: ContentResolver, uri: Uri): InputStream =
    localFile(uri)?.let(::FileInputStream)
        ?: resolver.openInputStream(uri)
        ?: throw IllegalArgumentException("无法读取图片")

private fun localFile(uri: Uri): File? = uri.takeIf { it.scheme == "file" }?.path?.let(::File)

private fun defaultFilename(mimeType: String): String = when (mimeType) {
    "image/png" -> "nunulo-upload.png"
    "image/webp" -> "nunulo-upload.webp"
    else -> "nunulo-upload.jpg"
}

internal fun savePendingUpload(prefs: SharedPreferences, pending: PendingUpload) {
    val photoPath = pending.draft.photoUri?.let(::localFile)?.absolutePath ?: return
    val draftFields = pendingUploadFields(pending.draft)
    val payload = JSONObject()
        .put("request_id", pending.requestId)
        .put("attempted", pending.attempted)
        .put("photo_path", photoPath)
    draftFields.forEach { (key, value) -> payload.put(key, value) }
    if (!prefs.edit().putString("pendingUpload", payload.toString()).commit()) {
        throw IllegalStateException("无法保存上传草稿")
    }
}

internal fun pendingUploadFields(draft: UploadDraft): Map<String, String> = linkedMapOf(
    "place_name" to draft.placeName,
    "latitude" to draft.latitude,
    "longitude" to draft.longitude,
    "location_source" to draft.locationSource,
    "note" to draft.note,
    "tags" to draft.tags,
    "visibility" to draft.visibility,
)

internal fun loadPendingUpload(prefs: SharedPreferences): PendingUpload? {
    val raw = prefs.getString("pendingUpload", null) ?: return null
    return runCatching {
        val payload = JSONObject(raw)
        val photo = File(payload.getString("photo_path"))
        if (!photo.isFile) {
            prefs.edit().remove("pendingUpload").apply()
            return null
        }
        PendingUpload(
            requestId = payload.getString("request_id"),
            attempted = payload.optBoolean("attempted", false),
            draft = UploadDraft(
                photoUri = Uri.fromFile(photo),
                placeName = payload.optString("place_name"),
                latitude = payload.optString("latitude"),
                longitude = payload.optString("longitude"),
                locationSource = payload.optString("location_source", "manual"),
                note = payload.optString("note"),
                tags = payload.optString("tags", "娃娃"),
                visibility = payload.optString("visibility", "private"),
            ),
        )
    }.getOrElse {
        prefs.edit().remove("pendingUpload").apply()
        null
    }
}

internal fun clearPendingUpload(prefs: SharedPreferences) {
    prefs.edit().remove("pendingUpload").apply()
}
